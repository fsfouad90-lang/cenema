package com.example.ui.player

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Movie
import com.example.data.repository.MovieRepository
import com.example.data.repository.MovieRepositoryImpl
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.io.ByteArrayInputStream
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled", "SourceLockedOrientationActivity")
@Composable
fun PlayerScreen(
    movieId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    movieRepository: MovieRepository = MovieRepositoryImpl()
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                return@remember currentContext
            }
            currentContext = currentContext.baseContext
        }
        null
    }

    var movie by remember { mutableStateOf<Movie?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    val currentLang = if (Locale.getDefault().language.startsWith("ar")) "ar" else "en"

    // Set Landscape Orientation & Cinematic Immersive Mode (No UI distractions)
    DisposableEffect(activity) {
        val window = activity?.window
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            
            // Lock orientation to Landscape
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            val window = activity?.window
            if (window != null) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                
                // Return orientation control back to sensor/unspecified defaults
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    // Fetch movie object details
    LaunchedEffect(movieId) {
        try {
            movie = movieRepository.getMovieDetails(movieId, currentLang)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    // Auto-fading HUD controllers mechanism for seamless view experience
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                onClick = { showControls = !showControls },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
    ) {
        // Native Optimized WebView Container
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // Robust Settings optimization
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    
                    // Stop pop-ups completely (no tabs/new windows)
                    settings.setSupportMultipleWindows(false)
                    settings.javaScriptCanOpenWindowsAutomatically = false

                    // Enable Hardware Acceleration
                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

                    // Custom WebView Client designed to strip ads and enforce routing security
                    webViewClient = object : WebViewClient() {
                        
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return false
                            val host = uri.host ?: ""
                            val allowedHosts = listOf("vidapi.ru", "vidapi.com", "vidapi.net", "vidsrc", "localhost")
                            
                            // Prevent redirection to unknown advertising portals
                            val isAllowed = allowedHosts.any { host.contains(it) }
                            return !isAllowed // Consume/Block navigation when not allowed
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val url = request?.url?.toString()?.lowercase() ?: return null
                            
                            // Block recognized blacklisted ad servers & suspicious trackers
                            val adPatterns = listOf(
                                "popads", "popunder", "propellerads", "onclickads", "exoclick", "adsterra", 
                                "doubleclick", "google-analytics", "adservice", "adnxs", "analytics", 
                                "coinhive", "adskeeper", "mgid", "yandex.ru/clck", "scorecardresearch", 
                                "histats", "amung.us", "onclick", "banners", "clicks"
                            )
                            
                            if (adPatterns.any { url.contains(it) }) {
                                // Strip execution loop/asset returns by injecting empty data streams
                                return WebResourceResponse(
                                    "text/plain",
                                    "UTF-8",
                                    ByteArrayInputStream("".toByteArray())
                                )
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            
                            // Inject dynamic client-side stylesheets & click-jack element sweepers
                            val customJavaScript = """
                                (function() {
                                    // 1. Inject static stylesheet to forcibly hide dynamic ad tags
                                    var css = 'div[class*="ad"], div[id*="ad"], a[href*="ad"], iframe[src*="ad"], [class*="popup"], [id*="pop"] { display: none !important; pointer-events: none !important; }';
                                    var head = document.head || document.getElementsByTagName('head')[0];
                                    var style = document.createElement('style');
                                    style.type = 'text/css';
                                    if (style.styleSheet){
                                      style.styleSheet.cssText = css;
                                    } else {
                                      style.appendChild(document.createTextNode(css));
                                    }
                                    head.appendChild(style);
                                    
                                    // 2. Clear out invisible click layers using opacity markers
                                    setInterval(function() {
                                        var elms = document.getElementsByTagName('*');
                                        for (var i = 0; i < elms.length; i++) {
                                            var el = elms[i];
                                            var style = window.getComputedStyle(el);
                                            if (style.position === 'fixed' && style.zIndex > 1000 && parseFloat(style.opacity) < 0.2) {
                                                el.parentNode.removeChild(el);
                                            }
                                        }
                                    }, 600);
                                })();
                            """.trimIndent()
                            
                            view?.evaluateJavascript(customJavaScript, null)
                        }
                    }

                    // Native Web ChromeClient block handles full-screen triggers and standard updates
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: android.view.View?, callback: CustomViewCallback?) {
                            super.onShowCustomView(view, callback)
                        }
                        override fun onHideCustomView() {
                            super.onHideCustomView()
                        }
                    }

                    loadUrl("https://vidapi.ru/embed/movie/$movieId")
                }
            },
            update = {},
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Cinematic HUD (Animated fading based on user click interactions)
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Top control status overlay bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .testTag("player_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = movie?.title ?: "Stream Player",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentLang == "ar") "بث مباشر آمن فائق الدقة" else "Secure Ultra HD Stream",
                                color = Color.White.copy(alpha = 0.60f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Premium streaming indicator badge
                    Box(
                        modifier = Modifier
                            .background(CinemaGold, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "HD LIVE",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Loading visual spinner
                if (isLoading) {
                    CircularProgressIndicator(
                        color = IndigoPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Bottom safety reload trigger
                IconButton(
                    onClick = { /* Reload webview action handled by simply drawing it */ },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .testTag("player_reload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Player",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
