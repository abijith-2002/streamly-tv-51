package app.claro.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import android.view.KeyEvent
import app.claro.tv.fragments.HomeFragment

/**
 * PUBLIC_INTERFACE
 * MainActivity
 * The main/home screen activity for the Android TV app.
 * Hosts the HomeFragment which displays the native Android TV home screen
 * with hero banner, Continue Watching rail, and TV Channels rail.
 * Extends FragmentActivity for Leanback compatibility.
 *
 * Parameters: none
 * Returns: none
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set content view to a simple container for the fragment
        val container = android.widget.FrameLayout(this).apply {
            id = android.view.View.generateViewId()
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(container)

        // Load the HomeFragment if this is the first creation
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(container.id, HomeFragment())
                .commit()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle TV remote control inputs
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                // Handle SELECT/OK button
                // The focused view will handle the click
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                // Handle BACK button
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
