package com.tinybrowse.ui.browser

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.tinybrowse.ui.theme.TinyBrowseColors

@Composable
fun Toolbar(
    currentUrl: String,
    currentTitle: String,
    isLoading: Boolean,
    isSecure: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputUrl by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    // Sync display URL when page loads
    LaunchedEffect(currentUrl) {
        if (!isEditing) {
            inputUrl = currentUrl
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = TinyBrowseColors.ToolbarBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back
            IconButton(
                onClick = onBack,
                enabled = canGoBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (canGoBack) TinyBrowseColors.TextPrimary
                           else TinyBrowseColors.TextHint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Forward
            IconButton(
                onClick = onForward,
                enabled = canGoForward,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Forward",
                    tint = if (canGoForward) TinyBrowseColors.TextPrimary
                           else TinyBrowseColors.TextHint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // URL bar
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 6.dp),
                shape = MaterialTheme.shapes.small,
                color = TinyBrowseColors.SurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SSL icon
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = if (isSecure) "Secure" else "Not secure",
                        tint = if (isSecure) TinyBrowseColors.Secure
                               else TinyBrowseColors.Insecure,
                        modifier = Modifier.size(14.dp)
                    )

                    BasicTextField(
                        value = if (isEditing) inputUrl else displayUrl(currentUrl, currentTitle),
                        onValueChange = { inputUrl = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = TinyBrowseColors.TextPrimary
                        ),
                        cursorBrush = SolidColor(TinyBrowseColors.Primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (inputUrl.isNotBlank()) {
                                    onNavigate(inputUrl)
                                    isEditing = false
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (inputUrl.isEmpty() && !isEditing) {
                                Text(
                                    text = "Search or enter URL",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TinyBrowseColors.TextHint
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Clear button when editing
                    if (isEditing && inputUrl.isNotEmpty()) {
                        IconButton(
                            onClick = { inputUrl = "" },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TinyBrowseColors.TextHint,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Refresh / Stop
            IconButton(
                onClick = if (isLoading) onStop else onReload,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = if (isLoading) "Stop" else "Refresh",
                    tint = TinyBrowseColors.TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Menu
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.size(36.dp)
            ) {
                Text(
                    text = "⋮",
                    style = MaterialTheme.typography.titleMedium,
                    color = TinyBrowseColors.TextPrimary
                )
            }
        }
    }
}

private fun displayUrl(url: String, title: String): String {
    if (url.isEmpty()) return ""
    return try {
        val uri = java.net.URI(url)
        val host = uri.host?.removePrefix("www.") ?: url
        if (title.isNotEmpty() && title != host) "$title — $host" else host
    } catch (e: Exception) {
        url
    }
}
