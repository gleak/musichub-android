package com.mediaplayer.android.ui.auth

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.GoogleGIcon
import com.mediaplayer.android.ui.common.MHMonogramTile
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@Composable
fun LoginScreen(
    state: AuthViewModel.State,
    onSignIn: (Context) -> Unit,
    pickerCancelled: SharedFlow<Unit> = remember { MutableSharedFlow() },
) {
    val context = LocalContext.current
    val signingIn = state is AuthViewModel.State.SigningIn
    val error = state as? AuthViewModel.State.Error

    var showCancelToast by remember { mutableStateOf(false) }
    LaunchedEffect(pickerCancelled) {
        pickerCancelled.collect {
            showCancelToast = true
            delay(2500)
            showCancelToast = false
        }
    }

    when (state) {
        is AuthViewModel.State.SignedIn -> Unit

        else -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MHGradient.loginBg()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 28.dp, end = 28.dp, top = 88.dp, bottom = 40.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MHMonogramTile()
                    Spacer(Modifier.height(32.dp))
                    EyebrowText(text = "Benvenuto")
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "MusicHub",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MHColors.TextHi,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Per ascoltare la tua libreria, accedi con Google.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MHColors.TextLo,
                    )

                    if (error != null) {
                        Spacer(Modifier.height(36.dp))
                        LoginErrorPanel(code = error.code)
                    }
                }

                Button(
                    onClick = { onSignIn(context) },
                    enabled = !signingIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F1F1F),
                        disabledContainerColor = Color.White.copy(alpha = 0.85f),
                        disabledContentColor = Color(0xFF1F1F1F),
                    ),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (signingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1F1F1F),
                            )
                        } else {
                            GoogleGIcon()
                        }
                        Text(
                            text = if (signingIn) "Accesso in corso…" else "Accedi con Google",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                if (signingIn) {
                    val mono = LocalMHMono.current
                    Text(
                        text = "auth/google · credential-exchange",
                        style = mono.duration.copy(
                            fontSize = 11.sp,
                            color = MHColors.TextLo2,
                            letterSpacing = 0.5.sp,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text(
                        text = "Continuando accetti i Termini e l'Informativa privacy.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MHColors.TextLo2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Soft toast — picker cancel. Per `mockup/mh-auth-states.jsx#LoginPickerCancelScreen`.
            AnimatedVisibility(
                visible = showCancelToast,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 130.dp),
            ) {
                PickerCancelToast()
            }
        }
    }
}

@Composable
private fun PickerCancelToast() {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xF21C1C1C))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MHColors.TextLo),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Accesso annullato",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MHColors.TextHi,
            )
            Text(
                text = "auth/picker-cancel",
                style = mono.duration.copy(
                    fontSize = 10.5.sp,
                    color = MHColors.TextLo,
                ),
            )
        }
    }
}

@Composable
private fun LoginErrorPanel(code: String) {
    val mono = LocalMHMono.current
    val red = Color(0xFFE14848)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(red.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = red.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(red),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Accesso non riuscito",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFFFFB3B3),
            )
            Spacer(Modifier.height(2.dp))
            Row {
                Text(
                    text = "Verifica la connessione e riprova. Codice: ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = Color(0xFFFFB3B3).copy(alpha = 0.7f),
                )
                Text(
                    text = code,
                    style = mono.duration.copy(
                        color = Color(0xFFFFB3B3).copy(alpha = 0.7f),
                        fontSize = 11.5.sp,
                    ),
                )
            }
        }
    }
}
