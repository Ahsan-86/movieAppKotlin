package com.ahsan.movieapp.ui.components

import com.ahsan.movieapp.R

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.OrientationEventListener
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.content.pm.ActivityInfo


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
 *    behavior once landscape is force-locked). Superseded by round #10: rotating back to portrait
 *    now exits too, and the landscape lock is only applied (and then released) for button entry.
 * 3. The app-bar "Open in YouTube" action was later removed (Ahsan, 2026-10-05): the embed's own
 *    "Watch on YouTube" button already hands off to the YouTube app / browser (see
 *    openYoutubeExternally), so a second app-level copy was redundant.
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

    val portraitReleaser = remember(activity) { activity?.let { PortraitReleaser(it) } }

    fun restorePortrait() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    // Leaving fullscreen (or this screen) while the phone is still sideways: force portrait until
    // the phone is physically upright, instead of leaving the portrait layout stretched in landscape.
    fun exitToPortrait() {
        val isLandscape = activity?.resources?.configuration?.orientation == Configuration.ORIENTATION_LANDSCAPE
        when {
            isLandscape && portraitReleaser != null -> portraitReleaser.start()
            portraitReleaser?.pending != true -> restorePortrait()
        }
    }

    // Orientation drives fullscreen both ways (round #10): landscape enters, portrait exits. The
    // embed's button and system back write the same isFullscreen state.
    LaunchedEffect(configuration.orientation) {
        isFullscreen = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    DisposableEffect(isFullscreen) {
        val window = activity?.window
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, !isFullscreen) }
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        var landscapeUnlocker: OrientationEventListener? = null

        if (isFullscreen) {
            portraitReleaser?.cancel()
            // Button-entered from portrait: force landscape, then hand control back to the sensor
            // once the phone is physically landscape, so rotating back to portrait exits. A
            // rotation-entered fullscreen is never locked.
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                landscapeUnlocker = object : OrientationEventListener(context) {
                    override fun onOrientationChanged(degrees: Int) {
                        if (degrees in 70..110 || degrees in 250..290) {
                            // SENSOR, not UNSPECIFIED: with system auto-rotate off, UNSPECIFIED would
                            // snap straight back to portrait and drop out of fullscreen.
                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
                            disable()
                        }
                    }
                }.apply { if (canDetectOrientation()) enable() }
            }
            bars?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            bars?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            exitToPortrait()
            bars?.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            landscapeUnlocker?.disable()
            bars?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Leaving the screen entirely (back button, or popped some other way) must always restore
    // portrait and the system bars, even if the user backs out while mid-fullscreen — otherwise
    // the Activity (shared with every other screen in the app, unlike the old per-dialog window)
    // would stay stuck landscape/edge-to-edge after returning to Detail.
    DisposableEffect(Unit) {
        onDispose {
            exitToPortrait()
            activity?.window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, true)
                WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler(enabled = isFullscreen) { isFullscreen = false }

    // The embed's OWN fullscreen button (fs: 1 in iframePlayerHtml) is the single fullscreen control
    // now — the old app-level overlay button sat on top of it (and, in portrait, floated at the
    // bottom of the screen, far from the video), which is why tapping the embed's button appeared
    // dead. EmbeddedYouTubePlayer bridges that button to this screen's isFullscreen state.
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
                // Black only while fullscreen. In portrait only the 16:9 player strip is black —
                // the rest of the screen keeps the normal theme surface instead of a big black void.
                .then(if (isFullscreen) Modifier.background(Color.Black) else Modifier)
        ) {
            // Single, unconditional call site — see this composable's doc, round #7 point 4 — only
            // the modifier (size/position) differs between fullscreen and not.
            EmbeddedYouTubePlayer(
                videoId = videoId,
                isFullscreen = isFullscreen,
                onFullscreenChange = { isFullscreen = it },
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize()
                } else {
                    // Top of screen, full width, 16:9 — matches the real YouTube app's own player
                    // sizing for a playing video (Ahsan's original "match YouTube app dimensions" ask).
                    Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                }
            )
        }
    }
}

/**
 * Holds the Activity in portrait after fullscreen is left while the phone is still sideways, then
 * hands orientation back to the system once the phone is physically upright. Deliberately outlives
 * [TrailerPlayerScreen]: backing out sideways must not leave Detail/Explore in landscape. Stops
 * itself once upright, on [cancel], or when the Activity is finishing.
 */
private class PortraitReleaser(private val activity: Activity) : OrientationEventListener(activity) {
    var pending = false
        private set

    // Temporary lock, released as soon as the phone is upright (Android 16 ignores it on large screens).
    @SuppressLint("SourceLockedOrientationActivity")
    fun start() {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (canDetectOrientation()) {
            pending = true
            enable()
        } else {
            release()
        }
    }

    fun cancel() {
        pending = false
        disable()
    }

    private fun release() {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        cancel()
    }

