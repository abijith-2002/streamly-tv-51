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
import app.claro.tv.models.TvChannel

/**
 * PUBLIC_INTERFACE
 * Custom view representing a TV Channel card with program info, live badge, and play button.
 * Optimized for Android TV with D-pad navigation and overscan-safe margins.
 *
 * Layout metrics per spec:
 * - Card width: 206dp (fixed)
 * - Image section height: 116dp
 * - Title/info area height: 40dp
 * - Corner radius: 0dp
 *
 * @param context Android context
 * @param attrs XML attributes
 */
class TvChannelCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val thumbnailView: FrameLayout
    private val progressBar: ProgressBar
    private val programTitleText: TextView
    private val channelInfoText: TextView
    private val timeInfoText: TextView
    private val liveBadge: TextView
    private val rentBadge: TextView
    private var tvChannel: TvChannel? = null

    init {
        // Card setup: width 206dp, height 156dp (116 + 40)
        layoutParams = LinearLayout.LayoutParams(
            dpToPx(206),
            dpToPx(156)
        ).apply {
            marginEnd = dpToPx(10)
        }
        radius = 0f
        cardElevation = dpToPx(4).toFloat()
        setCardBackgroundColor(Color.parseColor("#1a1a1a"))
        isFocusable = true
        isFocusableInTouchMode = true
        clipToPadding = false
        clipChildren = false
        useCompatPadding = false
        preventCornerOverlap = false

        // Vertical stack: image(116dp) + info(40dp)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            clipToPadding = false
            clipChildren = false
        }

        // Image section (top)
        thumbnailView = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(116)
            )
            setBackgroundColor(Color.parseColor("#2d2d2d"))
            clipToPadding = false
            clipChildren = false
        }

        // Progress bar at bottom-left
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(120),
                dpToPx(4)
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                marginStart = dpToPx(8)
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

        // Rent badge in top-left of image
        rentBadge = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                marginStart = dpToPx(6)
                topMargin = dpToPx(6)
            }
            text = "ALQUILÁ"
            textSize = 10f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#ffc107"))
            setPadding(dpToPx(6), dpToPx(3), dpToPx(6), dpToPx(3))
            visibility = GONE
        }
        thumbnailView.addView(rentBadge)

        // Info area height 40dp
        val infoContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(40)
            )
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
        }

        // First row: program title (bold-ish)
        programTitleText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            textSize = 14f
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        // Second row: channel and time compact with optional LIVE badge prefix
        val secondaryRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        liveBadge = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dpToPx(6)
            }
            text = "EN VIVO"
            textSize = 10f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#ff0000"))
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            visibility = GONE
        }

        channelInfoText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
            textSize = 12f
            setTextColor(Color.parseColor("#b0b0b0"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        timeInfoText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textSize = 12f
            setTextColor(Color.parseColor("#b0b0b0"))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }

        secondaryRow.addView(liveBadge)
        secondaryRow.addView(channelInfoText)
        secondaryRow.addView(timeInfoText)

        infoContainer.addView(programTitleText)
        infoContainer.addView(secondaryRow)

        container.addView(thumbnailView)
        container.addView(infoContainer)
        addView(container)

        // Focus scale animation
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
     * Binds TV channel data to the card view.
     *
     * @param channel TvChannel to display
     */
    fun bind(channel: TvChannel) {
        tvChannel = channel
        programTitleText.text = channel.programTitle
        channelInfoText.text = "${channel.channelNumber} | ${channel.channelName}"
        timeInfoText.text = " ${channel.startTime}-${channel.endTime}"
        progressBar.progress = (channel.progress * 100).toInt()
        liveBadge.visibility = if (channel.isLive) VISIBLE else GONE
        rentBadge.visibility = if (channel.isRentable) VISIBLE else GONE
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
