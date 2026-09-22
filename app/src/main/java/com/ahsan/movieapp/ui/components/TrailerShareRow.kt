package com.ahsan.movieapp.ui.components

import com.ahsan.movieapp.R

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight


/**
 * Watch Trailer + Share — shared by both Movie and TV detail screens (Phase 2.6 Session 2, per
 * Ahsan's confirmed scope; repositioned into each screen's title/meta/genre info column on
 * Ahsan's 2026-09-12 post-ship feedback — see com.ahsan.movieapp.ui.tv.TvDetailScreen]'s and
 * com.ahsan.movieapp.ui.detail.MovieDetailScreen's docs). "Watch Trailer" calls onWatchTrailer
 * with the trailer's YouTube video id — the caller (both detail screens, via
 * com.ahsan.movieapp.ui.navigation.MovieNavGraph) navigates to TrailerPlayerScreen, a real
 * full-screen destination hosting the embedded player (see that composable's doc for why a nav
 * destination replaced the dialog this row used to open directly). Share opens the system share
 * sheet with a text blurb — title + a link to that title's TMDB page (a placeholder link target
 * for now, per Ahsan's confirmed scope — may change in Phase 6).
 *
 * trailerKey is null when TMDB has no YouTube trailer for this title (see
 * com.ahsan.movieapp.data.mapper.bestYoutubeTrailerKey) — the trailer button simply doesn't
 * render then, so no "no trailer" message is ever needed here: a null trailerKey hides the whole
 * feature rather than showing an error. When trailerKey IS present but the YouTube player itself
 * refuses to play it (e.g. a real, valid trailer with embedding disabled by the studio's own
 * YouTube channel), the player area shows a message specific to the failure reason instead of a
 * generic one — see EmbeddedYouTubePlayer's doc. Share always renders.
 */
@Composable
fun TrailerShareRow(
    trailerKey: String?,
    shareTitle: String,
    shareUrl: String,
    onWatchTrailer: (videoId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (trailerKey != null) {
            OutlinedButton(onClick = { onWatchTrailer(trailerKey) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(text = stringResource(R.string.trailer_watch_trailer), modifier = Modifier.padding(start = 4.dp))
            }
        }
        IconButton(onClick = { shareText(context, shareTitle, shareUrl) }) {
            Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.trailer_share))
        }
    }
}

