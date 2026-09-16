package com.example.accessibilitymanager

import android.app.Application
import android.content.ComponentName
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

 data class AccessibilityServiceItem(
    val component: ComponentName,
    val label: String,
    val packageName: String,
    val isEnabled: Boolean
)

class AccessibilityViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("service_order", 0)
    private val _items = MutableStateFlow<List<AccessibilityServiceItem>>(emptyList())
    val items: StateFlow<List<AccessibilityServiceItem>> = _items.asStateFlow()
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val manager = getApplication<Application>().getSystemService(android.content.Context.ACCESSIBILITY_SERVICE)
                    as android.view.accessibility.AccessibilityManager
            val enabled = Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty().split(':').filter { it.isNotBlank() }.toSet()

            val installed = manager.getInstalledAccessibilityServiceList().map { info ->
                val component = ComponentName(info.resolveInfo.serviceInfo.packageName, info.resolveInfo.serviceInfo.name)
                AccessibilityServiceItem(
                    component = component,
                    label = info.resolveInfo.loadLabel(getApplication<Application>().packageManager).toString(),
                    packageName = component.packageName,
                    isEnabled = component.flattenToString() in enabled
                )
            }
            val savedOrder = preferences.getString("order", "").orEmpty().split('|').filter { it.isNotBlank() }
            _items.value = installed.sortedBy { item ->
                val position = savedOrder.indexOf(item.component.flattenToString())
                if (position < 0) Int.MAX_VALUE else position
            }
        }
    }

    fun setQuery(value: String) { _query.value = value }

    fun move(from: Int, to: Int) {
        val current = _items.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val moved = current.removeAt(from)
        current.add(to, moved)
        _items.value = current
        preferences.edit().putString("order", current.joinToString("|") { it.component.flattenToString() }).apply()
    }
}
