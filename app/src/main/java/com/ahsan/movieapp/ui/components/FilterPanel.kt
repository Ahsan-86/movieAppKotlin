package com.ahsan.movieapp.ui.components

import com.ahsan.movieapp.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ahsan.movieapp.domain.model.DiscoverFilters
import com.ahsan.movieapp.domain.model.GenreChip
import java.util.Calendar
import java.util.Locale

/** The extractable part of the search screen's Phase 2.5 filter panel — shared with the genre
 *  screen's filter section. Same dropdowns/slider/reset/show-results behavior either way; the
 *  genre dropdown is optional (empty [genres] omits it), since the genre screen pins its genre
 *  already. [onGenreSelected] is only invoked by the panel when [genres] is non-empty.
 */
@Composable
fun FilterPanelSection(
    filters: DiscoverFilters,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onYearSelected: (Int?) -> Unit,
    onLanguageSelected: (String?) -> Unit,
    onMinRatingChanged: (Float?) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    genres: List<GenreChip> = emptyList(),
    onGenreSelected: ((Int?) -> Unit)? = null
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(
                    text = if (filters.isEmpty) stringResource(R.string.filter_filters) else stringResource(R.string.filter_filters_active, filters.activeCount),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) stringResource(R.string.filter_collapse_filters) else stringResource(R.string.filter_expand_filters)
            )
        }
        if (isExpanded) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                if (genres.isNotEmpty() && onGenreSelected != null) {
                    val genreOptions = genres.mapNotNull { genre -> genre.movieGenreId?.let { it to genre.name } }
                    FilterDropdown(
                        label = stringResource(R.string.filter_genre),
                        selectedLabel = genreOptions.firstOrNull { it.first == filters.genreId }?.second ?: stringResource(R.string.filter_any),
                        options = genreOptions,
                        onSelected = onGenreSelected
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
                val yearOptions = (currentYear downTo FILTER_EARLIEST_YEAR).map { it to it.toString() }
                FilterDropdown(
                    label = stringResource(R.string.filter_release_year),
                    selectedLabel = filters.year?.toString() ?: stringResource(R.string.filter_any),
                    options = yearOptions,
                    onSelected = onYearSelected
                )
                Spacer(modifier = Modifier.height(12.dp))

                val selectedLanguage = FILTER_LANGUAGES.firstOrNull { it.first == filters.language }?.second
                val languageOptions = mutableListOf<Pair<String, String>>()
                for ((code, nameRes) in FILTER_LANGUAGES) {
                    languageOptions.add(code to stringResource(nameRes))
                }
                FilterDropdown(
                    label = stringResource(R.string.filter_language),
                    selectedLabel = if (selectedLanguage != null) stringResource(selectedLanguage) else stringResource(R.string.filter_any),
                    options = languageOptions,
                    onSelected = onLanguageSelected
                )
                Spacer(modifier = Modifier.height(16.dp))

                MinRatingSlider(minRating = filters.minRating, onMinRatingChanged = onMinRatingChanged)
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onReset,
                        enabled = !filters.isEmpty
                    ) { Text(stringResource(R.string.filter_reset)) }
                    Button(onClick = onApply, enabled = !filters.isEmpty, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.filter_show_results))
                    }
                }
            }
        }
    }
}

private val FILTER_LANGUAGES = listOf(
    "en" to R.string.filter_english,
    "es" to R.string.filter_spanish,
    "fr" to R.string.filter_french,
    "de" to R.string.filter_german,
    "hi" to R.string.filter_hindi,
    "ja" to R.string.filter_japanese,
    "ko" to R.string.filter_korean,
    "zh" to R.string.filter_chinese,
    "it" to R.string.filter_italian,
    "pt" to R.string.filter_portuguese
)

private const val FILTER_EARLIEST_YEAR = 1950

/**
 * A short summary row shown above filtered results (in place of the filter panel) so the applied
 * criteria stay visible and adjustable without needing to re-open the panel every time. Shared by
 * the search screen's Discover results and the genre screen's filtered browse. [resultCount]
 * (optional) adds an "N results found" line under the row — fed from the paging sources' total.
 */
@Composable
fun FilterSummaryBar(
    filters: DiscoverFilters,
    onEdit: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    resultCount: Int? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.filter_filtered_filters, filters.activeCount),
                style = MaterialTheme.typography.titleMedium
            )
            Row {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.filter_edit)) }
                TextButton(onClick = onClear) { Text(stringResource(R.string.filter_clear)) }
            }
        }
        if (resultCount != null) {
            Text(
                text = stringResource(R.string.filter_results_found, resultCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * A read-only text field that opens a [DropdownMenu] on tap. `OutlinedTextField(readOnly = true)`
 * still consumes taps for cursor placement, so a transparent clickable [Box] is layered on top to
 * actually open the menu — a plain [DropdownMenu] rather than `ExposedDropdownMenuBox` to avoid
 * depending on that API's exact shape in whatever Material3 version this project is on.
 */
@Composable
private fun <T> FilterDropdown(
    label: String,
    selectedLabel: String,
    options: List<Pair<T, String>>,
    onSelected: (T?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = { expanded = true })
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_any)) }, onClick = { onSelected(null); expanded = false })
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(text = { Text(optionLabel) }, onClick = { onSelected(value); expanded = false })
            }
        }
    }
}

/** 0 means "no minimum" (Any); the slider otherwise runs 1.0–9.0 in half-point steps. */
@Composable
private fun MinRatingSlider(minRating: Float?, onMinRatingChanged: (Float?) -> Unit) {
    val sliderValue = minRating ?: 0f
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.filter_minimum_rating, if (minRating == null) stringResource(R.string.filter_any) else String.format(Locale.US, "%.1f", minRating)),
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = sliderValue,
            onValueChange = { onMinRatingChanged(if (it <= 0f) null else it) },
            valueRange = 0f..9f,
            steps = 17 // 0.5-point increments across the 0..9 range
        )
    }
}