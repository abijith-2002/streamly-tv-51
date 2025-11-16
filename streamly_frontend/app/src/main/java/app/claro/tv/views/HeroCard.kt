package app.claro.tv.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView

/**
 * PUBLIC_INTERFACE
 * HeroCard - compact card to be displayed inside the hero banner row.
 * Designed to fit within the fixed hero banner container (872dp x 222dp).
 *
 * Dimensions:
 * - Width is provided by parent (872dp)
 * - Height: 222dp (matches hero banner height)
 * - Right margin is set by parent rail
 *
 * Focus:
 * - No scaling (keeps 0dp corner radius, prevents clipping)
 * - Focusable for TV DPAD navigation
 *
 * @param context Android context
 * @param attrs XML attributes
 */
class HeroCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CardView(context, attrs, defStyleAttr) {

    private val titleText: TextView

    init {
        // Card visual and layout config (parent sets exact size; maintain 0dp radius)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dpToPx(222)
        )
        radius = 0f
        cardElevation = dpToPx(4).toFloat()
        setCardBackgroundColor(Color.parseColor("#1a1a1a"))
        useCompatPadding = false

        isFocusable = true
        isFocusableInTouchMode = true

        // Root container for content
        val container = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#2d2d2d")) // placeholder background
        }

        // Simple center title as placeholder
        titleText = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER
            }
            text = "Destacado"
            textSize = 20f
            setTextColor(Color.WHITE)
        }
        container.addView(titleText)

        addView(container)

        // Keep scale fixed (no scale animation) to avoid focus clipping within hero banner
        onFocusChangeListener = null
    }

    /**
     * PUBLIC_INTERFACE
     * Sets the title text for this hero card.
     *
     * @param title Text to display in the hero card
     */
    fun setTitle(title: String) {
        titleText.text = title
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
