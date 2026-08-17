package com.wikzo.todo.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wikzo.todo.data.repository.SyncGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairDeviceUiState(
    val codeInput: String = "",
    val isClaiming: Boolean = false,
    val isJoined: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Backs the "enter/scan a code" side of pairing: a 6-digit code, typed or read off
 * a scanned QR code, is claimed via [SyncGroupRepository.claimPairingCode]. On
 * success this device's local group id has already been switched to the shared
 * group by the repository -- the screen just needs to show a brief confirmation
 * and let the caller navigate back to the (now shared) task list.
 */
@HiltViewModel
class PairDeviceViewModel @Inject constructor(
    private val syncGroupRepository: SyncGroupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairDeviceUiState())
    val uiState: StateFlow<PairDeviceUiState> = _uiState.asStateFlow()

    /** Called as the user types in the manual-entry field -- keeps only digits, capped at [CODE_LENGTH]. */
    fun onCodeInputChange(value: String) {
        val digitsOnly = value.filter(Char::isDigit).take(CODE_LENGTH)
        _uiState.update { it.copy(codeInput = digitsOnly, errorMessage = null) }
    }

    /** Called with a QR scanner's raw decoded text -- fills the field and, if it's a full code, submits it right away. */
    fun onCodeScanned(rawValue: String) {
        val digitsOnly = rawValue.filter(Char::isDigit).take(CODE_LENGTH)
        _uiState.update { it.copy(codeInput = digitsOnly, errorMessage = null) }
        if (digitsOnly.length == CODE_LENGTH) {
            claim(digitsOnly)
        }
    }

    fun claim(code: String = _uiState.value.codeInput) {
        if (code.length != CODE_LENGTH) {
            _uiState.update { it.copy(errorMessage = "Enter the 6-digit code") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isClaiming = true, errorMessage = null) }
            try {
                syncGroupRepository.claimPairingCode(code)
                _uiState.update { it.copy(isClaiming = false, isJoined = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isClaiming = false, errorMessage = e.message ?: "Couldn't link the device. Try again.")
                }
            }
        }
    }

    companion object {
        const val CODE_LENGTH = 6
    }
}
