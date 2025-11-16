package app.claro.tv.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private var heroBannerView: View? = null
    private var heroBannerId: Int = View.NO_ID
    private var heroCardView: HeroCard? = null

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

        // After hero exists, route DOWN from nav directly to the hero card if available
        heroCardView?.let { card ->
            topNavBarComposeView?.nextFocusDownId = card.id
        } ?: run {
            if (heroBannerId != View.NO_ID) {
                topNavBarComposeView?.nextFocusDownId = heroBannerId
            }
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

        // Route DOWN from hero to the first continue watching card (if available)
        if (firstCardId != View.NO_ID) {
            heroBannerView?.nextFocusDownId = firstCardId
            heroCardView?.nextFocusDownId = firstCardId
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
                bottomMargin = dpToPx(24)
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
                    // Focus the single hero card if present; fallback to banner container
                    if (heroCardView != null) {
                        heroCardView?.requestFocus()
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
     * Sets up the hero banner section with a single hero card (no carousel/pager).
     * Position: 42dp gap below navigation
     * Dimensions: Fixed 872dp x 222dp
     * DPAD behavior:
     * - UP returns to the navigation bar
     * - DOWN moves to the first rail card when available
     */
    private fun setupHeroBanner() {
        val heroBanner = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(872),
                dpToPx(222)
            ).apply {
                topMargin = dpToPx(42)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            // Keep neutral container background to frame the single card
            setBackgroundColor(Color.parseColor("#222222"))
            // Container itself should not be a primary focus target
            isFocusable = false
            isFocusableInTouchMode = false
            id = View.generateViewId()
        }
        heroBannerId = heroBanner.id
        heroBannerView = heroBanner

        // Single hero card sized exactly to banner container: 872dp x 222dp
        val card = HeroCard(requireContext()).apply {
            id = View.generateViewId()
            // Override layout params to fill banner area exactly with no margins
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(872),
                dpToPx(222)
            )
            // Ensure UP goes to nav bar
            if (topNavBarId != View.NO_ID) {
                nextFocusUpId = topNavBarId
            }
            // Will set DOWN target once rails are populated
            isFocusable = true
            isFocusableInTouchMode = true
            setTitle("Destacado")
        }
        heroCardView = card

        heroBanner.addView(card)

        // Route DOWN from nav directly to the hero card
        topNavBarComposeView?.nextFocusDownId = card.id

        // No scaling animation on banner container to prevent clipping
        heroBanner.onFocusChangeListener = null

        rootContainer.addView(heroBanner)
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
