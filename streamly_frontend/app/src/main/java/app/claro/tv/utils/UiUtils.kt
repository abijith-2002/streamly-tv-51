package app.claro.tv.utils

import android.content.Context

/**
 * PUBLIC_INTERFACE
 * Utility object for common UI operations in the Android TV app.
 * Provides helper methods for dimension conversions, color parsing, and focus management.
 */
object UiUtils {
    
    /**
     * PUBLIC_INTERFACE
     * Converts density-independent pixels (dp) to pixels (px).
     * 
     * @param context Android context
     * @param dp Value in dp
     * @return Value in px
     */
    fun dpToPx(context: Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * PUBLIC_INTERFACE
     * Converts pixels (px) to density-independent pixels (dp).
     * 
     * @param context Android context
     * @param px Value in px
     * @return Value in dp
     */
    fun pxToDp(context: Context, px: Int): Int {
        return (px / context.resources.displayMetrics.density).toInt()
    }

    /**
     * PUBLIC_INTERFACE
     * Calculates the usable viewport height considering overscan (8% on all sides).
     * For a 1920x1080 TV, this ensures content stays within visible area.
     * 
     * @param context Android context
     * @return Usable viewport height in px
     */
    fun getOverscanSafeHeight(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val totalHeight = displayMetrics.heightPixels
        val overscanMargin = (totalHeight * 0.08).toInt()
        return totalHeight - (overscanMargin * 2)
    }

    /**
     * PUBLIC_INTERFACE
     * Calculates the usable viewport width considering overscan (8% on all sides).
     * 
     * @param context Android context
     * @return Usable viewport width in px
     */
    fun getOverscanSafeWidth(context: Context): Int {
        val displayMetrics = context.resources.displayMetrics
        val totalWidth = displayMetrics.widthPixels
        val overscanMargin = (totalWidth * 0.08).toInt()
        return totalWidth - (overscanMargin * 2)
    }
}