/**
 * The trailer's own full-screen destination (route `trailer/{videoId}`, see
 * com.ahsan.movieapp.ui.navigation.Destinations.TrailerPlayer) — replaces the earlier
 * Dialog-based player Ahsan hit a real architectural problem with. Ahsan's 2026-09-15 feedback
 * ("far too small") was fixed in that Dialog version by growing the player to full device width;
 * his follow-up full-screen work (`SENSOR_LANDSCAPE` forcing + system-bar hiding, written directly
 * into the Dialog, plus `android:configChanges` on `MainActivity` in `AndroidManifest.xml`) then
 * surfaced the actual bug: a Compose androidx.compose.ui.window.Dialog opens its OWN floating
 * android.view.Window, separate from the hosting Activity's. Forcing
 * Activity.requestedOrientation from *inside* that dialog rotates the Activity underneath it,
 * and on rotation the floating dialog window doesn't reliably survive on this app's dev device (an
 * Infinix/Transsion phone, already flagged as a source of WebView-specific quirks earlier this
 * session) — collapsing the dialog and revealing the Movie/TV Detail screen underneath, exactly
 * Ahsan's reported bug ("MainActivity unexpectedly pops up on the details screen instead of
 * displaying in full-screen mode, breaking the separate screen layout").
 *
 * Moving the player into a real navigation destination — a normal composable in the SAME Activity
 * window, reached via androidx.navigation.NavController like every other screen in this app (see
 * com.ahsan.movieapp.ui.tv.SeasonEpisodesScreen for the identical Scaffold+TopAppBar+back-button
 * shape) — removes the second window entirely: `android:configChanges` on `MainActivity` (Ahsan's
 * own manifest addition, correct and still needed) now protects the ONE window this screen
 * actually runs in, so Activity.requestedOrientation forcing and system-bar hiding are both safe
 * here in a way they never could be inside a floating dialog — they now target the Activity's own
 * android.view.Window directly via Activity.getWindow rather than a
 * androidx.compose.ui.window.DialogWindowProvider, so there's no second window to fall out of
 * sync with. This also satisfies Ahsan's three UI asks for free: a real TopAppBar gives the
 * "Trailer" title, the player sits right below it (top of screen, not centered in a small floating
 * box), and the non-fullscreen layout fills the full content width at a 16:9 aspect ratio — the
 * same shape the real YouTube app uses for a playing video before its description.
 * BackHandler intercepts the system back button while fullscreen so it exits fullscreen first,
 * rather than leaving the screen in one press.
 *
 * **Round #7 (2026-09-15 UI polish, Ahsan's feedback after confirming playback works)** made four
 * further changes:
 * 1. The fullscreen toggle moved OFF the TopAppBar and onto the player itself (bottom-end corner
 *    overlay, visible in both states) — Ahsan's ask was for it to live "on the embed player, not
 *    app bar", closer to how a real video player's own controls are positioned.
 * 2. Rotating the device to landscape now enters fullscreen automatically (a
 *    LaunchedEffect keyed on android.content.res.Configuration.orientation) instead of only
 *    responding to an explicit tap — this only auto-ENTERS: once fullscreen forces
 *    `SENSOR_LANDSCAPE`, the device can't report a portrait configuration again until fullscreen is
 *    explicitly exited, so leaving fullscreen stays an explicit tap (matches most video apps'
 *    behavior once landscape is force-locked).
 * 3. The old plain "Open in YouTube" icon-only action is now YoutubeButton — an icon+text chip,
 *    per Ahsan's ask — still the same openInYoutube call underneath, and the IFrame player's own
 *    native YouTube-branding button (which duplicated that same action inside the video itself) is
 *    now suppressed via `playerVars.modestbranding` in iframePlayerHtml, since this app-level
 *    button already covers it.
 * 4. **Fixes a real bug this screen had since round #6**: the fullscreen/non-fullscreen branches
 *    used to each call EmbeddedYouTubePlayer from a DIFFERENT call site (one inside `if
 *    (isFullscreen)`, one inside the `else`) — Compose treats those as two unrelated composables,
 *    so toggling fullscreen tore down and recreated the underlying WebView each time, restarting
 *    playback from 0 (Ahsan's "maintain video playback position during fullscreen changes" ask).
 *    Fixed by keeping EmbeddedYouTubePlayer at a single, unconditional call site inside one
 *    Scaffold whose `topBar` conditionally renders nothing while fullscreen — the same
 *    `contentWindowInsets = WindowInsets(0, 0, 0, 0)` trick com.ahsan.movieapp.ui.navigation.MovieNavGraph's
 *    outer Scaffold already uses for an absent topBar, so an empty topBar costs zero padding rather
 *    than reserving a system-bar-height gap — and only the player's own `Modifier` (full-bleed vs.
 *    top-aligned 16:9) changes between the two states, which Compose can apply to the SAME
 *    AndroidView/WebView instance without disposing it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailerPlayerScreen(videoId: String, onBack: () -> Unit) {
    var isFullscreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val view = LocalView.current
    val configuration = LocalConfiguration.current

    fun restorePortrait() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // Auto-enter fullscreen the moment the device is physically rotated to landscape — see this
    // composable's doc, round #7 point 2. Only entering is automatic; exiting is still explicit.
    LaunchedEffect(configuration.orientation) {
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && !isFullscreen) {
            isFullscreen = true
        }
    }

    DisposableEffect(isFullscreen) {
        val window = activity?.window
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, !isFullscreen) }
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }

        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            bars?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            restorePortrait()
            bars?.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            bars?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Leaving the screen entirely (back button, or popped some other way) must always restore
    // portrait and the system bars, even if the user backs out while mid-fullscreen — otherwise
    // the Activity (shared with every other screen in the app, unlike the old per-dialog window)
    // would stay stuck landscape/edge-to-edge after returning to Detail.
    DisposableEffect(Unit) {
        onDispose {
            restorePortrait()
            activity?.window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, true)
                WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    Scaffold(
        topBar = {
            // Empty while fullscreen (not omitted) so Scaffold measures it at zero height rather
            // than reusing whatever height the real TopAppBar last had — paired with
            // contentWindowInsets below, this keeps the player's available space correct in both
            // states without any manual padding math.
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(stringResource(R.string.trailer_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        YoutubeButton(onClick = { openInYoutube(context, videoId) })
                        Spacer(Modifier.width(8.dp))
                    }
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            // Single, unconditional call site — see this composable's doc, round #7 point 4 — only
            // the modifier (size/position) differs between fullscreen and not.
            EmbeddedYouTubePlayer(
                videoId = videoId,
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    // Top of screen, full width, 16:9 — matches the real YouTube app's own player
                    // sizing for a playing video (Ahsan's original "match YouTube app dimensions" ask).
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                }
            )

            IconButton(
                onClick = { isFullscreen = !isFullscreen },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = if (isFullscreen) stringResource(R.string.trailer_exit_fullscreen) else stringResource(R.string.trailer_fullscreen),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * The "open in the real YouTube app/browser" action, restyled per Ahsan's round #7 ask ("change
 * redirect icon to YouTube icon with text") from a plain generic icon-only button to an icon+text
 * chip. [YoutubeGlyph] is a small hand-drawn badge (rounded red square + white play triangle) — the
 * Compose Material Icons set has no official YouTube brand mark to import, and this session's
 * device-bridge grant doesn't cover `res/drawable` for a proper vector asset, so a simple
 * generically-"YouTube-like" glyph drawn with [Canvas] is the self-contained option that needs no
 * new dependency or resource file.
 */
