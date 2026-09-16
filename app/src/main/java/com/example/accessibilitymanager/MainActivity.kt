package com.example.accessibilitymanager

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AccessibilityManagerApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
fun AccessibilityManagerApp(vm: AccessibilityViewModel = viewModel()) {
    val context = LocalContext.current
    val items by vm.items.collectAsState()
    val query by vm.query.collectAsState()
    var settingsOpened by remember { mutableStateOf(false) }
    val settingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        vm.refresh()
    }
    LaunchedEffect(settingsOpened) {
        if (settingsOpened) {
            settingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            settingsOpened = false
        }
    }

    val filtered = items.filter { it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true) }
    MaterialTheme {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = { Text("Менеджер доступности", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(8.dp))
                Text("Службы на устройстве", style = MaterialTheme.typography.headlineSmall)
                Text("Порядок сохраняется только внутри этого приложения.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("Поиск служб") },
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { settingsOpened = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Открыть системные настройки")
                }
                Spacer(Modifier.height(12.dp))
                if (filtered.isEmpty()) {
                    EmptyState()
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(filtered, key = { _, item -> item.component.flattenToString() }) { index, item ->
                            val actualIndex = items.indexOf(item)
                            ServiceCard(
                                item = item,
                                canMoveUp = actualIndex > 0,
                                canMoveDown = actualIndex < items.lastIndex,
                                onMoveUp = { vm.move(actualIndex, actualIndex - 1) },
                                onMoveDown = { vm.move(actualIndex, actualIndex + 1) },
                                onOpen = {
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
                                        putExtra(Intent.EXTRA_COMPONENT_NAME, item.component.flattenToString())
                                    }
                                    runCatching { settingsLauncher.launch(intent) }.getOrElse {
                                        settingsLauncher.launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ServiceCard(item: AccessibilityServiceItem, canMoveUp: Boolean, canMoveDown: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onOpen: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Accessibility, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.label, fontWeight = FontWeight.SemiBold)
                Text(if (item.isEnabled) "Включена" else "Выключена", color = if (item.isEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(item.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onMoveUp, enabled = canMoveUp) { Icon(Icons.Default.ArrowUpward, contentDescription = "Переместить выше") }
                IconButton(onClick = onMoveDown, enabled = canMoveDown) { Icon(Icons.Default.ArrowDownward, contentDescription = "Переместить ниже") }
            }
        }
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
            Text(if (item.isEnabled) "Управлять службой" else "Включить в настройках")
        }
    }
}

@androidx.compose.runtime.Composable
private fun EmptyState() {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text("Службы не найдены. Установите приложение со службой специальных возможностей или измените запрос.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
