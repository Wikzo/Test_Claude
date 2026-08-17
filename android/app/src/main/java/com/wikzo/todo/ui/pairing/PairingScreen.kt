package com.wikzo.todo.ui.pairing

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val QR_SIZE_PX = 512

/**
 * "Show my code" side of pairing: generates a 6-digit code + matching QR code for
 * another device to scan or type in, with a live "expires in..." countdown.
 */
@Composable
fun PairingScreen(
    onBack: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PairingScreen(
        uiState = uiState,
        onBack = onBack,
        onGenerateNewCode = viewModel::generateCode,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PairingScreen(
    uiState: PairingUiState,
    onBack: () -> Unit,
    onGenerateNewCode: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Link a device") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "On your other device, choose \"Enter a code\" and enter this code, or scan the QR code below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Box(
                modifier = Modifier
                    .padding(top = 32.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    uiState.isGenerating -> {
                        CircularProgressIndicator()
                    }
                    uiState.code != null -> {
                        CodeAndQr(code = uiState.code, isExpired = uiState.isExpired)
                    }
                    else -> {
                        Text(
                            text = uiState.errorMessage ?: "Couldn't generate a code.",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            if (uiState.code != null && !uiState.isGenerating) {
                Text(
                    text = if (uiState.isExpired) {
                        "Code expired"
                    } else {
                        "Expires in ${formatRemaining(uiState.remainingSeconds)}"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.isExpired) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            // uiState.code and uiState.errorMessage are mutually exclusive here --
            // generateCode() always clears both before a new attempt -- so a single
            // "Generate new code" action below covers both the expired-code and the
            // failed-to-generate cases.
            if (uiState.isExpired || (uiState.errorMessage != null && !uiState.isGenerating)) {
                Button(
                    onClick = onGenerateNewCode,
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text("Generate new code")
                }
            }
        }
    }
}

@Composable
private fun CodeAndQr(code: String, isExpired: Boolean) {
    val qrBitmap = remember(code) { generateQrCodeBitmap(code, QR_SIZE_PX) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 40.sp,
            letterSpacing = 8.sp,
            color = if (isExpired) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )

        Box(
            modifier = Modifier
                .padding(top = 24.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.White)
                .padding(16.dp),
        ) {
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR code for pairing code $code",
                modifier = Modifier
                    .size(200.dp)
                    .alpha(if (isExpired) 0.35f else 1f),
            )
        }
    }
}

private fun formatRemaining(remainingSeconds: Long?): String {
    val total = remainingSeconds ?: return "…"
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}
