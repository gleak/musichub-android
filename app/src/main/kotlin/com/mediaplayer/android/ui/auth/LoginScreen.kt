package com.mediaplayer.android.ui.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient

@Composable
fun LoginScreen(
    state: AuthViewModel.State,
    onSignIn: (Context) -> Unit,
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.screenBg()),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            is AuthViewModel.State.Loading -> CircularProgressIndicator(color = MHColors.Lime)

            is AuthViewModel.State.NotSignedIn,
            is AuthViewModel.State.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                ) {
                    // Lime monogram tile
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MHColors.Lime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "M",
                            color = Color(0xFF0A0A0A),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    EyebrowText(text = "Benvenuto")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "MusicHub",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MHColors.TextHi,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "La tua libreria musicale personale.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MHColors.TextLo,
                    )
                    Spacer(Modifier.height(48.dp))
                    Button(
                        onClick = { onSignIn(context) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MHColors.Lime,
                            contentColor = Color(0xFF0A0A0A),
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text("Accedi con Google", fontWeight = FontWeight.Bold)
                    }
                    if (state is AuthViewModel.State.Error) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF4D2E),
                        )
                    }
                }
            }

            is AuthViewModel.State.SignedIn -> Unit
        }
    }
}
