package com.project.lighthouse.authentication

import com.project.lighthouse.R
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignInScreen(
    state: SignInState,
    onSignInClick: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = state.signInError) {
        state.signInError?.let { error ->
            Toast.makeText(
                context,
                error,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Box (
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.lighthouse_logo),
            contentDescription = "Lighthouse Logo",
            modifier = Modifier
                .size(150.dp)
                .align(BiasAlignment(horizontalBias = 0f, verticalBias = -0.4f))
        )
        OutlinedButton(
            modifier = Modifier
                .size(200.dp, 80.dp)
                .align(BiasAlignment(horizontalBias = 0f, verticalBias = 0.4f)),
            onClick = onSignInClick,
        ) {
            Text(
                text = "Sign in",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