@Composable
private fun YoutubeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = YoutubeLogo,
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = stringResource(R.string.trailer_youtube),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private val YoutubeLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "Youtube",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Official YouTube mark (simple-icons path)
        path(fill = SolidColor(Color(0xFFFF0000))) {
            moveTo(23.498f, 6.186f)
            arcToRelative(3.016f, 3.016f, 0f, false, false, -2.122f, -2.136f)
            curveTo(19.505f, 3.545f, 12f, 3.545f, 12f, 3.545f)
            reflectiveCurveToRelative(-7.505f, 0f, -9.377f, 0.505f)
            arcTo(3.017f, 3.017f, 0f, false, false, 0.502f, 6.186f)
            curveTo(0f, 8.07f, 0f, 12f, 0f, 12f)
            reflectiveCurveToRelative(0f, 3.93f, 0.502f, 5.814f)
            arcToRelative(3.016f, 3.016f, 0f, false, false, 2.122f, 2.136f)
            curveTo(4.495f, 20.455f, 12f, 20.455f, 12f, 20.455f)
            reflectiveCurveToRelative(7.505f, 0f, 9.377f, -0.505f)
            arcToRelative(3.015f, 3.015f, 0f, false, false, 2.122f, -2.136f)
            curveTo(24f, 15.93f, 24f, 12f, 24f, 12f)
            reflectiveCurveToRelative(0f, -3.93f, -0.502f, -5.814f)
            close()
        }
        path(fill = SolidColor(Color.White)) {
            moveTo(9.545f, 15.568f)
            verticalLineTo(8.432f)
            lineTo(15.818f, 12f)
            close()
        }
    }.build()
}

