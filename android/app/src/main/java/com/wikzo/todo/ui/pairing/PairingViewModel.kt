package com.wikzo.todo.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wikzo.todo.data.repository.SyncGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val isGenerating: Boolean = false,
    val code: String? = null,
    val expiresAtMillis: Long? = null,
    val remainingSeconds: Long? = null,
    val isExpired: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Backs the "show my code" side of pairing: generates a 6-digit code (via
 * [SyncGroupRepository.createPairingCode]) for another device to claim, and runs
 * a local countdown to [PairingUiState.expiresAtMillis] so the UI can show a live
 * "expires in..." indicator and flip to an expired state without another network
 * round-trip.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val syncGroupRepository: SyncGroupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairingUiState())
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    init {
        generateCode()
    }

    /** Generates a fresh code, replacing any previous one -- used both on first load and by "Generate new code". */
    fun generateCode() {
        countdownJob?.cancel()
        viewModelScope.launch {
            _uiState.update { PairingUiState(isGenerating = true) }
            try {
                val pairing = syncGroupRepository.createPairingCode()
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        code = pairing.code,
                        expiresAtMillis = pairing.expiresAtMillis,
                    )
                }
                startCountdown(pairing.expiresAtMillis)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGenerating = false, errorMessage = e.message ?: "Couldn't generate a code. Try again.")
                }
            }
        }
    }

    private fun startCountdown(expiresAtMillis: Long) {
        countdownJob = viewModelScope.launch {
            while (isActive) {
                val remainingMillis = expiresAtMillis - System.currentTimeMillis()
                if (remainingMillis <= 0) {
                    _uiState.update { it.copy(remainingSeconds = 0, isExpired = true) }
                    break
                }
                _uiState.update { it.copy(remainingSeconds = remainingMillis / 1000, isExpired = false) }
                delay(1_000)
            }
        }
    }
}
