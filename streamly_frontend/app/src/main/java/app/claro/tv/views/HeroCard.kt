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
 * - Default width: 200dp
 * - Height: 222dp (matches hero banner height)
 * - Right margin: 12dp (sensible spacing between cards)
 *
 * Focus:
 * - Scales to 1.05x on focus
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
        // Card visual and layout config
        layoutParams = LinearLayout.LayoutParams(
            dpToPx(200),
            dpToPx(222)
        ).apply {
            marginEnd = dpToPx(12)
        }
        radius = dpToPx(8).toFloat()
        cardElevation = dpToPx(4).toFloat()
        setCardBackgroundColor(Color.parseColor("#1a1a1a"))
        useCompatPadding = true

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

        // Focus scaling effect
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
