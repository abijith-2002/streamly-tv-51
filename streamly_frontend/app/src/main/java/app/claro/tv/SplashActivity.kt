package app.claro.tv

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

/**
 * PUBLIC_INTERFACE
 * SplashActivity
 * This is the launcher activity for the Android TV app. It displays a centered app name
 * "Claro Video" on a plain background for approximately 3 seconds, then navigates to MainActivity.
 *
 * Parameters: none
 * Returns: none
 */
class SplashActivity : FragmentActivity() {

    private val splashDurationMillis: Long = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use a simple layout defined for splash with centered text
        setContentView(R.layout.activity_splash)

        // Ensure the screen stays on during the splash (optional)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set splash title text explicitly to ensure correct content
        val titleText: TextView = findViewById(R.id.splash_title)
        titleText.text = "Claro Video"

        // Post delayed navigation to MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }, splashDurationMillis)
    }
}
