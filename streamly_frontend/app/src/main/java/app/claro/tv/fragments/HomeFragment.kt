package app.claro.tv.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import app.claro.tv.models.ContentItem
import app.claro.tv.models.TvChannel
import app.claro.tv.views.ContinueWatchingCard
import app.claro.tv.views.TvChannelCard

/**
 * PUBLIC_INTERFACE
 * HomeFragment - The main home screen for the Android TV app.
 * Displays hero banner, Continue Watching rail, and TV Channels rail.
 * Implements D-pad navigation with proper focus management and overscan-safe layout.
 * 
 * Design based on AAF_inicio Copy 2 (screen 2001:3396) with pixel-accurate dimensions
 * for 1920x1080 resolution.
 */
class HomeFragment : Fragment() {

    private lateinit var rootScrollView: ScrollView
    private lateinit var rootContainer: LinearLayout
    private lateinit var continueWatchingRail: LinearLayout
    private lateinit var tvChannelsRail: LinearLayout
    private var firstFocusableView: View? = null

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
        }

        setupTopNavigation()
        setupHeroBanner()
        setupContinueWatchingSection()
        setupTvChannelsSection()

        rootScrollView.addView(rootContainer)
        return rootScrollView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set initial focus to the first navigation item
        view.post {
            firstFocusableView?.requestFocus()
        }
    }

    /**
     * Sets up the top navigation bar with app logo and menu items.
     * Position: 88dp from left, 36dp from top (handled by container padding)
     * Dimensions: Full width x 74dp height
     */
    private fun setupTopNavigation() {
        val navContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(74)
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        // Logo placeholder (169.637dp x 34.356dp from design)
        val logo = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(170),
                dpToPx(34)
            )
            text = "Claro Video"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#2196F3"))
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
        }

        // Spacer to position navigation menu
        val spacer = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(120),
                0
            )
        }

        // Navigation menu
        val navMenu = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.START
        }

        val navItems = listOf("Inicio", "Películas", "Series", "TV en vivo", "Kids", "Mis Contenidos")
        navItems.forEachIndexed { index, item ->
            val navButton = TextView(requireContext()).apply {
                text = item
                textSize = 20f
                setTextColor(if (index == 0) Color.WHITE else Color.parseColor("#b0b0b0"))
                setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                isFocusable = true
                isFocusableInTouchMode = true
                setBackgroundColor(if (index == 0) Color.parseColor("#26ffffff") else Color.TRANSPARENT)
                
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(24)
                }

                // Focus effect with scale animation
                onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                    animate()
                        .scaleX(if (hasFocus) 1.05f else 1.0f)
                        .scaleY(if (hasFocus) 1.05f else 1.0f)
                        .setDuration(150)
                        .start()
                    
                    if (hasFocus) {
                        setTextColor(Color.WHITE)
                        setBackgroundColor(Color.parseColor("#26ffffff"))
                    } else if (index != 0) {
                        setTextColor(Color.parseColor("#b0b0b0"))
                        setBackgroundColor(Color.TRANSPARENT)
                    }
                }

                // Store first focusable view
                if (index == 0 && firstFocusableView == null) {
                    firstFocusableView = this
                }
            }
            navMenu.addView(navButton)
        }

        // Avatar placeholder (56dp x 56dp)
        val avatarContainer = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(74),
                dpToPx(74)
            )
            isFocusable = true
            isFocusableInTouchMode = true
        }

        val avatar = View(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(56),
                dpToPx(56)
            ).apply {
                gravity = Gravity.CENTER
            }
            setBackgroundColor(Color.parseColor("#667eea"))
        }
        avatarContainer.addView(avatar)

        // Avatar focus ring effect
        avatarContainer.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            avatar.animate()
                .scaleX(if (hasFocus) 1.1f else 1.0f)
                .scaleY(if (hasFocus) 1.1f else 1.0f)
                .setDuration(150)
                .start()
        }

        navContainer.addView(logo)
        navContainer.addView(spacer)
        navContainer.addView(navMenu)
        navContainer.addView(avatarContainer)
        rootContainer.addView(navContainer)
    }

    /**
     * Sets up the hero banner section (main highlight carousel).
     * Position: 42dp gap below navigation
     * Dimensions: Full width x 444dp height
     */
    private fun setupHeroBanner() {
        val heroBanner = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(444)
            ).apply {
                topMargin = dpToPx(42)
            }
            setBackgroundColor(Color.parseColor("#2d2d2d"))
            isFocusable = true
            isFocusableInTouchMode = true
        }

        // Hero content placeholder
        val heroText = TextView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            text = "Featured Content"
            textSize = 48f
            setTextColor(Color.parseColor("#808080"))
        }

        // Focus effect for hero banner
        heroBanner.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            heroBanner.animate()
                .scaleX(if (hasFocus) 1.02f else 1.0f)
                .scaleY(if (hasFocus) 1.02f else 1.0f)
                .setDuration(200)
                .start()
        }

        heroBanner.addView(heroText)
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

        // Horizontal scroll view for cards
        val scrollView = HorizontalScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
        }

        continueWatchingRail = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Placeholder content data (replace with API data in production)
        val contentItems = listOf(
            ContentItem("1", "Rogue One", "", 0.404f),
            ContentItem("2", "Ex Machina", "", 0.404f),
            ContentItem("3", "Sing Street", "", 0.404f),
            ContentItem("4", "2012", "", 0.404f),
            ContentItem("5", "Ad Astra", "", 0.404f)
        )

        contentItems.forEach { item ->
            val card = ContinueWatchingCard(requireContext())
            card.bind(item)
            continueWatchingRail.addView(card)
        }

        scrollView.addView(continueWatchingRail)
        sectionContainer.addView(title)
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

        // Horizontal scroll view for TV cards
        val scrollView = HorizontalScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
        }

        tvChannelsRail = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Placeholder TV channel data (replace with API data in production)
        val tvChannels = listOf(
            TvChannel(
                "1", "Marca Claro Radio", "004", "Claro sports",
                "11:30", "12:30", true, false, "", 0.386f
            ),
            TvChannel(
                "2", "E.T.", "005", "HBO Channel",
                "11:30", "12:30", true, true, "", 0.386f
            ),
            TvChannel(
                "3", "Marca Claro Radio", "004", "Claro sports",
                "11:30", "12:30", true, false, "", 0.386f
            )
        )

        tvChannels.forEach { channel ->
            val card = TvChannelCard(requireContext())
            card.bind(channel)
            tvChannelsRail.addView(card)
        }

        scrollView.addView(tvChannelsRail)
        sectionContainer.addView(title)
        sectionContainer.addView(scrollView)
        rootContainer.addView(sectionContainer)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
