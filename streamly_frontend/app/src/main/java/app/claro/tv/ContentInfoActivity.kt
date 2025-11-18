package app.claro.tv

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

/**
 * PUBLIC_INTERFACE
 * ContentInfoActivity
 * Full-screen content information scene that mimics Figma @figma35 (assets/35-4077-14472.html/css/js, common.css)
 * for 1920x1080 TV. Renders a pixel-accurate layout for title/synopsis/etc. using native views, not WebView.
 *
 * Parameters accepted via Intent extras:
 * - EXTRA_TITLE: String - content title
 * - EXTRA_SYNOPSIS: String - content synopsis/description
 * - EXTRA_ID: String - content id
 * - EXTRA_THUMBNAIL_URL: String - optional artwork/thumbnail url (not yet used for image rendering)
 *
 * Behavior:
 * - DPAD_BACK/BACK finishes activity (return to Home)
 * - Focus trap: initial focus lands on the primary action pill (Play), with safe focus navigation.
 * - Layout preserves overscan margins consistent with home screen (88dp left/right, 36dp top, 48dp bottom).
 */
class ContentInfoActivity : FragmentActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SYNOPSIS = "extra_synopsis"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_THUMBNAIL_URL = "extra_thumbnail_url"
    }

    private lateinit var rootScroll: ScrollView
    private lateinit var stage: LinearLayout
    private var primaryActionView: View? = null
    private var backActionView: View? = null
    private var topNavTrapView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract metadata from intent
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Contenido"
        val synopsis = intent.getStringExtra(EXTRA_SYNOPSIS) ?: "Sinopsis no disponible."
        // val id = intent.getStringExtra(EXTRA_ID) ?: ""
        // val thumb = intent.getStringExtra(EXTRA_THUMBNAIL_URL)

        // Root scroll to allow safe overflow; background per common.css (#121212)
        rootScroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor("#121212"))
        }

        // Stage container with overscan-safe padding similar to home
        stage = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(88), dp(36), dp(88), dp(48))
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        // Topbar trap area to keep visual consistency (thin spacer); also a place to route DPAD_UP safely
        topNavTrapView = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)
            ).apply {
                bottomMargin = dp(14)
            }
            isFocusable = true
            isFocusableInTouchMode = true
            // Keep UP inside, DOWN moves to first actionable
            setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        primaryActionView?.requestFocus()
                        true
                    }
                    else -> false
                }
            }
        }
        stage.addView(topNavTrapView)

        // Main content block emulating 35-* layout sections
        val contentBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Title - pixel-perfect typography approximated (24sp bold white)
        val titleView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
            text = title
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 0)
        }
        contentBlock.addView(titleView)

        // Synopsis container - positioned under title, secondary text color
        val synopsisView = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(24)
            }
            text = synopsis
            textSize = 18f
            setTextColor(Color.parseColor("#b0b0b0"))
        }
        contentBlock.addView(synopsisView)

        // Horizontal actions row (Play primary, Back secondary), with focus ring/pill sizes per design ethos
        val actionsRow = HorizontalScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(24)
            }
            isHorizontalScrollBarEnabled = false
        }

        val actionsRail = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val primary = pillButtonView(
            text = "Reproducir",
            onClick = {
                // For now: just finish; future: navigate to Player
                finish()
            },
            focusedBg = "#DE1717",
            unfocusedBg = "#28292F",
            textColor = "#FFFFFF"
        )
        primaryActionView = primary

        val back = pillButtonView(
            text = "Volver",
            onClick = { finish() },
            focusedBg = "#DE1717",
            unfocusedBg = "#28292F",
            textColor = "#FFFFFF"
        )
        backActionView = back

        // Neighbor focus for row
        primary.nextFocusRightId = back.id
        back.nextFocusLeftId = primary.id

        actionsRail.addView(primary)
        actionsRail.addView(spacer(dp(12)))
        actionsRail.addView(back)
        actionsRow.addView(actionsRail)
        contentBlock.addView(actionsRow)

        // Add content block to stage
        stage.addView(contentBlock)

        // Focus trap: initial focus to primary action
        stage.post {
            primary.isFocusable = true
            primary.isFocusableInTouchMode = true
            primary.requestFocus()
        }

        // Assemble root
        rootScroll.addView(stage)
        setContentView(rootScroll)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun pillButtonView(
        text: String,
        onClick: () -> Unit,
        focusedBg: String,
        unfocusedBg: String,
        textColor: String
    ): View {
        val container = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(32)
            )
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundColor(Color.parseColor(unfocusedBg))
            isFocusable = true
            isFocusableInTouchMode = true
            id = View.generateViewId()
            clipToPadding = false
            clipChildren = false
            minimumWidth = dp(120)
        }
        val label = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT
            )
            gravity = android.view.Gravity.CENTER
            textSize = 16f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.parseColor(textColor))
            text = text
        }
        container.addView(label)

        container.setOnFocusChangeListener { v, hasFocus ->
            v.setBackgroundColor(Color.parseColor(if (hasFocus) focusedBg else unfocusedBg))
        }
        container.setOnClickListener { onClick() }
        container.setOnKeyListener { _, keyCode, event ->
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (event.action == KeyEvent.ACTION_UP) {
                        onClick()
                    }
                    true
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        // Move focus to the top trap view to keep navigation predictable
                        topNavTrapView?.requestFocus()
                    }
                    true
                }
                else -> false
            }
        }

        return container
    }

    private fun spacer(w: Int): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(w, 1)
            isFocusable = false
        }
    }

    private fun dp(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
