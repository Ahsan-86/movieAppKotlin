package com.ahsan.movieapp.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

/**
 * A single-line URL value that shrinks its font so the whole string stays visible inside the space
 * it's given — the "Website" row of the Information section (Movie + TV, 2026-09-22 fix: long TMDB
 * URLs used to clip/overflow the row's right-hand column). Measures the text at
 * [MaterialTheme.typography.bodyMedium] and, if that doesn't fit the available width, walks down
 * through progressively smaller sizes until one does — wrapping as an absolute last resort rather
 * than ever clipping. Rendered like InfoRow's normal value text (medium weight, primary
 * clickable styling, underlined) so the row still reads as one consistent list.
 */
@Composable
fun UrlAutoSizeText(
    url: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val baseStyle = MaterialTheme.typography.bodyMedium

    // Use the BoxWithConstraints scope itself for the measurement: `constraints.maxWidth` is the
    // width this column actually got (the weight(1f) slot inside InfoRow's Row), already in pixels
    // — exactly the unit TextMeasurer.measure() reports, so no density conversion is needed.
    BoxWithConstraints(modifier = modifier) {
        // Enter the scope via `with(this)` so the Compose `UnusedBoxWithConstraintsScope` lint
        // counts the explicit `this` as a use of the scope (property reads alone don't register).
        with(this) {
            val availableWidth = constraints.maxWidth

            // Recompute only when the available width or the address changes. Measuring in
            // composition is cheap here (a single short string) and is the standard pattern for
            // auto-sizing text.
            val style = remember(availableWidth, url, baseStyle) {
                val fit = listOf(14.sp, 13.sp, 12.sp, 11.sp, 10.sp, 9.sp).firstOrNull { size ->
                    textMeasurer.measure(
                        text = url,
                        style = baseStyle.copy(fontSize = size),
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    ).size.width <= availableWidth
                }
                baseStyle.copy(fontSize = fit ?: 9.sp)
            }

            val fitsOnOneLine = textMeasurer.measure(
                text = url,
                style = style,
                softWrap = false,
                maxLines = 1,
                overflow = TextOverflow.Clip
            ).size.width <= availableWidth

            Text(
                text = url,
                style = style,
                color = color,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                // Wrap rather than clip if even the smallest size is still too wide.
                softWrap = !fitsOnOneLine,
                maxLines = if (fitsOnOneLine) 1 else Int.MAX_VALUE
            )
        }
    }
}