package app.claro.tv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * PlayerActivity launches video playback using Media3 ExoPlayer for Android TV.
 * Pass the video URL in the intent with EXTRA_VIDEO_URL.
 */
class PlayerActivity : AppCompatActivity() {
    private var exoPlayer: ExoPlayer? = null
    private lateinit var playerView: PlayerView

    companion object {
        const val EXTRA_VIDEO_URL = "extra_video_url"

        // PUBLIC_INTERFACE
        /**
         * Launches the PlayerActivity with the provided video URL.
         */
        fun start(context: Context, url: String) {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra(EXTRA_VIDEO_URL, url)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        playerView = findViewById(R.id.player_view)
    }

    override fun onStart() {
        super.onStart()
        initializePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }

    private fun initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()
            playerView.player = exoPlayer
            playerView.requestFocus()
        }
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        if (videoUrl != null) {
            val mediaItem = MediaItem.fromUri(videoUrl)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.playWhenReady = true // Auto-play
            exoPlayer?.prepare()
        }
    }

    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    // Ensure DPAD_BACK exits player and DPAD/TV keys work as expected
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        return super.dispatchKeyEvent(event)
    }
}
