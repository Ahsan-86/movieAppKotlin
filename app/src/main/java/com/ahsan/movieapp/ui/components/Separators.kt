package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/**
 * The shared B3 accent separator (confirmed by Ahsan 2026-09-26): a small rotated square used as
 * the premium "diamond" between caption meta items, genre names, and the Person credit-filter
 * chips. Size and color come from the caller — hero captions use small white diamonds, the
 * Person chip row uses a larger palette-tone square.
 */
@Composable
fun SeparatorDiamond(
    size: Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .rotate(45f)
            .background(color)
    )
}

/**
 * The standardized inline separator for small secondary text lines (age·gender, "Filtered · N",
 * the non-diamond genre fallback). One glyph everywhere — literal " • " and " · " usages from
 * before are consolidated onto this.
 */
const val DOT_SEPARATOR = " · "