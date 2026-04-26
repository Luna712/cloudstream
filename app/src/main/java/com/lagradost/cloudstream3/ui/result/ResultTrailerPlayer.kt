package com.lagradost.cloudstream3.ui.result

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.lagradost.cloudstream3.CommonActivity.screenHeight
import com.lagradost.cloudstream3.CommonActivity.screenWidth
import com.lagradost.cloudstream3.CommonActivity.screenWidthWithOrientation
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentResultSwipeBinding
import com.lagradost.cloudstream3.ui.player.CSPlayerEvent
import com.lagradost.cloudstream3.ui.player.CSPlayerLoading
import com.lagradost.cloudstream3.ui.player.PlayerEventSource
import com.lagradost.cloudstream3.ui.player.SubtitleData
import com.lagradost.cloudstream3.utils.BackPressedCallbackHelper.attachBackPressedCallback
import com.lagradost.cloudstream3.utils.BackPressedCallbackHelper.detachBackPressedCallback
import com.lagradost.cloudstream3.utils.UIHelper.fixSystemBarsPadding
import com.lagradost.cloudstream3.utils.UIHelper.getStatusBarHeight

class ResultTrailerPlayer : BaseFragment<TrailerCustomLayoutBinding>(
    BindingCreator.Inflate(TrailerCustomLayoutBinding::inflate)
), PlayerView.Callbacks {

    private var lockRotation = false
    private var isFullScreenPlayer = false
    private var hasPipModeSupport = false

    companion object {
        const val TAG = "ResultTrailerPlayer"
    }

    private var playerWidthHeight: Pair<Int, Int>? = null
    private var introVisible = true

    private var player: IPlayer = CS3IPlayer()
    private var hasPipModeSupport: Boolean = false
    private var isFullScreenPlayer: Boolean = true
    private var lockRotation: Boolean = true
    private var isShowing: Boolean = false

    private var playerHostView: PlayerView? = null

    private fun uiReset() {
        isShowing = false
        updateUIVisibility()
    }

    override fun fixLayout(view: View) {
        fixSystemBarsPadding(view)
    }

    var currentTrailers: List<Pair<ExtractorLink, String>> = emptyList()
    var currentTrailerIndex = 0

    override fun nextMirror() {
        currentTrailerIndex++
        loadTrailer()
    }

    override fun hasNextMirror(): Boolean {
        return currentTrailerIndex + 1 < currentTrailers.size
    }

    override fun playerError(exception: Throwable) {
        if (player.getIsPlaying()) { // because we don't want random toasts in player
            playerHostView?.playerError(exception)
        } else nextMirror()
    }

    private fun loadTrailer(index: Int? = null) {
        val isSuccess =
            currentTrailers.getOrNull(index ?: currentTrailerIndex)
                ?.let { (extractedTrailerLink, _) ->
                    context?.let { ctx ->
                        player.onPause()
                        player.loadPlayer(
                            ctx,
                            false,
                            extractedTrailerLink,
                            null,
                            startPosition = 0L,
                            subtitles = emptySet(),
                            subtitle = null,
                            autoPlay = false,
                            preview = false
                        )
                        true
                    } ?: run {
                        false
                    }
                } ?: run {
                false
            }

        val turnVis = !isSuccess && !isFullScreenPlayer
        resultBinding?.apply {
            // If we load a trailer, then cancel the big logo and only show the small title
            if (isSuccess) {
                // This is still a bit of a race condition, but it should work if we have the
                // trailers observe after the page observe!
                bindLogo(
                    url = null,
                    headers = null,
                    logoView = backgroundPosterWatermarkBadge,
                    titleView = resultTitle
                )
            }
            resultSmallscreenHolder.isVisible = turnVis
            resultPosterBackgroundHolder.apply {
                val fadeIn: Animation = AlphaAnimation(alpha, if (turnVis) 1.0f else 0.0f).apply {
                    interpolator = DecelerateInterpolator()
                    duration = 200
                    fillAfter = true
                }
                clearAnimation()
                startAnimation(fadeIn)
            }

            // We don't want the trailer to be focusable if it's not visible
            resultSmallscreenHolder.descendantFocusability = if (isSuccess) {
                ViewGroup.FOCUS_AFTER_DESCENDANTS
            } else ViewGroup.FOCUS_BLOCK_DESCENDANTS
            oldbinding?.resultFullscreenHolder?.isVisible = !isSuccess && isFullScreenPlayer
        }
    }

    internal fun setTrailers(trailers: List<Pair<ExtractorLink, String>>?) {
        context?.updateHasTrailers()
        if (!LoadResponse.isTrailersEnabled) return
        currentTrailers = trailers?.sortedBy { -it.first.quality } ?: emptyList()
        loadTrailer()
    }

    // Single-tap on empty player area: toggle controls.
    override fun onSingleTap() {
        if (!introVisible) {
            if (isShowing) uiReset() else showControls()
        }
    }

    private fun showControls() {
        if (introVisible) return
        isShowing = true
        updateUIVisibility()
        playerHostView?.scheduleAutoHide()
    }

    override fun isValidTouch(rawX: Float, rawY: Float): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = binding.playerHolder.rootWindowInsets
                ?.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()) ?: return true
            return rawY > insets.top && rawX < (screenWidthWithOrientation - insets.right)
        }
        return rawY > (context?.getStatusBarHeight() ?: 0)
    }

    override fun isUIShowing(): Boolean = isShowing

    override fun onAutoHideUI() {
        if (player.getIsPlaying()) uiReset()
    }

    override fun onHidePlayerUI() = uiReset()

    override fun nextEpisode() {}
    override fun prevEpisode() {}
    override fun playerPositionChanged(position: Long, duration: Long) {}
    override fun nextMirror() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        uiReset()
        fixPlayerSize()
    }

    private fun fixPlayerSize() {
        oldbinding?.apply {
            if (isFullScreenPlayer) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ViewCompat.setOnApplyWindowInsetsListener(root, null)
                    root.overlay.clear()
                }
                root.setPadding(0, 0, 0, 0)
            } else {
                fixSystemBarsPadding(root)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ViewCompat.requestApplyInsets(root)
                }
            }
        }

        playerWidthHeight?.let { (w, h) ->
            if (w <= 0 || h <= 0) return@let

            val orientation = context?.resources?.configuration?.orientation ?: return

            val sw = if (orientation == Configuration.ORIENTATION_LANDSCAPE) screenWidth else screenHeight

            // resultBinding?.resultSmallscreenHolder?.isVisible = !isFullScreenPlayer
            // oldbinding?.resultFullscreenHolder?.isVisible = isFullScreenPlayer

            val to = sw * h / w

            /*resultBinding?.fragmentTrailer?.playerBackground?.apply {
                isVisible = true
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    if (isFullScreenPlayer) FrameLayout.LayoutParams.MATCH_PARENT else to
                )
            }*/

            binding.playerIntroPlay.apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    /*resultBinding?.resultTopHolder?.measuredHeight ?:*/ FrameLayout.LayoutParams.MATCH_PARENT
                )
            }

            if (binding.playerIntroPlay.isGone == true) {
                /*resultBinding?.resultTopHolder?.apply {
                    val anim = ValueAnimator.ofInt(
                        measuredHeight,
                        if (isFullScreenPlayer) ViewGroup.LayoutParams.MATCH_PARENT else to
                    )
                    anim.addUpdateListener { va ->
                        val v = va.animatedValue as Int
                        val lp: ViewGroup.LayoutParams = layoutParams
                        lp.height = v
                        layoutParams = lp
                    }
                    anim.duration = 200
                    anim.start()
                }*/
            }
        }
    }

    override fun playerDimensionsLoaded(width: Int, height: Int) {
        playerWidthHeight = width to height
        fixPlayerSize()
        // Apply autorotation when fullscreen (lockRotation = true).
        // PlayerView already set isVerticalOrientation before this callback fires.
        if (lockRotation) {
            activity?.requestedOrientation = playerHostView?.dynamicOrientation() ?: return
        }
    }

    override fun subtitlesChanged() {}
    override fun embeddedSubtitlesFetched(subtitles: List<SubtitleData>) {}
    override fun onTracksInfoChanged() {}
    override fun exitedPipMode() {}

    override fun onSeekPreviewText(text: String?) {
        binding.playerTimeText.apply {
            isVisible = text != null
            if (text != null) this.text = text
        }
    }

    private fun updateFullscreen(fullscreen: Boolean) {
        isFullScreenPlayer = fullscreen
        lockRotation = fullscreen
        playerHostView?.isFullScreen = fullscreen

        binding.playerFullscreen.setImageResource(
            if (fullscreen) R.drawable.baseline_fullscreen_exit_24 else R.drawable.baseline_fullscreen_24
        )
        if (fullscreen) {
            playerHostView?.enterFullscreen()
            /*oldbinding?.apply {
                resultTopBar.isVisible = false
                resultFullscreenHolder.isVisible = true
                resultMainHolder.isVisible = false
            }
            resultBinding?.fragmentTrailer?.playerBackground?.let { view ->
                (view.parent as ViewGroup?)?.removeView(view)
                oldbinding?.resultFullscreenHolder?.addView(view)
            }*/
        } else {
            /*oldbinding?.apply {
                resultTopBar.isVisible = true
                resultFullscreenHolder.isVisible = false
                resultMainHolder.isVisible = true
                resultBinding?.fragmentTrailer?.playerBackground?.let { view ->
                    (view.parent as ViewGroup?)?.removeView(view)
                    resultBinding?.resultSmallscreenHolder?.addView(view)
                }
            }*/
            playerHostView?.exitFullscreen()
        }
        fixPlayerSize()
        uiReset()

        if (isFullScreenPlayer) {
            activity?.attachBackPressedCallback("ResultTrailerPlayer") { updateFullscreen(false) }
        } else {
            activity?.detachBackPressedCallback("ResultTrailerPlayer")
        }
    }

    private fun updateUIVisibility() {
        binding.apply {
            playerGoBackHolder.isVisible = false
            val controlsVisible = isShowing && !introVisible
            playerTopHolder.isVisible = controlsVisible
            playerVideoHolder.isVisible = controlsVisible
            shadowOverlay.isVisible = controlsVisible
            playerPausePlayHolderHolder.isVisible =
                controlsVisible && playerHostView?.currentPlayerStatus != CSPlayerLoading.IsBuffering
        }
        // Fade center controls in/out; also resets stale fillAfter alpha from seek animations.
        playerHostView?.gestureHelper?.animateCenterControls(if (isShowing && !introVisible) 1f else 0f)
    }

    override fun playerStatusChanged() {
        if (introVisible) {
            binding.playerPausePlayHolderHolder.isVisible = false
        }
    }

    override fun onBindingCreated(binding: TrailerCustomLayoutBinding) {
        /*FragmentResultSwipeBinding.bind(root).let { bind ->
            resultBinding = bind.fragmentResult
            recommendationBinding = bind.resultRecommendations
            syncBinding = bind.resultSync
            binding = bind
        }*/
        val ctx = context ?: return
        playerHostView = PlayerView(ctx)
        playerHostView?.player = player
        playerHostView?.hasPipModeSupport = hasPipModeSupport
        playerHostView?.callbacks = this
        playerHostView?.bindViews(binding.root)
        playerHostView?.initialize()

        playerHostView?.videoOutline = binding.videoOutline
        playerHostView?.requestUpdateBrightnessOverlayOnNextLayout()

        binding.playerFullscreen.setOnClickListener { updateFullscreen(!isFullScreenPlayer) }
        updateFullscreen(isFullScreenPlayer)
        uiReset()

        binding.playerIntroPlay.setOnClickListener {
            binding.playerIntroPlay.isGone = true
            introVisible = false
            player.handleEvent(CSPlayerEvent.Play, PlayerEventSource.UI)
            fixPlayerSize()
            showControls()
        }

        binding.apply {
            playerOpenSource.setOnClickListener {
                currentTrailers.getOrNull(currentTrailerIndex)?.let { (_, ogTrailerLink) ->
                    context?.openBrowser(ogTrailerLink)
                }
            }
        }
    }

    override fun onPause() {
        playerHostView?.releaseKeyEventListener()
        super.onPause()
    }

    override fun onResume() {
        context?.let { ctx ->
            playerHostView?.onResume(ctx)
            playerHostView?.setupKeyEventListener()
        }
        super.onResume()
    }

    override fun onStop() {
        playerHostView?.onStop()
        super.onStop()
    }

    override fun onDestroyView() {
        playerHostView?.release()
        super.onDestroyView()
    }
}
