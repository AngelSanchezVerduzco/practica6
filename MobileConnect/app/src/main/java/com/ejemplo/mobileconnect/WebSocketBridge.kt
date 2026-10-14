package com.ejemplo.mobileconnect

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Contrato canal v2 (compañero):
 * - unirse:  {"tipo":"unirse","sala":"practica6"}  (sin emisor)
 * - enviar:  {"tipo":"enviar","texto":"..."}
 * - limpiar: {"tipo":"limpiar"}
 * - recibe: historial | mensaje | asignacion | error
 * El servidor asigna A/B (primer enviar = A; el otro = B).
 */
class WebSocketBridge(
    private val onStatus: (String) -> Unit,
    private val onHistorial: (List<WsMensaje>) -> Unit,
    private val onMensaje: (WsMensaje) -> Unit,
    private val onAsignacion: (String?) -> Unit,
    private val onErrorServidor: (String) -> Unit,
    private val onConnectedChanged: (Boolean) -> Unit,
) {
    companion object {
        private const val TAG = "MobileConnectWS"
        const val DEFAULT_URL = "ws://10.13.8.252:8080/ws"
        const val DEFAULT_SALA = "practica6"
    }

    data class WsMensaje(
        val id: Long,
        val emisor: String,
        val texto: String,
        val enviadoEn: String,
    )

    private val main = Handler(Looper.getMainLooper())
    private var client: OkHttpClient? = null
    private var socket: WebSocket? = null
    private var sala: String = DEFAULT_SALA
    private var joined = false

    fun connect(url: String, salaId: String) {
        disconnect()
        sala = salaId.trim().ifBlank { DEFAULT_SALA }
        joined = false
        val target = url.trim().ifBlank { DEFAULT_URL }

        main.post {
            onStatus("Conectando a $target …")
            onConnectedChanged(false)
            onAsignacion(null)
        }

        client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder().url(target).build()
        socket = client!!.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // v2: solo sala (el servidor asigna A/B).
                val join = JSONObject()
                    .put("tipo", "unirse")
                    .put("sala", sala)
                Log.d(TAG, "onOpen -> $join")
                webSocket.send(join.toString())
                main.post { onStatus("Uniendo a sala $sala…") }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "recv ${text.take(180)}")
                handleIncoming(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                joined = false
                main.post {
                    onConnectedChanged(false)
                    onAsignacion(null)
                    onStatus("Desconectado ($code)")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "fail", t)
                joined = false
                main.post {
                    onConnectedChanged(false)
                    onStatus("Error de conexión: ${t.message ?: "revisa IP/Wi‑Fi"}")
                }
            }
        })
    }

    fun enviar(texto: String): Boolean {
        val ws = socket ?: return false
        if (!joined) return false
        val payload = JSONObject()
            .put("tipo", "enviar")
            .put("texto", texto)
        return ws.send(payload.toString())
    }

    fun limpiar(): Boolean {
        val ws = socket ?: return false
        if (!joined) return false
        return ws.send(JSONObject().put("tipo", "limpiar").toString())
    }

    fun disconnect() {
        val oldSocket = socket
        val oldClient = client
        socket = null
        client = null
        joined = false
        runCatching { oldSocket?.cancel() }
        runCatching { oldClient?.dispatcher?.cancelAll() }
        main.post {
            onConnectedChanged(false)
            onAsignacion(null)
        }
    }

    private fun handleIncoming(raw: String) {
        try {
            val obj = JSONObject(raw.trim())
            when (obj.optString("tipo")) {
                "historial" -> {
                    joined = true
                    val lista = parseLista(obj.optJSONArray("mensajes"))
                    main.post {
                        onConnectedChanged(true)
                        onStatus("Conectado · sala $sala")
                        onHistorial(lista)
                    }
                }
                "mensaje" -> {
                    val m = parseUno(obj) ?: return
                    main.post { onMensaje(m) }
                }
                "asignacion" -> {
                    val rol: String? = if (obj.isNull("emisor")) null else obj.optString("emisor", null)
                    val limpio = when (rol) {
                        "A", "B" -> rol
                        else -> null
                    }
                    main.post {
                        onAsignacion(limpio)
                        onStatus(
                            if (limpio == null) "Conectado · sala $sala · sin rol aún"
                            else "Conectado · sala $sala · eres Dispositivo $limpio",
                        )
                    }
                }
                "error" -> {
                    val msg = obj.optString("mensaje", "Error del servidor")
                    main.post {
                        onStatus("Servidor: $msg")
                        onErrorServidor(msg)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "JSON inválido", e)
        }
    }

    private fun parseLista(arr: JSONArray?): List<WsMensaje> {
        if (arr == null) return emptyList()
        val out = ArrayList<WsMensaje>(arr.length())
        for (i in 0 until arr.length()) {
            parseUno(arr.getJSONObject(i))?.let { out.add(it) }
        }
        return out
    }

    private fun parseUno(m: JSONObject): WsMensaje? {
        val texto = m.optString("texto", "")
        if (texto.isBlank()) return null
        return WsMensaje(
            id = m.optLong("id", System.currentTimeMillis()),
            emisor = m.optString("emisor", "?"),
            texto = texto,
            enviadoEn = m.optString("enviadoEn", ""),
        )
    }
}
