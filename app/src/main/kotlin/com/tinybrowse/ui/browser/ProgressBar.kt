package com.tinybrowse.ui.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tinybrowse.ui.theme.TinyBrowseColors

@Composable
fun ProgressBar(
    progress: Int,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading && progress < 100) {
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = modifier
                .fillMaxWidth()
                .height(2.dp),
            color = TinyBrowseColors.Primary,
            trackColor = TinyBrowseColors.SurfaceVariant,
        )
    }
}
