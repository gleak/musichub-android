package com.mediaplayer.android.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.common.MHMonogramTile
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Brand-locked splash for the initial silent auth probe. Per
 * `mockup/mh-auth-states.jsx#AuthProbeScreen`. Stages:
 *  - [AuthViewModel.ProbeStage.Token] — verifying stored Google token.
 *  - [AuthViewModel.ProbeStage.Me] — token good, fetching `/api/auth/me`.
 *  - [AuthViewModel.ProbeStage.RejectedSilent] — server rejected the token;
 *    flashes red while the VM clears the token and falls back to the picker.
 *
 * Replaces the bare `CircularProgressIndicator` previously shown for the
 * initial-frame `State.Loading` (`AuthViewModel.kt:30-47`).
 */
@Composable
fun AuthProbeScreen(stage: AuthViewModel.ProbeStage) {
    val mono = LocalMHMono.current
    val muted = stage == AuthViewModel.ProbeStage.RejectedSilent
    val (label, code, targetPct) = when (stage) {
        AuthViewModel.ProbeStage.Token -> Triple(
            "Verifico sessione",
            "auth/token-refresh",
            0.35f,
        )
        AuthViewModel.ProbeStage.Me -> Triple(
            "Carico profilo",
            "auth/refresh-me",
            0.78f,
        )
        AuthViewModel.ProbeStage.RejectedSilent -> Triple(
            "Sessione scaduta",
            "auth/token-rejected · clear",
            1f,
        )
    }
    val pct by animateFloatAsState(
        targetValue = targetPct,
        animationSpec = tween(durationMillis = 300),
        label = "auth-probe-pct",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (muted) RejectedRadialBrush else ProbeRadialBrush)
            .padding(start = 28.dp, end = 28.dp, top = 180.dp, bottom = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.alpha(if (muted) 0.55f else 1f)) {
            MHMonogramTile()
        }
        Spacer(Modifier.height(32.dp))

        Text(
            text = "MusicHub",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = (-0.6).sp,
            ),
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (muted) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE14848)),
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MHColors.Lime,
                )
            }
            Text(
                text = "$label…",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MHColors.TextLo,
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .height(2.dp)
                    .background(if (muted) Color(0xFFE14848) else MHColors.Lime),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = code,
            style = mono.duration.copy(
                fontSize = 10.sp,
                color = MHColors.TextLo2,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

private val ProbeRadialBrush: ShaderBrush = radialBrush(top = Color(0xFF2A4615))
private val RejectedRadialBrush: ShaderBrush = radialBrush(top = Color(0xFF2A1515))

private fun radialBrush(top: Color): ShaderBrush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader = RadialGradientShader(
        colors = listOf(top, MHColors.BgTop, MHColors.BgBottom),
        colorStops = listOf(0f, 0.35f, 1f),
        center = Offset(size.width / 2f, 0f),
        radius = maxOf(size.width * 0.6f, size.height * 0.6f),
        tileMode = TileMode.Clamp,
    )
}
