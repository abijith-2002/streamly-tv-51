package app.claro.tv.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import app.claro.tv.models.ContentItem

/**
 * PUBLIC_INTERFACE
 * Custom view representing a Continue Watching card with thumbnail, progress bar, and title.
 * Designed for Android TV D-pad navigation with proper focus handling.
 *
 * Layout metrics per spec:
 * - Card width: 206dp (fixed)
 * - Card radius: 0dp
 * - Image section height: 116dp
 * - Title text container height: 40dp
 * - Total height accommodates focus scale without clipping (container doesn't clip)
 *
 * Progress bar (Seguí viendo) per requirement:
 * - Track container (outer): width 218dp, height 9.2dp
 * - Progress indicator (inner): height 4.6dp, vertically centered in the 9.2dp track
 * - Progress color: #DE1717
 * - RTL-aware layout and no clipping during focus scale
 *
 * @param context Android context
 * @param attrs XML attributes
 */
class ContinueWatchingCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val thumbnailView: FrameLayout
    private val progressTrack: FrameLayout
    private val progressFill: FrameLayout
    private val progressBar: ProgressBar
    private val titleText: TextView
    private var contentItem: ContentItem? = null

    init {
        // Card setup - maintain width 206dp; height is image(116) + title(40) = 156dp.
        layoutParams = LinearLayout.LayoutParams(
            dpToPx(206),
            dpToPx(156)
        ).apply {
            // Keep inter-item spacing 10dp; start spacing handled by rail container
            marginEnd = dpToPx(10)
        }
        radius = 0f
        cardElevation = dpToPx(4).toFloat()
        setCardBackgroundColor(Color.parseColor("#1a1a1a"))
        // Ensure focusable for D-pad and in touch mode for consistency
        isFocusable = true
        isFocusableInTouchMode = true
        // Avoid clipping during focus scale
        clipToPadding = false
        clipChildren = false
        useCompatPadding = false
        preventCornerOverlap = false

        // Root container for vertical stack
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipToPadding = false
            clipChildren = false
        }

        // Image section height exactly 116dp
        thumbnailView = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(116)
            )
            setBackgroundColor(Color.parseColor("#2d2d2d"))
            clipToPadding = false
            clipChildren = false
        }

        // Build custom progress track and fill to meet exact dimensions and color.
        // Track: 218dp x 9.2dp, positioned bottom and centered horizontally in the image.
        // Fill: height 4.6dp, vertically centered within the 9.2dp track. Width will be set in bind().
        val trackWidthPx = dpToPxF(218f)
        val trackHeightPx = dpToPxF(9.2f)
        val fillHeightPx = dpToPxF(4.6f)

        progressTrack = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                trackWidthPx,
                trackHeightPx
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                // Keep a small bottom margin to separate from image bottom edge; preserve prior 6dp
                bottomMargin = dpToPx(6)
            }
            // Semi-transparent background to simulate a track (white at 20% like design guide)
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
            // Avoid clipping; focus scale should not cut progress visuals
            clipToPadding = false
            clipChildren = false
        }

        // Inner progress indicator (fill) - red color, centered vertically within the track
        progressFill = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                0, // width is set during bind() based on progress
                fillHeightPx,
                Gravity.CENTER_VERTICAL or Gravity.START
            )
            // Set RTL-aware; for RTL, we will adjust via layoutDirection and gravity handling
            layoutDirection = LAYOUT_DIRECTION_LOCALE
            setBackgroundColor(Color.parseColor("#DE1717"))
            clipToPadding = false
            clipChildren = false
        }
        progressTrack.addView(progressFill)

        // Keep a tiny transparent ProgressBar only to reuse system animation/state
        // but visually hidden; we will drive our custom fill via its progress.
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(1, 1) // effectively hidden
            max = 100
            alpha = 0f
            isEnabled = false
        }

        // Add custom track and the hidden system bar into the image area
        thumbnailView.addView(progressTrack)
        thumbnailView.addView(progressBar)

        // Title container height exactly 40dp
        val titleArea = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(40)
            )
            setBackgroundColor(Color.parseColor("#66000000"))
        }

        titleText = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                marginStart = dpToPx(8)
                marginEnd = dpToPx(8)
            }
            // Maintain configured title size at 16sp per requirements
            textSize = 16f
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            isFocusable = false
            isFocusableInTouchMode = false
        }
        titleArea.addView(titleText)

        container.addView(thumbnailView)
        container.addView(titleArea)
        addView(container)

        // Scale on focus with no clipping
        onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            animate()
                .scaleX(if (hasFocus) 1.05f else 1.0f)
                .scaleY(if (hasFocus) 1.05f else 1.0f)
                .setDuration(200)
                .start()
        }
    }

    /**
     * PUBLIC_INTERFACE
     * Binds content item data to the card view.
     *
     * @param item ContentItem to display
     */
    fun bind(item: ContentItem) {
        contentItem = item
        titleText.text = item.title

        val pct = item.progress.coerceIn(0f, 1f)
        val trackWidth = progressTrack.layoutParams.width
        // If layout not yet measured, post to ensure width is available
        if (trackWidth <= 0) {
            progressTrack.post {
                applyProgressToFill(pct)
            }
        } else {
            applyProgressToFill(pct)
        }
        // Keep ProgressBar value in sync (used as internal state holder)
        progressBar.progress = (pct * 100).toInt()
    }

    private fun applyProgressToFill(progressFraction: Float) {
        val trackWidthPx = progressTrack.width.takeIf { it > 0 } ?: progressTrack.layoutParams.width
        val rtl = layoutDirection == LAYOUT_DIRECTION_RTL
        val newWidth = (trackWidthPx * progressFraction).toInt().coerceIn(0, trackWidthPx)

        val lp = progressFill.layoutParams as FrameLayout.LayoutParams
        lp.width = newWidth
        // Gravity START/END awareness for RTL: use START so it flips automatically per locale
        lp.gravity = Gravity.CENTER_VERTICAL or Gravity.START
        progressFill.layoutParams = lp

        // Ensure no clipping during scale
        progressTrack.clipToPadding = false
        progressTrack.clipChildren = false
        (progressTrack.parent as? ViewGroup)?.apply {
            clipToPadding = false
            clipChildren = false
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    // Fractional dp support, e.g., 9.2dp and 4.6dp
    private fun dpToPxF(dp: Float): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
