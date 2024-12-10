package com.example.streamease


import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView


class VideoPlayscreen : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var playbutton:ImageButton
    private lateinit var pausebutton:ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_playscreen)
        enableEdgeToEdge()





        var i:String? =intent.getStringExtra("url")
        val textView:TextView=findViewById(R.id.titleofplayer)
        textView.text = i.toString().substring(29);
        playerView = findViewById(R.id.video_view)
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE


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

        // Build the MediaItem
        val videoUrl = intent.getStringExtra("hdvideourl")
        val uri = Uri.parse(videoUrl)
        val mediaItem: MediaItem = MediaItem.fromUri(uri)


        // Prepare the player with the media item
        player!!.setMediaItem(mediaItem)
        player!!.prepare()
        player?.playWhenReady =true; // Start playing when ready


        var fullscreen = false
        var fullscreenButton = playerView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
        fullscreenButton.setOnClickListener(View.OnClickListener {
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
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
        })




    }

    private fun pause() {
        playbutton.visibility=View.VISIBLE;
        pausebutton.visibility=View.GONE;
        player?.playWhenReady =false;
    }

    private fun play() {
        playbutton.visibility=View.GONE;
        pausebutton.visibility=View.VISIBLE;

    }
}