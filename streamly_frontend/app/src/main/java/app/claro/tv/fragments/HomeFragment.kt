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
import app.claro.tv.R

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

    // Keep references to the first focusable child in each rail for precise focus routing
    private var continueWatchingFirstCardId: Int = View.NO_ID
    private var tvChannelsFirstCardId: Int = View.NO_ID
    private lateinit var continueWatchingLoadingView: View
    private lateinit var tvChannelsLoadingView: View
    private var topNavBarComposeView: ComposeView? = null
    private var topNavBarId: Int = View.NO_ID

    // Dynamic vertical spacing to ensure top edge-to-nav equals nav-to-hero.
    private var navToHeroGapPx: Int = 0

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
    private val heroCardSpacingDp: Int = 12
    private val heroPeekDp: Int = 34

    // Derived px values
    private var heroCardWidthPx: Int = 0
    private var heroCardHeightPx: Int = 0
    private var heroCardSpacingPx: Int = 0
    private var heroPeekPx: Int = 0
    private var heroSidePaddingPx: Int = 0

    private var heroCurrentIndex: Int = 0
    private val heroAutoScrollIntervalMs: Long = 3000L
    private val heroAutoScrollResumeIdleMs: Long = 4000L
    private val heroHandler = Handler(Looper.getMainLooper())

    private val heroScrollDurationMs: Long = 240L
    private val heroScrollInterpolator = AccelerateDecelerateInterpolator()

    private var autoScrollPausedByUser: Boolean = false
    private var heroScrollAnimator: android.animation.ValueAnimator? = null
    private var suppressFocusForAutoScroll: Boolean = false

    private val heroAutoScrollRunnable = object : Runnable {
        override fun run() {
            val scrollView = heroScrollView
            if (autoScrollPausedByUser || heroCards.isEmpty() || scrollView == null) {
                heroHandler.postDelayed(this, heroAutoScrollIntervalMs)
                return
            }

            val nextIndex = (heroCurrentIndex + 1) % heroCards.size
            suppressFocusForAutoScroll = true
            try {
                centerHeroAt(nextIndex, animate = true)
            } finally {
                suppressFocusForAutoScroll = false
            }
            heroHandler.postDelayed(this, heroAutoScrollIntervalMs)
        }
    }

    // Focus gate: disable rails until user presses DPAD_DOWN
    private var railsFocusEnabled: Boolean = false

    private lateinit var viewModel: HomeViewModel
    private lateinit var repository: ContentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize repository - using fake data by default here
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
        // Root scroll view for vertical scrolling
        rootScrollView = ScrollView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        rootContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(dpToPx(88), dpToPx(36), dpToPx(88), dpToPx(48))
            clipToPadding = false
            clipChildren = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        // Compose Top Navigation
        addComposeTopNavBar()

        setupHeroBanner()
        setupContinueWatchingSection()
        setupTvChannelsSection()

        // Initially gate rails until explicit DPAD_DOWN from nav
        setRailsFocusable(false)

        // After hero exists, route DOWN from nav directly to first hero card if possible
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

        // Request initial focus on Search icon in nav bar
        view.post {
            topNavBarComposeView?.let { composeView ->
                composeView.isFocusable = true
                composeView.isFocusableInTouchMode = true
                (composeView.getTag(R.id.tag_request_search_focus) as? Runnable)?.run()
                composeView.requestFocus()
            }
        }

        // Global DPAD_RIGHT fallback to jump to search (from initial/home content area)
        view.rootView?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                topNavBarComposeView?.let { navView ->
                    (navView.getTag(R.id.tag_request_search_focus) as? Runnable)?.run()
                    return@setOnKeyListener true
                }
            }
            false
        }

        observeViewModelStates()

        // Load initial data
        viewModel.loadContinueWatching()
        viewModel.loadTvChannels()
    }

    override fun onResume() {
        super.onResume()
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
        heroScrollAnimator?.cancel()
        heroScrollAnimator = null
    }

    /**
     * Observes ViewModel LiveData and updates UI accordingly.
     */
    private fun observeViewModelStates() {
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

    private fun showContinueWatchingLoading() {
        continueWatchingLoadingView.visibility = View.VISIBLE
        continueWatchingRail.visibility = View.GONE
    }

    private fun hideContinueWatchingLoading() {
        continueWatchingLoadingView.visibility = View.GONE
        continueWatchingRail.visibility = View.VISIBLE
    }

    private fun showTvChannelsLoading() {
        tvChannelsLoadingView.visibility = View.VISIBLE
        tvChannelsRail.visibility = View.GONE
    }

    private fun hideTvChannelsLoading() {
        tvChannelsLoadingView.visibility = View.GONE
        tvChannelsRail.visibility = View.VISIBLE
    }

    private fun showErrorWithRetry(message: String, isForContinueWatching: Boolean) {
        Toast.makeText(
            requireContext(),
            "$message\nTap to retry",
            Toast.LENGTH_LONG
        ).show()

        view?.postDelayed({
            if (isForContinueWatching) {
                viewModel.retryContinueWatching()
            } else {
                viewModel.retryTvChannels()
            }
        }, 3000)
    }

    private fun updateContinueWatchingRail(items: List<ContentItem>) {
        continueWatchingRail.removeAllViews()

        var firstCardId: Int = View.NO_ID

        items.forEachIndexed { index, item ->
            val card = ContinueWatchingCard(requireContext()).apply {
                id = View.generateViewId()
                if (topNavBarId != View.NO_ID) {
                    nextFocusUpId = topNavBarId
                }
                isFocusable = railsFocusEnabled
                isFocusableInTouchMode = railsFocusEnabled
                descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
                (layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                    if (index == 0) lp.marginStart = lp.marginStart
                }
            }
            if (index == 0) {
                firstCardId = card.id
            }
            card.bind(item)
            continueWatchingRail.addView(card)
        }

        continueWatchingFirstCardId = firstCardId

        if (firstCardId != View.NO_ID) {
            heroBannerView?.nextFocusDownId = firstCardId
            heroCards.forEach { it.nextFocusDownId = firstCardId }

            // Provide fallback DOWN handling from hero to this rail
            heroScrollView?.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                    val target = view?.findViewById<View>(continueWatchingFirstCardId)
                    if (target != null) {
                        setRailsFocusable(true)
                        target.requestFocus()
                        true
                    } else {
                        false
                    }
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN) {
                    // Move focus to nav bar on UP; prefer last-focused item
                    val navView = topNavBarComposeView
                    if (navView != null) {
                        (navView.getTag(R.id.tag_request_last_nav_focus) as? Runnable)?.run()
                        return@setOnKeyListener true
                    }
                    false
                } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                    // Optional: allow RIGHT to jump to search
                    val navView = topNavBarComposeView
                    if (navView != null) {
                        (navView.getTag(R.id.tag_request_search_focus) as? Runnable)?.run()
                        return@setOnKeyListener true
                    }
                    false
                } else {
                    false
                }
            }

            heroCards.forEach { card ->
                card.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                        val target = view?.findViewById<View>(continueWatchingFirstCardId)
                        if (target != null) {
                            setRailsFocusable(true)
                            target.requestFocus()
                            true
                        } else {
                            false
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN) {
                        // Move focus to nav bar (last-focused item preferred)
                        val navView = topNavBarComposeView
                        if (navView != null) {
                            (navView.getTag(R.id.tag_request_last_nav_focus) as? Runnable)?.run()
                            return@setOnKeyListener true
                        }
                        false
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                        // RIGHT can also jump directly to search
                        topNavBarComposeView?.let { navView ->
                            (navView.getTag(R.id.tag_request_search_focus) as? Runnable)?.run()
                            return@setOnKeyListener true
                        }
                        false
                    } else {
                        false
                    }
                }
            }
        } else {
            continueWatchingRail.post {
                val targetId = continueWatchingFirstCardId.takeIf { it != View.NO_ID }
                if (targetId != null) {
                    heroBannerView?.nextFocusDownId = targetId
                    heroCards.forEach { it.nextFocusDownId = targetId }
                }
            }
        }
    }

    private fun updateTvChannelsRail(channels: List<TvChannel>) {
        tvChannelsRail.removeAllViews()

        var firstCardLocalId: Int = View.NO_ID

        channels.forEachIndexed { index, channel ->
            val card = TvChannelCard(requireContext()).apply {
                id = View.generateViewId()
                val upId = if (continueWatchingFirstCardId != View.NO_ID) continueWatchingFirstCardId else continueWatchingRail.id
                nextFocusUpId = upId
                isFocusable = railsFocusEnabled
                isFocusableInTouchMode = railsFocusEnabled
                descendantFocusability = ViewGroup.FOCUS_BEFORE_DESCENDANTS
            }
            if (index == 0) {
                firstCardLocalId = card.id
            }
            card.bind(channel)
            tvChannelsRail.addView(card)
        }

        tvChannelsFirstCardId = firstCardLocalId
    }

    /**
     * Adds the Compose TopNavBar to the rootContainer with DPAD routing to hero.
     */
    private fun addComposeTopNavBar() {
        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
                gravity = Gravity.CENTER_HORIZONTAL
            }

            id = View.generateViewId()
            isFocusable = true
            isFocusableInTouchMode = true

            // Provide a runnable to move focus to the hero container from within Compose
            setTag(
                R.id.tag_request_focus_hero,
                Runnable {
                    setRailsFocusable(true)
                    if (heroCards.isNotEmpty()) {
                        heroCards.first().requestFocus()
                    } else {
                        heroBannerView?.requestFocus()
                    }
                }
            )

            // Intercept DPAD_DOWN at the nav host to move into hero if needed (fallback)
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                    setRailsFocusable(true)
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
                // Minimal Material3 environment
                androidx.compose.material3.MaterialTheme {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        TopNavBar(
                            onItemClick = { /* No-op click handlers for now */ },
                            requestInitialFocus = true
                        )
                    }
                }
            }
        }

        topNavBarComposeView = composeView
        topNavBarId = composeView.id
        rootContainer.addView(composeView)
    }

    /**
     * Sets up the hero banner section as a horizontally scrollable carousel.
     */
    private fun setupHeroBanner() {
        heroCardWidthPx = dpToPx(heroCardWidthDp)
        heroCardHeightPx = dpToPx(heroCardHeightDp)
        heroCardSpacingPx = dpToPx(heroCardSpacingDp)
        heroPeekPx = dpToPx(heroPeekDp)
        heroSidePaddingPx = 0

        val container = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                heroCardHeightPx
            ).apply {
                marginStart = -dpToPx(88)
                marginEnd = -dpToPx(88)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            clipToPadding = false
            clipChildren = false
            setBackgroundColor(Color.TRANSPARENT)
            isFocusable = false
            isFocusableInTouchMode = false
            id = View.generateViewId()
        }
        heroBannerId = container.id
        heroBannerView = container

        val hsv = object : HorizontalScrollView(requireContext()) {
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
            setPadding(0, 0, 0, 0)
            clipToPadding = false
            clipChildren = false
            isHorizontalScrollBarEnabled = false
            isSmoothScrollingEnabled = true

            if (topNavBarId != View.NO_ID) {
                nextFocusUpId = topNavBarId
            }
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
                ) {
                    pauseAutoScrollForUserInteraction()
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.action == KeyEvent.ACTION_DOWN) {
                    val targetId = continueWatchingFirstCardId
                    val target = if (targetId != View.NO_ID) view?.findViewById<View>(targetId) else null
                    if (target != null) {
                        setRailsFocusable(true)
                        target.requestFocus()
                        return@setOnKeyListener true
                    }
                }
                // NEW: Route DPAD_UP to nav bar (last-focused preferred; fallback to search)
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN) {
                    val navView = topNavBarComposeView
                    if (navView != null) {
                        (navView.getTag(R.id.tag_request_last_nav_focus) as? Runnable)?.run()
                        return@setOnKeyListener true
                    }
                }
                // Route DPAD_RIGHT from hero area to search in TopNavBar
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                    val navView = topNavBarComposeView
                    if (navView != null) {
                        (navView.getTag(R.id.tag_request_search_focus) as? Runnable)?.run()
                        return@setOnKeyListener true
                    }
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

        heroTitles.forEachIndexed { index, title ->
            val card = HeroCard(requireContext()).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    heroCardWidthPx,
                    heroCardHeightPx
                ).apply {
                    marginEnd = heroCardSpacingPx
                }
                if (topNavBarId != View.NO_ID) {
                    nextFocusUpId = topNavBarId
                }
                isFocusable = true
                isFocusableInTouchMode = true
                setTitle(title)

                setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) {
                        if (suppressFocusForAutoScroll) return@setOnFocusChangeListener
                        val scrollView = heroScrollView
                        val focusInsideHero = scrollView?.hasFocus() == true || heroCards.any { it.hasFocus() }
                        if (!focusInsideHero) return@setOnFocusChangeListener

                        val position = heroCards.indexOf(v as HeroCard).let { if (it >= 0) it else index }
                        pauseAutoScrollForUserInteraction()
                        heroScrollView?.post {
                            centerHeroAt(position, animate = true)
                        }
                    }
                }

                addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                    if (suppressFocusForAutoScroll) return@addOnLayoutChangeListener
                    val scrollView = heroScrollView
                    val focusInsideHero = scrollView?.hasFocus() == true || heroCards.any { it.hasFocus() }
                    if (!focusInsideHero) return@addOnLayoutChangeListener

                    val position = heroCards.indexOf(v as HeroCard)
                    if (position == heroCurrentIndex) {
                        centerHeroAt(position, animate = false)
                    }
                }

                setOnKeyListener { _, keyCode, keyEvent ->
                    if (keyEvent.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
                                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        pauseAutoScrollForUserInteraction()
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            val currentIdx = heroCards.indexOf(this@apply)
                            if (currentIdx >= 0) {
                                val delta = if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
                                val nextIndex = (currentIdx + delta + heroCards.size) % heroCards.size
                                heroScrollView?.post {
                                    centerHeroAt(nextIndex, animate = true)
                                    if (!suppressFocusForAutoScroll) {
                                        heroCards.getOrNull(nextIndex)?.requestFocus()
                                    }
                                }
                                return@setOnKeyListener true
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            topNavBarComposeView?.let { navView ->
                                (navView.getTag(R.id.tag_request_search_focus) as? Runnable)?.run()
                                return@setOnKeyListener true
                            }
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP && keyEvent.action == KeyEvent.ACTION_DOWN) {
                        topNavBarComposeView?.let { navView ->
                            (navView.getTag(R.id.tag_request_last_nav_focus) as? Runnable)?.run()
                            return@setOnKeyListener true
                        }
                    }
                    false
                }
            }
            heroCards.add(card)
            rail.addView(card)
        }

        hsv.addView(rail)
        container.addView(hsv)

        container.clipChildren = false
        container.clipToPadding = false
        rail.clipChildren = false
        rail.clipToPadding = false

        heroCards.firstOrNull()?.let { first ->
            topNavBarComposeView?.nextFocusDownId = first.id
        }

        rootContainer.addView(container)

        container.post {
            if (continueWatchingFirstCardId != View.NO_ID) {
                heroBannerView?.nextFocusDownId = continueWatchingFirstCardId
                heroCards.forEach { it.nextFocusDownId = continueWatchingFirstCardId }
            }
        }

        container.post {
            val navView = topNavBarComposeView
            val heroView = heroBannerView
            if (navView != null && heroView != null) {
                val navBottom = navView.bottom
                val heroTop = heroView.top
                val measuredGap = (heroTop - navBottom).coerceAtLeast(0)
                navToHeroGapPx = measuredGap

                val overscanMinTop = dpToPx(36)
                val desiredTopPadding = navToHeroGapPx.coerceAtLeast(overscanMinTop)

                val currentLeft = rootContainer.paddingLeft
                val currentRight = rootContainer.paddingRight
                val currentBottom = rootContainer.paddingBottom

                rootContainer.setPadding(currentLeft, desiredTopPadding, currentRight, currentBottom)

                rootContainer.clipToPadding = false
                rootContainer.clipChildren = false
            }
        }

        heroScrollView?.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val viewportWidth = heroScrollView?.width ?: 0
            if (viewportWidth > 0) {
                val rawSide = ((viewportWidth - heroCardWidthPx) / 2) - heroPeekPx
                heroSidePaddingPx = rawSide.coerceAtLeast(0)
                heroScrollView?.setPadding(heroSidePaddingPx, 0, heroSidePaddingPx, 0)
                heroScrollView?.clipToPadding = false
                heroScrollView?.clipChildren = false
                heroScrollView?.post {
                    centerHeroAt(heroCurrentIndex, animate = false)
                }
            }
        }

        container.post {
            val viewportWidth = heroScrollView?.width ?: 0
            if (viewportWidth > 0) {
                val rawSide = ((viewportWidth - heroCardWidthPx) / 2) - heroPeekPx
                heroSidePaddingPx = rawSide.coerceAtLeast(0)
                heroScrollView?.setPadding(heroSidePaddingPx, 0, heroSidePaddingPx, 0)

                heroScrollView?.clipToPadding = false
                heroScrollView?.clipChildren = false

                heroScrollView?.post {
                    centerHeroAt(heroCurrentIndex, animate = false)
                }
            }
        }

        rootContainer.clipToPadding = false
        rootContainer.clipChildren = false
    }

    /**
     * Centers the hero carousel at the given index.
     */
    private fun centerHeroAt(index: Int, animate: Boolean) {
        if (heroScrollView == null || heroCards.isEmpty()) return
        heroCurrentIndex = ((index % heroCards.size) + heroCards.size) % heroCards.size

        val h = heroScrollView ?: return
        val viewportWidth = h.width
        if (viewportWidth <= 0) {
            h.post { centerHeroAt(index, animate) }
            return
        }

        if (heroSidePaddingPx <= 0) {
            val rawSide = ((viewportWidth - heroCardWidthPx) / 2) - heroPeekPx
            heroSidePaddingPx = rawSide.coerceAtLeast(0)
            h.setPadding(heroSidePaddingPx, 0, heroSidePaddingPx, 0)
            h.clipToPadding = false
            h.clipChildren = false
        }

        val unitWidth = heroCardWidthPx + heroCardSpacingPx
        val targetCenterX = heroSidePaddingPx + heroCurrentIndex * unitWidth + (heroCardWidthPx / 2f)
        val desiredScrollX = (targetCenterX - (viewportWidth / 2f)).toInt().coerceAtLeast(0)

        if (!animate) {
            h.scrollTo(desiredScrollX, 0)
            return
        }

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

    private fun pauseAutoScrollForUserInteraction() {
        autoScrollPausedByUser = true
        stopHeroAutoScroll()
        heroHandler.removeCallbacks(resumeAutoScrollRunnable)
        heroHandler.postDelayed(resumeAutoScrollRunnable, heroAutoScrollResumeIdleMs)
    }

    private val resumeAutoScrollRunnable = Runnable {
        autoScrollPausedByUser = false
        startHeroAutoScroll()
    }

    private fun startHeroAutoScroll() {
        heroHandler.removeCallbacks(heroAutoScrollRunnable)
        if (!autoScrollPausedByUser) {
            heroHandler.postDelayed(heroAutoScrollRunnable, heroAutoScrollIntervalMs)
        }
    }

    private fun stopHeroAutoScroll() {
        heroHandler.removeCallbacks(heroAutoScrollRunnable)
    }

    private fun setupContinueWatchingSection() {
        val sectionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            setPaddingRelative(0, 0, 0, 0)
            clipToPadding = false
            clipChildren = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        val title = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            text = "Seguí viendo"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT
        }

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

        val scrollView = HorizontalScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            isFocusable = false
            isFocusableInTouchMode = false
            clipToPadding = false
            clipChildren = false
            setPaddingRelative(dpToPx(10), 0, 0, 0)
            if (topNavBarId != View.NO_ID) {
                nextFocusUpId = topNavBarId
            }
            isSmoothScrollingEnabled = true

            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN) {
                    // Let system handle via nextFocusUpId (TopNavBar)
                    return@setOnKeyListener false
                }
                false
            }
        }

        continueWatchingRail = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            clipToPadding = false
            clipChildren = false
        }

        scrollView.addView(continueWatchingRail)
        sectionContainer.addView(title)
        sectionContainer.addView(continueWatchingLoadingView)
        sectionContainer.addView(scrollView)
        rootContainer.addView(sectionContainer)
    }

    private fun setupTvChannelsSection() {
        val sectionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(10)
            }
            setPaddingRelative(0, 0, 0, 0)
            clipToPadding = false
            clipChildren = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        val title = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            text = "Canales de TV"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT
        }

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

        val scrollView = HorizontalScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            isFocusable = false
            isFocusableInTouchMode = false
            setPaddingRelative(dpToPx(10), 0, 0, 0)
            if (topNavBarId != View.NO_ID) {
                nextFocusUpId = topNavBarId
            }
            isSmoothScrollingEnabled = true

            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN) {
                    val targetId = if (continueWatchingFirstCardId != View.NO_ID) {
                        continueWatchingFirstCardId
                    } else {
                        continueWatchingRail.id
                    }
                    val target = view?.findViewById<View>(targetId)
                    if (target != null) {
                        target.requestFocus()
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }

        tvChannelsRail = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            clipToPadding = false
            clipChildren = false
        }

        scrollView.addView(tvChannelsRail)
        sectionContainer.addView(title)
        sectionContainer.addView(tvChannelsLoadingView)
        sectionContainer.addView(scrollView)
        rootContainer.addView(sectionContainer)
    }

    /**
     * Focus gate: enable or disable focus for rails. When disabled, rails won’t receive focus until DOWN is pressed on nav.
     */
    private fun setRailsFocusable(enabled: Boolean) {
        railsFocusEnabled = enabled

        continueWatchingRail.importantForAccessibility =
            if (enabled) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        tvChannelsRail.importantForAccessibility =
            if (enabled) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

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

    private fun dpToPxF(dp: Float): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
