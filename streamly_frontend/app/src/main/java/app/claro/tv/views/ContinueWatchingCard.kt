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

        // Progress bar positioned at bottom-center of image
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
            textSize = 14f
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
        progressBar.progress = (item.progress * 100).toInt()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