@Composable
private fun YoutubeGlyph(size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        drawRoundRect(
            color = Color(0xFFFF0000),
            cornerRadius = CornerRadius(this.size.width * 0.3f, this.size.height * 0.3f)
        )
        val w = this.size.width
        val h = this.size.height
        val triangleWidth = w * 0.38f
        val triangleHeight = h * 0.42f
        val left = (w - triangleWidth) / 2f + w * 0.04f
        val path = Path().apply {
            moveTo(left, h / 2f - triangleHeight / 2f)
            lineTo(left, h / 2f + triangleHeight / 2f)
            lineTo(left + triangleWidth, h / 2f)
            close()
        }
        drawPath(path, color = Color.White)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * A [WebView] running YouTube's own IFrame Player API, loaded via [WebView.loadDataWithBaseURL].
 * Several real, distinct bugs were found and fixed here, all on Ahsan's device (an
 * Infinix/Transsion phone):
 *
 * **Bug 1 (2026-09-12, "video player configuration error", YouTube error 153)**: the original
 * implementation loaded YouTube's `/embed/{videoId}` page directly via `loadUrl()`, which has no
 * real HTTPS origin/referrer behind it from the WebView's perspective — error 153 is specifically
 * YouTube's IFrame API rejecting playback over missing/inconsistent origin and referrer info.
 * Fixed by switching to `loadDataWithBaseURL`. This is the officially supported way to embed
 * YouTube playback in a WebView — distinct from (and NOT) the deprecated native YouTube Android
 * Player SDK, which needs the YouTube app and Google Play Services installed; this approach needs
 * neither.
 *
 * **Bug 2 (2026-09-15, found and root-caused by Ahsan himself)**: Bug 1's original fix used
 * `https://www.youtube.com` as both the WebView's base URL and the implicit origin — a common
 * WebView workaround, but not a genuine origin, and still left the page's actual referrer
 * unset/inconsistent (the same class of problem as error 153 above, just not fully resolved).
 * Ahsan fixed this properly: [playerOrigin] derives a real, consistent origin from the app's own
 * package name, used as BOTH `loadDataWithBaseURL`'s base URL AND the `origin`/`widget_referrer`
 * values passed into `playerVars` — matching what the IFrame API's own reference docs describe
 * (the origin should be the URL scheme + host of the actual embedding page) instead of spoofing
 * YouTube's own domain. He also added a `<meta name="referrer">` tag so the page sends a real,
 * consistent referrer header alongside that origin. Two more contributing settings gaps he closed
 * at the same time: `setLayerType(View.LAYER_TYPE_HARDWARE, null)` (some WebView/Chromium builds,
 * including OEM ones, need an explicit hardware layer to render `<video>` element frames at all —
 * without it, playback can silently fail even once the API otherwise initializes correctly) and
 * `CookieManager.setAcceptThirdPartyCookies` (some of the IFrame API's own session/analytics
 * cookies are third-party from the WebView's declared origin, and are blocked by default).
 *
 * **Bug 2b (also fixed by Ahsan, same round)**: [WebViewClient.shouldOverrideUrlLoading] had been
 * unconditionally blocking every navigation (added earlier to stop a hypothesized "Watch on
 * YouTube" fallback link from hijacking the WebView) — but the IFrame player's own internal embed
 * iframe is itself loaded via a frame navigation, which a blanket block also silently prevented,
 * regardless of origin/referrer correctness. [isYoutubeHost] now allows navigation to YouTube's own
 * domain family (needed for the player itself, thumbnails, and video segment delivery) while still
 * blocking anything else. In hindsight this was likely the dominant cause of "every video fails to
 * play", more so than the origin/referrer issue above — both needed fixing regardless.
 *
 * Together, Bug 2 and 2b are why the WebChromeClient.onShowCustomView/
 * WebChromeClient.onHideCustomView host (added the previous round, kept unchanged here — see
 * FrameLayout usage below) turned out not to be sufficient by itself: it fixes a real, separate
 * WebView limitation (some builds route ordinary inline playback through the native "custom view"
 * mechanism even for `playsinline` video) but couldn't have compensated for the embed iframe never
 * loading in the first place.
 *
 * **Bug 3 (found 2026-09-12, narrower — movie id 980431, an unreleased 2026 title)**: even with
 * the above fixed, a specific video can still genuinely fail to embed — most likely error 101/150
 * ("embedding disabled by request"), which some studios set on official trailers/teasers to keep
 * views on youtube.com itself. That's a real, permanent restriction on that video, not a bug in
 * this WebView's setup. onPlayerError bridges the JS `onError` event back to Kotlin via a
 * `@JavascriptInterface` (the callback fires on a WebView-owned thread, hence the
 * Handler-to-main-thread hop) so this composable can show a message that actually distinguishes
 * "embedding disabled" from a generic failure, rather than a single flat string.
 */
@Composable
private fun EmbeddedYouTubePlayer(videoId: String, modifier: Modifier = Modifier) {
    var playbackErrorCode by remember(videoId) { mutableStateOf<Int?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowContentAccess = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    }
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    webViewClient = object : WebViewClient() {
                        // The IFrame player's own embed iframe is itself loaded via a frame
                        // navigation — allow navigation within YouTube's own domain family (the
                        // player, thumbnails, video segment delivery) while still blocking
                        // anything else (e.g. an ad or fallback link trying to take over the
                        // WebView with a full third-party page). See this composable's doc, Bug 2b.
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            return !isYoutubeHost(request.url.host)
                        }

                        @Deprecated("Deprecated in Java")
                        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                            return !isYoutubeHost(Uri.parse(url ?: return true).host)
                        }
                    }
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onPlayerError(code: Int) {
                                // @JavascriptInterface methods run on a WebView-internal thread, not
                                // the main thread — hop over before touching Compose state.
                                mainHandler.post { playbackErrorCode = code }
                            }
                        },
                        "AndroidPlayerBridge"
                    )
                    tag = videoId
                }
                // Root view returned to Compose: a FrameLayout holding the WebView, purely so the
                // WebChromeClient below has a container to host YouTube's native "custom view" when
                // starting playback needs one — see this composable's doc for why that turned out
                // to be necessary (though not sufficient by itself) on Ahsan's device.
                FrameLayout(context).apply {
                    val container = this
                    addView(webView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                    webView.webChromeClient = object : WebChromeClient() {
                        private var customView: View? = null
                        private var customViewCallback: CustomViewCallback? = null

                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (view == null) return
                            if (customView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            customView = view
                            customViewCallback = callback
                            webView.visibility = View.GONE
                            container.addView(
                                view,
                                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                            )
                        }

                        override fun onHideCustomView() {
                            val view = customView ?: return
                            container.removeView(view)
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                            webView.visibility = View.VISIBLE
                        }
                    }
                    val origin = playerOrigin(context)
                    webView.loadDataWithBaseURL(origin, iframePlayerHtml(videoId, origin), "text/html", "utf-8", null)
                }
            },
            update = { container ->
                // Only reload if the video actually changed — avoids a pointless reload/flicker on
                // every recomposition of this composable. loadDataWithBaseURL's own url is always
                // the constant base origin, not something that reflects videoId, hence the tag kept
                // on the WebView itself (the container is a plain, otherwise-untagged FrameLayout).
                val webView = container.getChildAt(0) as WebView
                val origin = playerOrigin(webView.context)
                if (webView.tag != videoId) {
                    webView.tag = videoId
                    playbackErrorCode = null
                    webView.loadDataWithBaseURL(origin, iframePlayerHtml(videoId, origin), "text/html", "utf-8", null)
                }
            },
            onRelease = { container ->
                // Stops playback/audio immediately and releases the WebView's resources when this
                // composable leaves composition (dialog dismissed) — added by Ahsan, 2026-09-15, to
                // fix a real leak/background-audio risk the previous version had (nothing explicitly
                // destroyed the WebView on dispose).
                val webView = container.getChildAt(0) as WebView
                webView.loadUrl("about:blank")
                webView.destroy()
            }
        )

        playbackErrorCode?.let { code ->
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = youtubePlaybackErrorMessage(code),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

/** Matches `host` against YouTube's own domain family: an exact match or a proper subdomain (a
 *  leading dot before the suffix) — NOT a bare [String.endsWith], which would also match an
 *  unrelated domain like "evilyoutube.com" or "notgoogle.com" that merely ends with the same
 *  characters. */
private fun isYoutubeHost(host: String?): Boolean {
    val h = host.orEmpty()
    val allowedDomains = listOf("youtube.com", "youtube-nocookie.com", "youtu.be", "ytimg.com", "ggpht.com", "googlevideo.com", "google.com")
    return allowedDomains.any { domain -> h == domain || h.endsWith(".$domain") }
}

private fun playerOrigin(context: Context): String =
    "https://${context.packageName}"   // https://com.ahsan.movieapp

/**
 * Maps a YouTube IFrame API `onError` code
 * (see https://developers.google.com/youtube/iframe_api_reference#onError) to a message that tells
 * Ahsan (and eventually end users) whether this is a permanent, nothing-to-fix-in-app restriction
 * (embedding disabled) or a more generic failure.
 */
@Composable
private fun youtubePlaybackErrorMessage(code: Int): String = when (code) {
    100 -> stringResource(R.string.trailer_error_unavailable)
    101, 150 -> stringResource(R.string.trailer_error_embedding_disabled)
    else -> stringResource(R.string.trailer_error_playback, code)
}

/**
 * A minimal HTML host page for YouTube's IFrame Player API — full-bleed black background so the
 * player fills [EmbeddedYouTubePlayer]'s box exactly, autoplaying (needs
 * `mediaPlaybackRequiresUserGesture = false` on the [WebView], set above) and inline rather than
 * taking over with a native fullscreen view (`playsinline`) — though see [EmbeddedYouTubePlayer]'s
 * doc for why the hosting [WebView] still needs a working `onShowCustomView` regardless. `fs: 0`
 * hides the player's OWN fullscreen button — [TrailerPlayerScreen] now owns fullscreen presentation
 * at the app level instead. `modestbranding: 1` (round #7) asks the IFrame player to suppress its
 * own YouTube-branding button in the player chrome — [TrailerPlayerScreen]'s [YoutubeButton] is
 * already the app's one "open in YouTube" affordance, so the player's built-in one would just be a
 * redundant second copy of the same action. `onError` forwards the YouTube error code to Kotlin via the
 * `AndroidPlayerBridge` interface registered in [EmbeddedYouTubePlayer] rather than rendering its
 * own message in the page — that keeps the message itself (and its styling/localization) on the
 * Kotlin/Compose side. [origin] (see [playerOrigin]) is passed both as this page's own base URL
 * (by the caller, via `loadDataWithBaseURL`) and here as `playerVars.origin`/`widget_referrer`, so
 * they're always consistent — see [EmbeddedYouTubePlayer]'s doc, Bug 2.
 */
private fun iframePlayerHtml(videoId: String, origin: String): String = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <meta name="referrer" content="strict-origin-when-cross-origin">
        <style>
            html, body { margin: 0; padding: 0; background: #000; overflow: hidden; height: 100%; }
            #player { position: absolute; inset: 0; }
        </style>
    </head>
    <body>
        <div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
            var player = null;
            function resizePlayer() {
                if (player && player.setSize) {
                    player.setSize(window.innerWidth, window.innerHeight);
                }
            }
            function onYouTubeIframeAPIReady() {
                new YT.Player('player', {
                    host: 'https://www.youtube-nocookie.com',
                    width: '100%',
                    height: '100%',
                    videoId: '$videoId',
                    playerVars: {
                        autoplay: 1,
                        playsinline: 1,
                        rel: 0,
                        fs: 1,
                        controls: 1,
                        modestbranding: 1,
                        origin: '$origin',
                        widget_referrer: '$origin'
                    },
                    events: {
                        onReady: function() { resizePlayer(); },
                        onError: function(e) {
                            if (window.AndroidPlayerBridge) {
                                AndroidPlayerBridge.onPlayerError(e.data);
                            }
                        }
                    }
                });
            }
            window.addEventListener('resize', resizePlayer);
        </script>
    </body>
    </html>
""".trimIndent()

/** Switches to the real YouTube app if installed, else falls back to opening it in the browser. */
private fun openInYoutube(context: Context, videoId: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId")))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")))
    }
}

/** Text share (title + TMDB page link) via the system share sheet. */
private fun shareText(context: Context, title: String, url: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
