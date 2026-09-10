package com.ahsan.movieapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Full person profile — bio, photo, primary department — for the person screen's header.
 * [birthday]/[deathday] are TMDB's ISO "YYYY-MM-DD" strings (both null when unknown); [gender] is
 * TMDB's numeric code (0 = unspecified/unknown, 1 = female, 2 = male, 3 = non-binary) — see
 * PersonDetails.genderLabel for the display mapping.
 */
data class PersonDetailsDto(
    val id: Int,
    val name: String,
    val biography: String?,
    @SerializedName("profile_path") val profilePath: String?,
    @SerializedName("known_for_department") val knownForDepartment: String?,
    val birthday: String?,
    val deathday: String?,
    val gender: Int?
)

/** `/person/{id}/combined_credits` — movie + TV credits in one response. */
data class CombinedCreditsDto(
    val id: Int,
    val cast: List<CombinedCreditDto>,
    val crew: List<CombinedCreditDto>
)

/**
 * One credit row from combined_credits. [character] is only present on cast entries, [job] only
 * on crew entries (e.g. "Director", "Writer", "Producer") — the repository filters crew by
 * job == "Director" to build the "As Director" tab. [mediaType] distinguishes movie vs tv; a tv
 * entry uses [name]/[firstAirDate] instead of [title]/[releaseDate], same convention as multi-search.
 */
data class CombinedCreditDto(
    val id: Int,
    @SerializedName("media_type") val mediaType: String?,
    val title: String?,
    val name: String?,
    val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("vote_count") val voteCount: Int?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    val character: String?,
    val job: String?
)
