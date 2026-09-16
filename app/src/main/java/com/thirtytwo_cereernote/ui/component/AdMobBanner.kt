package com.thirtytwo_cereernote.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Public-source placeholder. Production ad implementation and IDs are private. */
@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(50.dp), contentAlignment = Alignment.Center) {
        Text("Advertising integration omitted in public source")
    }
}
