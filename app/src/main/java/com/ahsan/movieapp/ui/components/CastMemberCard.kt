package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ahsan.movieapp.domain.model.CastMember

/**
 * One tappable cast member in a horizontal row: a circular profile avatar, the actor's name, and
 * the character they played beneath it. Shared by both [com.ahsan.movieapp.ui.detail.MovieDetailScreen]
 * and [com.ahsan.movieapp.ui.tv.TvDetailScreen]'s "Cast & Crew" rows (each screen previously
 * carried an identical private copy — extracted here with a size bump: avatar 72→84dp, card
 * 84→96dp, name `labelMedium`→`labelLarge`).
 */
@Composable
fun CastMemberCard(
    member: CastMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(CARD_WIDTH).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(AVATAR_SIZE)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (member.profileUrl != null) {
                AsyncImage(
                    model = member.profileUrl,
                    contentDescription = member.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = member.name,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxSize().padding(18.dp)
                )
            }
        }
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp)
        )
        if (member.character.isNotBlank()) {
            Text(
                text = member.character,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private val AVATAR_SIZE = 84.dp
private val CARD_WIDTH = 96.dp