    override fun onOrientationChanged(degrees: Int) {
        when {
            activity.isFinishing || activity.isDestroyed -> cancel()
            degrees in 0..30 || degrees in 330..359 -> release()
        }
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
private fun EmbeddedYouTubePlayer(
    videoId: String,
    isFullscreen: Boolean,
    onFullscreenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var playbackErrorCode by remember(videoId) { mutableStateOf<Int?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val currentOnFullscreenChange by rememberUpdatedState(onFullscreenChange)
    val currentIsFullscreen by rememberUpdatedState(isFullscreen)
    // Lets Compose-side exits (system back, rotation handling) close the embed's native fullscreen
    // "custom view" — the WebChromeClient below owns that view, so it publishes a hide() here.
    val customViewHost = remember { CustomViewHost() }

    LaunchedEffect(isFullscreen) {
        if (!isFullscreen && customViewHost.isShowing) customViewHost.hide?.invoke()
    }

    // Stop playback when the app goes to the background (home button, screen off, another app):
    // a WebView keeps playing audio on its own otherwise. Paused, not resumed — same as the real
    // YouTube app, the user taps play again on return.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val webView = customViewHost.webView ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    webView.evaluateJavascript("if (player && player.pauseVideo) player.pauseVideo();", null)
                    webView.onPause()
                }
                Lifecycle.Event.ON_RESUME -> webView.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                // JS is required by the YouTube IFrame API; the page is our own HTML and navigation
                // is restricted to YouTube hosts (shouldOverrideUrlLoading below).
                @SuppressLint("SetJavaScriptEnabled")
                val webView = WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.allowContentAccess = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    webViewClient = object : WebViewClient() {
                        // The IFrame player's own embed iframe is itself loaded via a frame
                        // navigation — allow navigation within YouTube's own domain family (the
                        // player, thumbnails, video segment delivery) while still blocking
                        // anything else (e.g. an ad or fallback link trying to take over the
                        // WebView with a full third-party page). See this composable's doc, Bug 2b.
                        // A "Watch on YouTube" link (youtube.com/watch, youtu.be) leaves the WebView
                        // for the YouTube app / browser; the embed itself stays here.
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (isYoutubeWatchUrl(request.url)) {
                                openYoutubeExternally(view.context, request.url)
                                return true
                            }
                            return !isYoutubeHost(request.url.host)
                        }
                    }
                    // Lets target=_blank links reach onCreateWindow below instead of silently
                    // loading into this WebView.
                    settings.setSupportMultipleWindows(true)
                    addJavascriptInterface(
                        object {
                            // Called from the page's JS via the AndroidPlayerBridge interface.
                            @Suppress("unused")
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
                    customViewHost.webView = this
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
                            // Rotation-entered fullscreen has no custom view up, so the embed's
                            // button still reads "enter" — treat that tap as exit (one tap, not two).
                            if (currentIsFullscreen && customView == null) {
                                callback?.onCustomViewHidden()
                                currentOnFullscreenChange(false)
                                return
                            }
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
                            // The embed's own fullscreen button was tapped: grow this player's box to
                            // the whole screen (hide bars, lock landscape) so the custom view above
                            // actually fills it instead of staying inside the small 16:9 strip.
                            customViewHost.isShowing = true
                            customViewHost.hide = { onHideCustomView() }
                            currentOnFullscreenChange(true)
                        }

                        override fun onHideCustomView() {
                            val view = customView ?: return
                            container.removeView(view)
                            customView = null
                            customViewCallback?.onCustomViewHidden()
                            customViewCallback = null
                            webView.visibility = View.VISIBLE
                            customViewHost.isShowing = false
                            currentOnFullscreenChange(false)
                        }

                        // target=_blank ("Watch on YouTube" in the player chrome): the URL isn't
                        // known yet, so hand the page a throwaway WebView, catch its first
                        // navigation, and send YouTube links out to the app / browser. Non-gesture
                        // popups (ads) are refused.
                        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
                            if (!isUserGesture) return false
                            val popup = WebView(view.context)
                            popup.webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                                    if (isYoutubeHost(request.url.host)) openYoutubeExternally(v.context, request.url)
                                    mainHandler.post { v.destroy() }
                                    return true
                                }
                            }
                            (resultMsg.obj as WebView.WebViewTransport).webView = popup
                            resultMsg.sendToTarget()
                            return true
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
                customViewHost.webView = null
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

/** Plain holder so the WebChromeClient (created inside AndroidView's factory) and Compose effects
 *  can share "is the embed's native fullscreen view up, and how do I close it" plus the live
 *  [WebView] (for lifecycle pause/resume). */
private class CustomViewHost {
    var isShowing: Boolean = false
    var hide: (() -> Unit)? = null
    var webView: WebView? = null
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

/** A link to a YouTube watch page (as opposed to the /embed player, which stays in the WebView). */
private fun isYoutubeWatchUrl(uri: Uri): Boolean {
    val host = uri.host.orEmpty()
    val isYoutubeCom = host == "youtube.com" || host.endsWith(".youtube.com")
    return host == "youtu.be" || (isYoutubeCom && uri.path.orEmpty().startsWith("/watch"))
}

/** Opens a YouTube URL in the YouTube app if installed, else the external browser. */
private fun openYoutubeExternally(context: Context, uri: Uri) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(YOUTUBE_PACKAGE))
    } catch (ignored: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (ignored: ActivityNotFoundException) {
            // No browser either — nothing sensible to open.
        }
    }
}

private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

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
 * doc for why the hosting [WebView] still needs a working `onShowCustomView` regardless. `fs: 1`
 * shows the player's OWN fullscreen button, which is now the single fullscreen control — its
 * onShowCustomView/onHideCustomView events are bridged to [TrailerPlayerScreen]'s isFullscreen state.
 * `modestbranding: 1` (round #7) is a leftover; YouTube no longer honors it, and the player's own
 * "Watch on YouTube" button is now the app's only "open in YouTube" affordance. `onError` forwards the YouTube error code to Kotlin via the
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
                player = new YT.Player('player', {
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

/** Text share (title + TMDB page link) via the system share sheet. */
private fun shareText(context: Context, title: String, url: String) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    }
    context.startActivity(Intent.createChooser(sendIntent, null))
}
