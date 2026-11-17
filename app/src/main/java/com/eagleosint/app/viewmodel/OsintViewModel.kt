package com.eagleosint.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eagleosint.app.data.OsintRepository
import com.eagleosint.app.data.ScanResultItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val lastQuery: String? = null,
    val results: List<ScanResultItem> = emptyList(),
    val statusMessage: String = "Введите цель для начала разведки"
)

class OsintViewModel(
    private val repository: OsintRepository = OsintRepository()
) : ViewModel() {

    private val _moduleStates = MutableStateFlow<Map<String, ScanUiState>>(emptyMap())
    val moduleStates: StateFlow<Map<String, ScanUiState>> = _moduleStates.asStateFlow()

    private val scanJobs = mutableMapOf<String, Job>()

    fun stateFor(moduleRoute: String): ScanUiState =
        _moduleStates.value[moduleRoute] ?: ScanUiState()

    fun startScan(moduleRoute: String, target: String) {
        val cleanTarget = target.trim()
        if (cleanTarget.isEmpty()) return

        scanJobs[moduleRoute]?.cancel()
        updateModuleState(moduleRoute) {
            ScanUiState(
                isScanning = true,
                progress = 0.08f,
                lastQuery = cleanTarget,
                results = emptyList(),
                statusMessage = "Запуск модуля сканирования..."
            )
        }

        scanJobs[moduleRoute] = viewModelScope.launch {
            when (moduleRoute) {
                "godorker" -> {
                    val dorks = repository.generateDorks(cleanTarget)
                    updateModuleState(moduleRoute) {
                        ScanUiState(
                            isScanning = false,
                            progress = 1f,
                            lastQuery = cleanTarget,
                            results = dorks,
                            statusMessage = "Сгенерировано ${dorks.size} поисковых запросов"
                        )
                    }
                }
                "github" -> {
                    val collected = mutableListOf<ScanResultItem>()
                    repository.inspectGitHubUser(cleanTarget).collect { item ->
                        collected.add(item)
                        updateModuleState(moduleRoute) {
                            ScanUiState(
                                isScanning = true,
                                progress = (collected.size / 6f).coerceIn(0.15f, 0.9f),
                                lastQuery = cleanTarget,
                                results = collected.toList(),
                                statusMessage = "Анализ GitHub: получено ${collected.size} записей..."
                            )
                        }
                    }
                    updateModuleState(moduleRoute) { current ->
                        current.copy(
                            isScanning = false,
                            progress = 1f,
                            statusMessage = "GitHub-анализ завершён (${collected.size} записей)"
                        )
                    }
                }
                else -> {
                    val collected = mutableListOf<ScanResultItem>()
                    val totalPlatforms = repository.platformCount
                    repository.checkUsername(cleanTarget).collect { item ->
                        collected.add(item)
                        val done = collected.size >= totalPlatforms
                        updateModuleState(moduleRoute) {
                            ScanUiState(
                                isScanning = !done,
                                progress = collected.size.toFloat() / totalPlatforms.toFloat(),
                                lastQuery = cleanTarget,
                                results = collected.toList(),
                                statusMessage = if (done) {
                                    val foundCount = collected.count { it.found }
                                    "Готово: найдено $foundCount из $totalPlatforms"
                                } else {
                                    "Проверено ${collected.size} из $totalPlatforms платформ"
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearResults(moduleRoute: String) {
        scanJobs[moduleRoute]?.cancel()
        _moduleStates.update { current -> current - moduleRoute }
    }

    fun clearAllHistory() {
        scanJobs.values.forEach { it.cancel() }
        scanJobs.clear()
        _moduleStates.value = emptyMap()
    }

    private fun updateModuleState(moduleRoute: String, transform: (ScanUiState) -> ScanUiState) {
        _moduleStates.update { current ->
            val prev = current[moduleRoute] ?: ScanUiState()
            current + (moduleRoute to transform(prev))
        }
    }
}
