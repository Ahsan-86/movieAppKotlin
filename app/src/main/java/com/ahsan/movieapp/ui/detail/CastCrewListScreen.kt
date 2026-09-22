package com.ahsan.movieapp.ui.detail

import com.ahsan.movieapp.R

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ahsan.movieapp.ui.components.EmptyState
import com.ahsan.movieapp.ui.components.FullScreenError
import com.ahsan.movieapp.ui.components.FullScreenLoading

/**
 * Phase 3 Round A's "view all" destination — the full cast list plus the director, opened from
 * the arrow next to MovieDetailScreen's "Cast & Crew" heading. Same full-screen pattern as
 * GenreScreen/PersonScreen: own Scaffold + TopAppBar + real back button, not a root-level tab.
 * Cast + director only, no other crew roles — matches the confirmed Phase 3 scope.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastCrewListScreen(
    onBack: () -> Unit,
    onPersonClick: (personId: Int, personName: String) -> Unit,
    viewModel: CastCrewListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.castcrew_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> FullScreenLoading()
                state.isEmpty && state.errorMessage != null ->
                    FullScreenError(message = state.errorMessage ?: stringResource(R.string.castcrew_couldnt_load))
                state.isEmpty -> EmptyState(title = stringResource(R.string.nothing_here), body = stringResource(R.string.castcrew_empty_body))
                else -> LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    state.director?.let { director ->
                        item { SectionHeading(stringResource(R.string.castcrew_director)) }
                        item {
                            PersonRow(
                                name = director.name,
                                subtitle = null,
                                photoUrl = director.profileUrl,
                                onClick = { onPersonClick(director.id, director.name) }
                            )
                        }
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
                    }

                    if (state.cast.isNotEmpty()) {
                        item { SectionHeading(stringResource(R.string.cast_title)) }
                        items(state.cast, key = { it.id }) { member ->
                            PersonRow(
                                name = member.name,
                                subtitle = member.character.ifBlank { null },
                                photoUrl = member.profileUrl,
                                onClick = { onPersonClick(member.id, member.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun PersonRow(
    name: String,
    subtitle: String?,
    photoUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
