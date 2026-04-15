package com.example.triade_monitoramento



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ScreenContainer(
    background: Color = Color.White,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 36.dp,
                bottom = 16.dp
            )
    ) {
        content()
    }
}