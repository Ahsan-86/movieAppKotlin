package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ahsan.movieapp.R
import com.ahsan.movieapp.domain.model.WatchProvider
import com.ahsan.movieapp.domain.model.WatchProviderRegion

/**
 * Phase 3 Round C's streaming-availability section, shared by the Movie and TV detail screens —
 * grouped Stream/Rent/Buy provider-logo rows for one TMDB region, with a region dropdown to switch
 * (locale-derived default, US fallback — see the region-resolution logic in `MovieDetailViewModel` /
 * `TvDetailViewModel`). Switching regions is instant/local, not a re-fetch — TMDB already returned
 * every region in one call. The caller only renders this composable when [availableRegions] is
 * non-empty (TMDB has data for at least one region); if the currently *selected* region has none,
 * the dropdown still shows (so the user can pick one that does) with a short "not available" line in
 * place of the provider rows.
 *
 * TMDB's `/watch/providers` response doesn't hand back a deep link per provider — only one
 * `link` per region, to TMDB's own watch-providers page for this title (which forwards through
 * JustWatch to the actual services). So every logo in this region, plus the attribution line
 * itself, opens that same [WatchProviderRegion.link] — that's also what TMDB's API terms require
 * showing as a real, working link whenever this data is displayed, not just decorative text.
 */
@Composable
fun WatchProvidersSection(
    region: WatchProviderRegion?,
    availableRegions: List<String>,
    selectedRegion: String,
    onRegionChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.detail_where_to_watch), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            RegionDropdown(selectedRegion = selectedRegion, availableRegions = availableRegions, onRegionChange = onRegionChange)
        }

        if (region == null || region.isEmpty) {
            Text(
                text = stringResource(R.string.detail_not_available_in_region, selectedRegion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            val uriHandler = LocalUriHandler.current
            val onOpenProviders: (() -> Unit)? = region.link
                ?.takeIf { it.isNotBlank() }
                ?.let { link -> { uriHandler.openUri(link) } }

            ProviderRow(label = stringResource(R.string.detail_provider_stream), providers = region.flatrate, onProviderClick = onOpenProviders)
            ProviderRow(label = stringResource(R.string.detail_provider_rent), providers = region.rent, onProviderClick = onOpenProviders)
            ProviderRow(label = stringResource(R.string.detail_provider_buy), providers = region.buy, onProviderClick = onOpenProviders)
            Text(
                text = stringResource(R.string.detail_provider_attribution),
                style = MaterialTheme.typography.labelSmall,
                color = if (onOpenProviders != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = if (onOpenProviders != null) TextDecoration.Underline else null,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .then(if (onOpenProviders != null) Modifier.clickable(onClick = onOpenProviders) else Modifier)
            )
        }
    }
}

/** A compact chip + dropdown menu of every region TMDB has watch-provider data for, ISO codes as-is (e.g. "US", "GB"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegionDropdown(selectedRegion: String, availableRegions: List<String>, onRegionChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { expanded = true },
            label = { Text(selectedRegion) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.detail_change_region)) }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            availableRegions.forEach { code ->
                DropdownMenuItem(
                    text = { Text(code) },
                    onClick = {
                        onRegionChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** One Stream/Rent/Buy row — renders nothing if this region has no providers for that category. */
@Composable
private fun ProviderRow(label: String, providers: List<WatchProvider>, onProviderClick: (() -> Unit)?) {
    if (providers.isEmpty()) return
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(
            contentPadding = PaddingValues(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(providers, key = { it.id }) { provider ->
                ProviderLogo(provider = provider, onClick = onProviderClick)
            }
        }
    }
}

/**
 * TMDB doesn't hand back a per-provider deep link (see [WatchProvidersSection]'s doc comment), so
 * every logo shares the same [onClick] — the region's one TMDB watch-providers link — same
 * nullable-callback convention as the other tappable cards on the detail screens.
 */
@Composable
private fun ProviderLogo(provider: WatchProvider, onClick: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        ShimmerAsyncImage(
            model = provider.logoUrl,
            contentDescription = provider.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
