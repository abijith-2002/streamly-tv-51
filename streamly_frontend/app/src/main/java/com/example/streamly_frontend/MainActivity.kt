package app.claro.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import android.view.KeyEvent
import android.widget.TextView

/**
 * PUBLIC_INTERFACE
 * MainActivity
 * The main/home screen activity for the Android TV app.
 * Extends FragmentActivity for Leanback compatibility.
 *
 * Parameters: none
 * Returns: none
 */
class MainActivity : FragmentActivity() {

    private lateinit var titleText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleText = findViewById(R.id.title_text)
        titleText.text = "Claro Video"

        // TODO: Initialize your rating screen components here
        // setupRatingOverlay()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle TV remote control inputs
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                // Handle SELECT/OK button
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
