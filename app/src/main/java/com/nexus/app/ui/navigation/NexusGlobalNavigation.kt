package com.nexus.app.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nexus.app.network.NexusTailscaleConnector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusGlobalNavigation() {
    val navController = rememberNavController()
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("NEXUS CORE v40.45.7") }) }
    ) { padding ->
        NavHost(navController, startDestination = "home", modifier = Modifier.padding(padding)) {
            composable("home") { 
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NEXUS MASTER COCKPIT", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.navigate("timeline") }) { Text("Zeitstrahl Matrix") }
                    Button(onClick = { navController.navigate("settings") }) { Text("System Einstellungen") }
                }
            }
            composable("timeline") { TimelineScreen() }
            composable("settings") { Text("Einstellungen: Tailscale Node 100.107.24.67 aktiv", modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
fun TimelineScreen() {
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("Warte auf Aktion...") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Fokus Nachrichten", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Event #124: Terminbestätigung Schule")
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    scope.launch {
                        statusText = "Sende an PC..."
                        val success = NexusTailscaleConnector.executeActionOnPc("not_important", "124")
                        statusText = if (success) "Erfolgreich an DB gesendet!" else "Fehler: PC nicht erreichbar."
                    }
                }) {
                    Text("Aus Fokus entfernen (not_important)")
                }
                Spacer(Modifier.height(8.dp))
                Text(statusText, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
