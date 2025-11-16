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
 * @param context Android context
 * @param attrs XML attributes
 */
class ContinueWatchingCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val thumbnailView: FrameLayout
    private val progressBar: ProgressBar
    private val titleText: TextView
    private var contentItem: ContentItem? = null

    init {
        // Card setup - new compact dimensions 206dp x 116dp, keep spacing between cards
        layoutParams = LinearLayout.LayoutParams(
            dpToPx(206),
            dpToPx(116)
        ).apply {
            marginEnd = dpToPx(10)
        }
        radius = 0f
        cardElevation = dpToPx(4).toFloat()
        setCardBackgroundColor(Color.parseColor("#1a1a1a"))
        isFocusable = true
        isFocusableInTouchMode = true
        // Avoid clipping during focus scale
        clipToPadding = false
        clipChildren = false

        // Root container
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipToPadding = false
            clipChildren = false
        }

        // Thumbnail container with progress bar overlay
        // Height scaled to fit compact card: leave small area for title
        thumbnailView = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(80)
            )
            setBackgroundColor(Color.parseColor("#2d2d2d"))
            clipToPadding = false
            clipChildren = false
        }

        // Progress bar at bottom of thumbnail, width adapted to new card width
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(180),
                dpToPx(4)
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(6)
            }
            max = 100
            progressDrawable = context.getDrawable(android.R.drawable.progress_horizontal)
            progressTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#33ffffff")
            )
        }
        thumbnailView.addView(progressBar)

        // Title area compact
        val titleArea = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(36)
            )
            setBackgroundColor(Color.parseColor("#66000000"))
        }

        titleText = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
                leftMargin = dpToPx(8)
                rightMargin = dpToPx(8)
                topMargin = dpToPx(6)
            }
            textSize = 14f
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        titleArea.addView(titleText)

        container.addView(thumbnailView)
        container.addView(titleArea)
        addView(container)

        // Focus change listener for scale effect; ensure no clipping
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
        progressBar.progress = (item.progress * 100).toInt()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
