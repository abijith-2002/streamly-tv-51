package app.claro.tv.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.claro.tv.data.repository.ContentRepository
import app.claro.tv.data.repository.FakeContentRepository
import app.claro.tv.models.ContentItem
import app.claro.tv.models.TvChannel
import app.claro.tv.viewmodel.HomeViewModel
import app.claro.tv.viewmodel.HomeViewModelFactory
import app.claro.tv.viewmodel.UiState
import app.claro.tv.views.ContinueWatchingCard
import app.claro.tv.views.TopNavBar
import app.claro.tv.views.TvChannelCard
import app.claro.tv.views.HeroCard

/**
 * PUBLIC_INTERFACE
 * HomeFragment - The main home screen for the Android TV app.
 * Displays hero banner, Continue Watching rail, and TV Channels rail.
 * Implements D-pad navigation with proper focus management and overscan-safe layout.
 *
 * Design based on AAF_inicio Copy 2 (screen 2001:3396) with pixel-accurate dimensions
 * for 1920x1080 resolution.
 *
 * Now integrated with real API data via ViewModel and Repository pattern.
 */
class HomeFragment : Fragment() {

    private lateinit var rootScrollView: ScrollView
    private lateinit var rootContainer: LinearLayout
    private lateinit var continueWatchingRail: LinearLayout
    private lateinit var tvChannelsRail: LinearLayout
    private lateinit var continueWatchingLoadingView: View
    private lateinit var tvChannelsLoadingView: View
    private var topNavBarComposeView: ComposeView? = null
    private var topNavBarId: Int = View.NO_ID

    // Hero carousel container and items
    private var heroBannerView: View? = null
    private var heroBannerId: Int = View.NO_ID
    private var heroScrollView: HorizontalScrollView? = null
    private var heroRail: LinearLayout? = null
    private val heroCards: MutableList<HeroCard> = mutableListOf()
    private val heroTitles: List<String> = listOf(
        "Destacado 1", "Destacado 2", "Destacado 3"
    )

    // Carousel sizing (dp)
    private val heroCardWidthDp: Int = 872
    private val heroCardHeightDp: Int = 222
    // Slightly increase spacing between cards to subtly separate hero items
    private val heroCardSpacingDp: Int = 12
    // Enforce 34dp peek for previous/next cards
    private val heroPeekDp: Int = 34

    // Derived px values (computed in setupHeroBanner / after layout)
    private var heroCardWidthPx: Int = 0
    private var heroCardHeightPx: Int = 0
    private var heroCardSpacingPx: Int = 0
    private var heroPeekPx: Int = 0
    private var heroSidePaddingPx: Int = 0 // dynamic symmetric content padding to allow equal peeking

    private var heroCurrentIndex: Int = 0
    private val heroAutoScrollIntervalMs: Long = 3000L
    private val heroAutoScrollResumeIdleMs: Long = 4000L
    private val heroHandler = Handler(Looper.getMainLooper())

    // Smooth scroll config for consistent easing/duration on TV
    private val heroScrollDurationMs: Long = 240L
    private val heroScrollInterpolator = AccelerateDecelerateInterpolator()

    private var autoScrollPausedByUser: Boolean = false
    private var heroScrollAnimator: android.animation.ValueAnimator? = null

    private val heroAutoScrollRunnable = object : Runnable {
        override fun run() {
            // Do nothing if paused by user or view not ready
            if (autoScrollPausedByUser || heroCards.isEmpty() || heroScrollView == null) {
                // Check again later
                heroHandler.postDelayed(this, heroAutoScrollIntervalMs)
                return
            }
            // Advance index and loop
            val nextIndex = (heroCurrentIndex + 1) % heroCards.size
            // Use the same routine to center and then shift focus, to keep behavior consistent
            centerHeroAt(nextIndex, animate = true)
            heroCards.getOrNull(nextIndex)?.requestFocus()
            heroHandler.postDelayed(this, heroAutoScrollIntervalMs)
        }
    }

    // Focus gate: disable rails until user presses DPAD_DOWN
    private var railsFocusEnabled: Boolean = false

    private lateinit var viewModel: HomeViewModel
    private lateinit var repository: ContentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repository - force fake data to disable all API/network calls
        repository = FakeContentRepository()

