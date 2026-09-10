package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * `/movie/{id}/watch/providers` response — streaming availability for the Detail screen's
 * streaming-availability section (Phase 3 Round C), powered by JustWatch. TMDB returns every
 * region in ONE call, keyed by ISO 3166-1 country code (e.g. "US", "GB") under [results] — no
 * per-region fetch needed; the Detail screen's region dropdown filters this one response
 * client-side rather than re-fetching per region.
 */
data class WatchProvidersResponseDto(
    val id: Int,
    val results: Map<String, WatchProviderRegionDto>?
)

/**
 * One region's availability. `free`/`ads` (ad-supported viewing) exist in TMDB's response but
 * aren't decoded here — Round C's confirmed scope is Stream/Rent/Buy only.
 */
data class WatchProviderRegionDto(
    val link: String?,
    val flatrate: List<WatchProviderDto>?,
    val rent: List<WatchProviderDto>?,
    val buy: List<WatchProviderDto>?
)

data class WatchProviderDto(
    @SerializedName("provider_id") val providerId: Int,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("logo_path") val logoPath: String?,
    @SerializedName("display_priority") val displayPriority: Int?
)
