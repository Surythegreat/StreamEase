package com.example.streamease


import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.View.inflate
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationRequestCompat.Quality
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView


class VideoPlayscreen : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var QualityBut: Button
    private lateinit var  dialog :AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_playscreen)
        enableEdgeToEdge()


        var i: String? = intent.getStringExtra("url")
        val textView: TextView = findViewById(R.id.titleofplayer)
        textView.text = i.toString().substring(29);
        playerView = findViewById(R.id.video_view)
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        QualityBut = playerView.findViewById(R.id.quality_button)
        QualityBut.setOnClickListener { onQualityButtonPressed() }

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

//        playbutton = playerView.findViewById(R.id.exo_play)
//        pausebutton = playerView.findViewById(R.id.exo_pause)
//
//        playbutton.setOnClickListener(object : OnClickListener {
//            override fun onClick(p0: View?) {
//                play()
//            }
//
//        })
//        pausebutton.setOnClickListener(object : OnClickListener {
//            override fun onClick(p0: View?) {
//                pause()
//            }
//
//        })
        val p = View.inflate(this, R.layout.qualitytrack, null)
        val builder:AlertDialog.Builder =AlertDialog.Builder(this)
        builder.setView(p)
        dialog  = builder.create()

        // Build the MediaItem
        val videoUrl = intent.getStringArrayListExtra("Videolinks")
        val uri = Uri.parse(videoUrl?.get(0) ?: " ")
        val mediaItem: MediaItem = MediaItem.fromUri(uri)


        // Prepare the player with the media item
        player!!.setMediaItem(mediaItem)
        player!!.prepare()
        player?.playWhenReady = true; // Start playing when ready


        var fullscreen = false
        var fullscreenButton = playerView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
        fullscreenButton.setOnClickListener {
            if (fullscreen) {
                fullscreenButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_fullscreen_open
                    )
                )
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                if (supportActionBar != null) {
                    supportActionBar!!.show()
                }
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                val params = playerView.layoutParams
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = (200 * applicationContext.resources.displayMetrics.density).toInt()
                playerView.layoutParams = params
                fullscreen = false
            } else {
                fullscreenButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.ic_fullscreen_close
                    )
                )
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                if (supportActionBar != null) {
                    supportActionBar!!.hide()
                }
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                val params = playerView.layoutParams
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                playerView.layoutParams = params
                fullscreen = true
            }
        }
    }

    private fun onQualityButtonPressed() {

        player?.pause()
        dialog.show()

    }
}