package com.ejemplo.mobileconnect

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.ejemplo.mobileconnect.theme.MobileConnectTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/**
 * MobileConnect — Práctica 6 / contrato WebSocket v2.
 * El servidor asigna A o B; el cliente solo envía unirse(sala) + enviar(texto).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            MobileConnectTheme {
                MobileConnectApp()
            }
        }
    }
}

data class MensajeUi(
    val id: Long,
    val emisor: String,
    val texto: String,
    val horaTexto: String,
)

private fun formatHora(enviadoEn: String): String {
    if (enviadoEn.isBlank()) {
        return DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
    }
    return try {
        DateTimeFormatter.ofPattern("HH:mm:ss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(enviadoEn))
    } catch (_: Exception) {
        enviadoEn.takeLast(8)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileConnectApp() {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var wsUrl by remember { mutableStateOf(WebSocketBridge.DEFAULT_URL) }
    var sala by remember { mutableStateOf(WebSocketBridge.DEFAULT_SALA) }
    var miRol by remember { mutableStateOf<String?>(null) } // A | B | null (lo pone el servidor)
    var estado by remember { mutableStateOf("Sin conectar") }
    var conectado by remember { mutableStateOf(false) }
    var borrador by remember { mutableStateOf("") }
    val mensajes = remember { mutableStateListOf<MensajeUi>() }
    val idsVistos = remember { mutableSetOf<Long>() }

    val contadorA = mensajes.count { it.emisor == "A" }
    val contadorB = mensajes.count { it.emisor == "B" }

    fun reemplazarHistorial(lista: List<WebSocketBridge.WsMensaje>) {
        mensajes.clear()
        idsVistos.clear()
        lista.forEach { m ->
            idsVistos.add(m.id)
            mensajes.add(
                MensajeUi(
                    id = m.id,
                    emisor = m.emisor,
                    texto = m.texto,
                    horaTexto = formatHora(m.enviadoEn),
                ),
            )
        }
    }

    fun agregarSiNuevo(m: WebSocketBridge.WsMensaje) {
        if (!idsVistos.add(m.id)) return
        mensajes.add(
            MensajeUi(
                id = m.id,
                emisor = m.emisor,
                texto = m.texto,
                horaTexto = formatHora(m.enviadoEn),
            ),
        )
    }

    val bridge = remember {
        WebSocketBridge(
            onStatus = { estado = it },
            onHistorial = { reemplazarHistorial(it) },
            onMensaje = { agregarSiNuevo(it) },
            onAsignacion = { rol -> miRol = rol },
            onErrorServidor = { msg ->
                scope.launch { snackbarHostState.showSnackbar(msg) }
            },
            onConnectedChanged = { conectado = it },
        )
    }

    DisposableEffect(Unit) {
        onDispose { bridge.disconnect() }
    }

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) listState.animateScrollToItem(mensajes.lastIndex)
    }

    val etiquetaRol = when (miRol) {
        "A" -> "Este dispositivo: Dispositivo A"
        "B" -> "Este dispositivo: Dispositivo B"
        else -> "Este dispositivo: sin rol (el primero en enviar será A)"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MobileConnect") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                ),
                actions = {
                    IconButton(
                        onClick = {
                            if (!conectado) {
                                scope.launch { snackbarHostState.showSnackbar("Conéctate primero") }
                                return@IconButton
                            }
                            if (!bridge.limpiar()) {
                                scope.launch { snackbarHostState.showSnackbar("No se pudo limpiar") }
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("Limpiar enviado") }
                            }
                        },
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar", tint = Color.White)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = borrador,
                    onValueChange = { borrador = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Mensaje") },
                    singleLine = true,
                )
                Button(
                    onClick = {
                        val texto = borrador.trim()
                        if (texto.isEmpty()) {
                            scope.launch {
                                snackbarHostState.showSnackbar("No se puede enviar un mensaje vacío")
                            }
                            return@Button
                        }
                        if (!conectado) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Primero Conectar")
                            }
                            return@Button
                        }
                        if (!bridge.enviar(texto)) {
                            scope.launch { snackbarHostState.showSnackbar("Falló el envío") }
                            return@Button
                        }
                        borrador = ""
                        scope.launch { snackbarHostState.showSnackbar("Mensaje enviado") }
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Enviar")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "Dispositivo A / Dispositivo B (asignados por el servidor)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF455A64),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    etiquetaRol,
                    fontWeight = FontWeight.Bold,
                    color = when (miRol) {
                        "A" -> Color(0xFF1565C0)
                        "B" -> Color(0xFF00897B)
                        else -> Color(0xFF6A1B9A)
                    },
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = wsUrl,
                    onValueChange = { wsUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL WebSocket") },
                    singleLine = true,
                    enabled = !conectado,
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = sala,
                    onValueChange = { sala = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Sala (misma que tu compañero)") },
                    singleLine = true,
                    enabled = !conectado,
                )

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContadorChip("Msgs A", contadorA, Modifier.weight(1f), Color(0xFF1565C0))
                    ContadorChip("Msgs B", contadorB, Modifier.weight(1f), Color(0xFF00897B))
                }

                Spacer(Modifier.height(6.dp))
                Text("Estado: $estado", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Conectando…")
                            }
                            bridge.connect(wsUrl, sala)
                        },
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Conectar")
                    }
                    OutlinedButton(onClick = {
                        bridge.disconnect()
                        estado = "Desconectado"
                        conectado = false
                        miRol = null
                    }) {
                        Icon(Icons.Default.LinkOff, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Cortar")
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
            ) {
                if (mensajes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            if (conectado) "Conectado. El primero que envíe será A."
                            else "Conecta con la misma sala que tu compañero.",
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(mensajes, key = { it.id }) { msg ->
                            BurbujaMensaje(msg, miRol = miRol)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContadorChip(
    titulo: String,
    contador: Int,
    modifier: Modifier = Modifier,
    color: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold, color = color)
            Text("$contador", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun BurbujaMensaje(msg: MensajeUi, miRol: String?) {
    val propio = miRol != null && msg.emisor.equals(miRol, ignoreCase = true)
    val bg = if (msg.emisor == "A") Color(0xFF1565C0) else Color(0xFF00897B)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (propio) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bg, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Dispositivo ${msg.emisor}",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(text = msg.texto, color = Color.White)
            Text(
                text = msg.horaTexto,
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
