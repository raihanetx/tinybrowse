package com.tinybrowse.ui.start

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tinybrowse.data.model.SavedSite
import com.tinybrowse.ui.theme.TinyBrowseColors
import com.tinybrowse.util.UrlUtils

@Composable
fun StartPage(
    savedSites: List<SavedSite>,
    onSearch: (String) -> Unit,
    onSiteClick: (SavedSite) -> Unit,
    onSiteLongClick: (SavedSite) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App title
        Text(
            text = "TinyBrowse",
            style = MaterialTheme.typography.titleLarge,
            color = TinyBrowseColors.TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search DuckDuckGo or enter URL") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TinyBrowseColors.TextHint
                )
            },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(
                onGo = {
                    if (searchQuery.isNotBlank()) {
                        onSearch(searchQuery)
                        searchQuery = ""
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TinyBrowseColors.Primary,
                unfocusedBorderColor = TinyBrowseColors.ToolbarBorder,
                focusedContainerColor = TinyBrowseColors.Surface,
                unfocusedContainerColor = TinyBrowseColors.Surface,
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Saved sites
        if (savedSites.isNotEmpty()) {
            Text(
                text = "Saved Sites",
                style = MaterialTheme.typography.labelLarge,
                color = TinyBrowseColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(savedSites, key = { it.id }) { site ->
                    SavedSiteCard(
                        site = site,
                        onClick = { onSiteClick(site) },
                        onLongClick = { onSiteLongClick(site) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedSiteCard(
    site: SavedSite,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TinyBrowseColors.SurfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = UrlUtils.getDomain(site.url),
                style = MaterialTheme.typography.bodySmall,
                color = TinyBrowseColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = site.title.ifEmpty { site.url },
                style = MaterialTheme.typography.labelSmall,
                color = TinyBrowseColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