        // Create ViewModel
        val factory = HomeViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Root scroll view for vertical scrolling if content exceeds screen height
        rootScrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
            // Prevent scroll view from intercepting D-pad events initially
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        rootContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.parseColor("#121212"))
            // Overscan-safe padding: 88dp left/right, 36dp top, 48dp bottom
            setPadding(dpToPx(88), dpToPx(36), dpToPx(88), dpToPx(48))
            // Ensure hero full-bleed area isn't clipped by the root container
            clipToPadding = false
            clipChildren = false
            // Allow descendants to be focused
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        // Compose Top Navigation - horizontally centered, 18dp from top
        addComposeTopNavBar()

        setupHeroBanner()
        setupContinueWatchingSection()
        setupTvChannelsSection()

        // Initially gate rails until the user explicitly presses DPAD_DOWN
        setRailsFocusable(false)

        // After hero exists, route DOWN from nav directly to the first hero card if available
        if (heroCards.isNotEmpty()) {
            topNavBarComposeView?.nextFocusDownId = heroCards.first().id
        } else if (heroBannerId != View.NO_ID) {
            topNavBarComposeView?.nextFocusDownId = heroBannerId
        }

        rootScrollView.addView(rootContainer)
        return rootScrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Request initial focus on the ComposeView containing TopNavBar
        // The TopNavBar Composable will internally request focus on the search icon
        view.post {
            topNavBarComposeView?.let { composeView ->
                // Make the ComposeView focusable and request focus
                // This will trigger the LaunchedEffect in TopNavBar to focus the search icon
                composeView.isFocusable = true
                composeView.isFocusableInTouchMode = true
                composeView.requestFocus()
            }
        }

        // Observe ViewModel state changes
        observeViewModelStates()

        // Load initial data
        viewModel.loadContinueWatching()
        viewModel.loadTvChannels()
    }

    override fun onResume() {
        super.onResume()
        // Ensure nav regains focus when returning to the screen
        topNavBarComposeView?.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            post { requestFocus() }
        }
        startHeroAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        stopHeroAutoScroll()
        // Cancel any in-flight animator to prevent leaks
        heroScrollAnimator?.cancel()
        heroScrollAnimator = null
    }

    /**
     * Observes ViewModel LiveData and updates UI accordingly.
     */
    private fun observeViewModelStates() {
        // Observe Continue Watching state
        viewModel.continueWatchingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    showContinueWatchingLoading()
                }
                is UiState.Success -> {
                    hideContinueWatchingLoading()
                    updateContinueWatchingRail(state.data)
                }
                is UiState.Error -> {
                    hideContinueWatchingLoading()
                    showErrorWithRetry(state.message, isForContinueWatching = true)
                }
            }
        }

        // Observe TV Channels state
        viewModel.tvChannelsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    showTvChannelsLoading()
                }
                is UiState.Success -> {
                    hideTvChannelsLoading()
                    updateTvChannelsRail(state.data)
                }
                is UiState.Error -> {
                    hideTvChannelsLoading()
                    showErrorWithRetry(state.message, isForContinueWatching = false)
                }
            }
        }
    }

    /**
     * Shows loading indicator for Continue Watching section.
     */
    private fun showContinueWatchingLoading() {
        continueWatchingLoadingView.visibility = View.VISIBLE
        continueWatchingRail.visibility = View.GONE
    }

    /**
     * Hides loading indicator for Continue Watching section.
     */
    private fun hideContinueWatchingLoading() {
        continueWatchingLoadingView.visibility = View.GONE
        continueWatchingRail.visibility = View.VISIBLE
    }

    /**
     * Shows loading indicator for TV Channels section.
     */
    private fun showTvChannelsLoading() {
        tvChannelsLoadingView.visibility = View.VISIBLE
        tvChannelsRail.visibility = View.GONE
    }

    /**
     * Hides loading indicator for TV Channels section.
     */
    private fun hideTvChannelsLoading() {
        tvChannelsLoadingView.visibility = View.GONE
        tvChannelsRail.visibility = View.VISIBLE
    }

    /**
     * Shows error message with retry option.
     */
    private fun showErrorWithRetry(message: String, isForContinueWatching: Boolean) {
        Toast.makeText(
            requireContext(),
            "$message\nTap to retry",
            Toast.LENGTH_LONG
        ).show()

        // Auto-retry after showing error
        view?.postDelayed({
            if (isForContinueWatching) {
                viewModel.retryContinueWatching()
            } else {
                viewModel.retryTvChannels()
            }
        }, 3000)
    }

    /**
     * Updates Continue Watching rail with fetched data.
     */
    private fun updateContinueWatchingRail(items: List<ContentItem>) {
        continueWatchingRail.removeAllViews()

        var firstCardId: Int = View.NO_ID

        items.forEachIndexed { index, item ->
            val card = ContinueWatchingCard(requireContext()).apply {
                // Give each card a stable view id for focus routing
                id = View.generateViewId()
                // Ensure upward focus moves to TopNavBar
                if (topNavBarId != View.NO_ID) {
                    nextFocusUpId = topNavBarId
                }
                // Respect current rails focus gate
                isFocusable = railsFocusEnabled
                isFocusableInTouchMode = railsFocusEnabled
            }
            if (index == 0) {
                firstCardId = card.id
            }
            card.bind(item)
            continueWatchingRail.addView(card)
        }

        // Route DOWN from every hero card to the first continue watching card (if available)
        if (firstCardId != View.NO_ID) {
            heroBannerView?.nextFocusDownId = firstCardId
            heroCards.forEach { it.nextFocusDownId = firstCardId }
        }
    }

    /**
     * Updates TV Channels rail with fetched data.
     */
    private fun updateTvChannelsRail(channels: List<TvChannel>) {
        tvChannelsRail.removeAllViews()

        channels.forEach { channel ->
            val card = TvChannelCard(requireContext()).apply {
                // Give each card a stable view id for focus routing
                id = View.generateViewId()
                // Ensure upward focus moves to TopNavBar
                if (topNavBarId != View.NO_ID) {
                    nextFocusUpId = topNavBarId
                }
                // Respect current rails focus gate
                isFocusable = railsFocusEnabled
                isFocusableInTouchMode = railsFocusEnabled
            }
            card.bind(channel)
            tvChannelsRail.addView(card)
        }
    }

    /**
     * Adds the Compose TopNavBar to the rootContainer.
     * Requirements:
     * - 18dp from the top of the screen
     * - Horizontally centered
     * - Container modifier must be exactly the specified chain inside TopNavBar
     * - Initial focus on search icon
     * - Arrow navigation LEFT/RIGHT cycles within the group; DOWN goes to hero
     */
    private fun addComposeTopNavBar() {
        // We mount a ComposeView above other sections with a top margin of 18dp from the root container top.
        val composeView = ComposeView(requireContext()).apply {
            // Dispose composition to avoid leaks
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                // 18dp from top of the screen; rootContainer has its own padding, so we only add top margin here
                topMargin = dpToPx(18)
                // Reduce bottom margin to tighten spacing to hero (24dp -> 12dp); we'll reduce more at hero container
                bottomMargin = dpToPx(10) // reduced by ~2dp further to shrink nav-to-hero gap by ~6–8dp total
                gravity = Gravity.CENTER_HORIZONTAL
            }

            // Make ComposeView focusable so it can receive and delegate focus to Compose elements
            id = View.generateViewId()
            isFocusable = true
            isFocusableInTouchMode = true

            // Intercept DPAD_DOWN to open the focus gate for rails and move focus to hero card
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                    setRailsFocusable(true)
                    // Focus the first hero card if present; fallback to banner container
                    if (heroCards.isNotEmpty()) {
                        heroCards.first().requestFocus()
                    } else {
                        heroBannerView?.requestFocus()
                    }
                    return@setOnKeyListener true
                }
                false
            }

            setContent {
                // Use Material3 adapter to ensure typography tokens are available
                androidx.compose.material3.MaterialTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        TopNavBar(
                            // Click handlers are stubs
                            onItemClick = { /* no-op for now */ },
                            requestInitialFocus = true
                        )
                    }
                }
            }
        }

        // Store reference for focus management
        topNavBarComposeView = composeView
        topNavBarId = composeView.id
        rootContainer.addView(composeView)
    }

    /**
     * Sets up the hero banner section as a horizontally scrollable carousel.
     * - Fixed container size: 872dp x 222dp
     * - Multiple hero cards: each exactly 872dp x 222dp
     * - Auto-advances every 3 seconds, loops to start
     * - D-pad:
     *   LEFT/RIGHT -> navigate between cards (focusable children)
     *   UP -> Top navigation bar
     *   DOWN -> Content rails (first card in Continue Watching)
     */
    private fun setupHeroBanner() {
        // Compute card px values
        heroCardWidthPx = dpToPx(heroCardWidthDp)
        heroCardHeightPx = dpToPx(heroCardHeightDp)
        heroCardSpacingPx = dpToPx(heroCardSpacingDp)
        heroPeekPx = dpToPx(heroPeekDp)
        heroSidePaddingPx = 0 // will compute after layout from viewport width and desired peek

        // Root hero container: full-bleed width (compensate rootContainer side paddings)
        val container = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heroCardHeightPx
            ).apply {
                // Reduce spacing between nav and hero further (previous 30dp). Decrease an extra ~6–8dp.
                topMargin = dpToPx(24) // final reduction for a tighter nav-to-hero gap without clipping
                // Root container has 88dp start/end padding; use negative margins to allow full width bleed
                marginStart = -dpToPx(88)
                marginEnd = -dpToPx(88)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            // Disable clipping so peeking and any focus effects won't be cut
            clipToPadding = false
            clipChildren = false
            setBackgroundColor(Color.TRANSPARENT)
            isFocusable = false
            isFocusableInTouchMode = false
            id = View.generateViewId()
        }
        heroBannerId = container.id
        heroBannerView = container

        // HorizontalScrollView: full width, dynamic side padding set after layout
        val hsv = object : HorizontalScrollView(requireContext()) {
            // Disable over-scroll glow for TV polish
            override fun overScrollBy(
                deltaX: Int,
                deltaY: Int,
                scrollX: Int,
                scrollY: Int,
                scrollRangeX: Int,
                scrollRangeY: Int,
                maxOverScrollX: Int,
                maxOverScrollY: Int,
                isTouchEvent: Boolean
            ): Boolean {
                return super.overScrollBy(
                    deltaX, deltaY, scrollX, scrollY,
                    scrollRangeX, scrollRangeY, 0, 0, isTouchEvent
                )
            }
        }.apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heroCardHeightPx
            )
            // Padding set after layout based on measured width; do not clip peeks
            setPadding(0, 0, 0, 0)
            clipToPadding = false
            clipChildren = false
            isHorizontalScrollBarEnabled = false
            isSmoothScrollingEnabled = true

            // Route UP to nav from inside carousel
            if (topNavBarId != View.NO_ID) {
                nextFocusUpId = topNavBarId
            }
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

            // Pause auto-scroll when user is interacting with DPAD within the hero
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
                ) {
                    pauseAutoScrollForUserInteraction()
                }
                false
            }
        }
        heroScrollView = hsv

        val rail = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            clipToPadding = false
            clipChildren = false
        }
        heroRail = rail

        // Create hero cards: exactly 872 x 222
        heroTitles.forEachIndexed { index, title ->
            val card = HeroCard(requireContext()).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    heroCardWidthPx,
                    heroCardHeightPx
                ).apply {
                    // spacing between cards to retain 34dp peek visibility
                    marginEnd = heroCardSpacingPx
                }
                // UP should go to top nav
                if (topNavBarId != View.NO_ID) {
                    nextFocusUpId = topNavBarId
                }
                isFocusable = true
                isFocusableInTouchMode = true
                setTitle(title)

                // Ensure card keeps 0dp radius (already enforced in HeroCard) and re-center on focus
                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        val position = heroCards.indexOf(v as HeroCard).let { if (it >= 0) it else index }
                        pauseAutoScrollForUserInteraction()
                        centerHeroAt(position, animate = true)
                    }
                }

                // Ensure re-centering when attached (e.g., after data update or layout pass)
                addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                    val position = heroCards.indexOf(v as HeroCard)
                    if (position == heroCurrentIndex) {
                        centerHeroAt(position, animate = false)
                    }
                }

                // Pause auto-advance when user navigates/presses on a hero card
                setOnKeyListener { _, keyCode, keyEvent ->
                    if (keyEvent.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        pauseAutoScrollForUserInteraction()
                    }
                    false
                }
            }
            heroCards.add(card)
            rail.addView(card)
        }

        hsv.addView(rail)
        container.addView(hsv)

        // Ensure no parent clipping prevents peeking of adjacent cards
        container.clipChildren = false
        container.clipToPadding = false
        rail.clipChildren = false
        rail.clipToPadding = false

        // Route DOWN from nav directly to the first hero card
        heroCards.firstOrNull()?.let { first ->
            topNavBarComposeView?.nextFocusDownId = first.id
        }

        // Add to root
        rootContainer.addView(container)

        // Recompute on layout width changes to maintain exact centering and peeking
        heroScrollView?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val viewportWidth = heroScrollView?.width ?: 0
            if (viewportWidth > 0) {
                // sidePadding = max(0, (viewportWidth - cardWidth)/2 - peek)
                val rawSide = ((viewportWidth - heroCardWidthPx) / 2) - heroPeekPx
                heroSidePaddingPx = rawSide.coerceAtLeast(0)
                heroScrollView?.setPadding(heroSidePaddingPx, 0, heroSidePaddingPx, 0)
                heroScrollView?.clipToPadding = false
                heroScrollView?.clipChildren = false
                // Always keep current card centered after layout changes
                centerHeroAt(heroCurrentIndex, animate = false)
            }
        }

        // After layout, compute side padding from actual viewport width to achieve equal peeking
        container.post {
            val viewportWidth = heroScrollView?.width ?: 0
            if (viewportWidth > 0) {
                // sidePaddingPx = max(0, (viewportWidth - cardWidth)/2 - peek)
                val rawSide = ((viewportWidth - heroCardWidthPx) / 2) - heroPeekPx
                heroSidePaddingPx = rawSide.coerceAtLeast(0)
                heroScrollView?.setPadding(heroSidePaddingPx, 0, heroSidePaddingPx, 0)

                // Ensure no clipping blocks peeking
                heroScrollView?.clipToPadding = false
                heroScrollView?.clipChildren = false

                // Initially center the first card with symmetric peeking (34dp)
                centerHeroAt(heroCurrentIndex, animate = false)
            }
        }

        // Ensure the parent root doesn't clip the full-bleed hero
        rootContainer.clipToPadding = false
        rootContainer.clipChildren = false
    }

    /**
     * Centers the hero carousel at the given index so that the card is centered in the viewport,
     * leaving partial visibility (peeking) of adjacent cards.
     *
     * @param index Target card index
     * @param animate Whether to animate the scroll
     */
    private fun centerHeroAt(index: Int, animate: Boolean) {
        if (heroScrollView == null || heroCards.isEmpty()) return
        heroCurrentIndex = ((index % heroCards.size) + heroCards.size) % heroCards.size

        val h = heroScrollView ?: return
        val viewportWidth = h.width
        if (viewportWidth <= 0) return

        // Ensure side padding honors 34dp peeks on both sides
        if (heroSidePaddingPx <= 0) {
            val rawSide = ((viewportWidth - heroCardWidthPx) / 2) - heroPeekPx
            heroSidePaddingPx = rawSide.coerceAtLeast(0)
            h.setPadding(heroSidePaddingPx, 0, heroSidePaddingPx, 0)
            h.clipToPadding = false
            h.clipChildren = false
        }

        // Compute target scroll X so that target card center aligns with viewport center
        val unitWidth = heroCardWidthPx + heroCardSpacingPx
        val targetCenterX = heroSidePaddingPx + heroCurrentIndex * unitWidth + (heroCardWidthPx / 2f)
        val desiredScrollX = (targetCenterX - (viewportWidth / 2f)).toInt().coerceAtLeast(0)

        if (!animate) {
            h.scrollTo(desiredScrollX, 0)
            return
        }

        // Smooth scroll with ValueAnimator for deterministic duration/easing
        heroScrollAnimator?.cancel()
        val startX = h.scrollX
        val endX = desiredScrollX
        if (startX == endX) return

        heroScrollAnimator = android.animation.ValueAnimator.ofInt(startX, endX).apply {
            duration = heroScrollDurationMs
            interpolator = heroScrollInterpolator
            addUpdateListener { animator ->
                val x = animator.animatedValue as Int
                h.scrollTo(x, 0)
            }
            start()
        }
    }

    /**
     * Pause auto-scroll because the user interacted (e.g., DPAD navigation).
     * Auto-scroll will resume after a short idle.
     */
    private fun pauseAutoScrollForUserInteraction() {
        autoScrollPausedByUser = true
        stopHeroAutoScroll()
        // Schedule resume after idle window
        heroHandler.removeCallbacks(resumeAutoScrollRunnable)
        heroHandler.postDelayed(resumeAutoScrollRunnable, heroAutoScrollResumeIdleMs)
    }

    private val resumeAutoScrollRunnable = Runnable {
        autoScrollPausedByUser = false
        startHeroAutoScroll()
    }

    /**
     * Starts hero auto-scroll (3s interval) if not paused.
     */
    private fun startHeroAutoScroll() {
        heroHandler.removeCallbacks(heroAutoScrollRunnable)
        if (!autoScrollPausedByUser) {
            heroHandler.postDelayed(heroAutoScrollRunnable, heroAutoScrollIntervalMs)
        }
    }

    /**
     * Stops hero auto-scroll immediately.
     */
    private fun stopHeroAutoScroll() {
        heroHandler.removeCallbacks(heroAutoScrollRunnable)
    }

    /**
     * Sets up the Continue Watching section with horizontal scrolling cards.
     * Position: 56dp below hero banner
     * Title: "Seguí viendo" (24sp, white, bold)
     */
    private fun setupContinueWatchingSection() {
        val sectionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(56)
            }
            // Prevent this section from stealing focus on load
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        // Section title
        val title = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
            text = "Seguí viendo"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // Loading indicator
        continueWatchingLoadingView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(312)
            )
            text = "Loading..."
            textSize = 24f
            setTextColor(Color.parseColor("#808080"))
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        // Horizontal scroll view for cards
        val scrollView = HorizontalScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            // Prevent scroll view from stealing focus
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            // Route UP into the TopNavBar if user navigates upwards from within the rail
            if (topNavBarId != View.NO_ID) {
                nextFocusUpId = topNavBarId
            }
            isSmoothScrollingEnabled = true
        }

        continueWatchingRail = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Prevent rail from stealing focus initially; gating is handled separately
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }

        scrollView.addView(continueWatchingRail)
        sectionContainer.addView(title)
        sectionContainer.addView(continueWatchingLoadingView)
        sectionContainer.addView(scrollView)
        rootContainer.addView(sectionContainer)
    }

    /**
     * Sets up the TV Channels section with horizontal scrolling cards.
     * Position: 72dp below Continue Watching
     * Title: "Canales de TV" (24sp, white, bold)
     */
    private fun setupTvChannelsSection() {
        val sectionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(72)
            }
            // Prevent this section from stealing focus on load
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        // Section title
        val title = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
            text = "Canales de TV"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // Loading indicator
        tvChannelsLoadingView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(212)
            )
            text = "Loading..."
            textSize = 24f
            setTextColor(Color.parseColor("#808080"))
            gravity = Gravity.CENTER
            visibility = View.GONE
        }

        // Horizontal scroll view for TV cards
        val scrollView = HorizontalScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            // Prevent scroll view from stealing focus
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            // Route UP into the TopNavBar if user navigates upwards from within the rail
            if (topNavBarId != View.NO_ID) {
                nextFocusUpId = topNavBarId
            }
            isSmoothScrollingEnabled = true
        }

        tvChannelsRail = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Prevent rail from stealing focus initially; gating is handled separately
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        }

        scrollView.addView(tvChannelsRail)
        sectionContainer.addView(title)
        sectionContainer.addView(tvChannelsLoadingView)
        sectionContainer.addView(scrollView)
        rootContainer.addView(sectionContainer)
    }

    /**
     * Focus gate: enable or disable focus for rails. When disabled, rails won't receive focus until DOWN is pressed on nav.
     */
    private fun setRailsFocusable(enabled: Boolean) {
        railsFocusEnabled = enabled

        // Accessibility gating (hide descendants from focus/AT when disabled)
        continueWatchingRail.importantForAccessibility =
            if (enabled) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        tvChannelsRail.importantForAccessibility =
            if (enabled) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

        // Update all existing child cards
        for (i in 0 until continueWatchingRail.childCount) {
            val child = continueWatchingRail.getChildAt(i)
            child.isFocusable = enabled
            child.isFocusableInTouchMode = enabled
        }
        for (i in 0 until tvChannelsRail.childCount) {
            val child = tvChannelsRail.getChildAt(i)
            child.isFocusable = enabled
            child.isFocusableInTouchMode = enabled
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
