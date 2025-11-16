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
        // Card setup
        layoutParams = LinearLayout.LayoutParams(
            dpToPx(745),
            dpToPx(212)
        ).apply {
            // Slightly increased spacing between cards for better separation
            marginEnd = dpToPx(10)
        }
        radius = 0f
        cardElevation = dpToPx(4).toFloat()
        setCardBackgroundColor(Color.parseColor("#1a1a1a"))
        isFocusable = true
        isFocusableInTouchMode = true

        // Root container - horizontal layout
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Thumbnail container (left side)
        thumbnailView = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(377),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#2d2d2d"))
        }

        // Progress bar at bottom of thumbnail
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(207),
                dpToPx(6)
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                leftMargin = dpToPx(15)
                bottomMargin = dpToPx(19)
            }
            max = 100
            progressDrawable = context.getDrawable(android.R.drawable.progress_horizontal)
            progressTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
                Color.parseColor("#33ffffff")
            )
        }
        thumbnailView.addView(progressBar)

        // Play button indicator (circular overlay)
        val playButton = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(92),
                dpToPx(92)
            ).apply {
                gravity = Gravity.CENTER
            }
            setBackgroundColor(Color.parseColor("#99000000"))
        }
        thumbnailView.addView(playButton)

        // Rent badge (top-left corner)
        rentBadge = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = dpToPx(15)
                topMargin = dpToPx(17)
            }
            text = "ALQUILÁ"
            textSize = 12f
            setTextColor(Color.BLACK)
            setBackgroundColor(Color.parseColor("#ffc107"))
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
            visibility = GONE
        }
        thumbnailView.addView(rentBadge)

        // Info container (right side)
        val infoContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(337),
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }

        // Program title
        programTitleText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textSize = 28f
            setTextColor(Color.WHITE)
            maxLines = 2
            setPadding(0, 0, 0, dpToPx(8))
        }

        // Channel info
        channelInfoText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textSize = 18f
            setTextColor(Color.parseColor("#b0b0b0"))
            setPadding(0, 0, 0, dpToPx(8))
        }

        // Live badge
        liveBadge = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(8)
            }
            text = "EN VIVO"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#ff0000"))
            setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6))
        }

        // Time info
        timeInfoText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textSize = 18f
            setTextColor(Color.parseColor("#b0b0b0"))
        }

        infoContainer.addView(programTitleText)
        infoContainer.addView(channelInfoText)
        infoContainer.addView(liveBadge)
        infoContainer.addView(timeInfoText)

        container.addView(thumbnailView)
        container.addView(infoContainer)
        addView(container)

        // Focus change listener
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
        timeInfoText.text = "${channel.startTime} - ${channel.endTime}"
        progressBar.progress = (channel.progress * 100).toInt()
        liveBadge.visibility = if (channel.isLive) VISIBLE else GONE
        rentBadge.visibility = if (channel.isRentable) VISIBLE else GONE
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
