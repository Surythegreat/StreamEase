package com.example.streamease


import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide


class VideoPlayscreen : AppCompatActivity(){
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var qualityBut: Button
    private lateinit var dialog: AlertDialog
    private lateinit var mediaItemArrayList: ArrayList<MediaItem>
    private var videoQuality:java.util.ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_playscreen)
        enableEdgeToEdge()

        val i: String? = intent.getStringExtra("url")
        val textView: TextView = findViewById(R.id.titleofplayer)
        val tit = i.toString().substring(29).replace("-"," ")
        textView.text = buildString {
            append(tit.uppercase()[0])
            append(tit.substring(1))
        }
        playerView = findViewById(R.id.video_view)
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        qualityBut = playerView.findViewById(R.id.quality_button)
        qualityBut.setOnClickListener { onQualityButtonPressed() }

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
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setView(p)
        dialog = builder.create()

        mediaItemArrayList = arrayListOf()
        val track:LinearLayout =p.findViewById(R.id.quality_track)
        // Build the MediaItem
        val videoUrl = intent.getStringArrayListExtra("Videolinks")
        videoQuality= intent.getStringArrayListExtra("videoquality")
        if (videoUrl != null) {
            for (index in videoUrl.indices){
                val mybut = layoutInflater.inflate(R.layout.qualitybutton_layout,track,false) as Button
                track.addView(mybut)
                val uri = Uri.parse(videoUrl[index])
                val mediaItem: MediaItem = MediaItem.fromUri(uri)
                mediaItemArrayList.add(mediaItem)
                mybut.text = videoQuality?.get(index) ?: " "
                mybut.setOnClickListener{onQualityChangedButtons(index)}
            }
        }


        qualityBut.text = buildString {
            append("Quality:")
            append(videoQuality?.get(0) ?: " ")
        }
        // Prepare the player with the media item
        player!!.setMediaItem(mediaItemArrayList[0])
        player!!.prepare()
        player?.playWhenReady = true // Start playing when ready


        var fullscreen = false
        val fullscreenButton = playerView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
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
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT
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

        val scroll = findViewById<LinearLayout>(R.id.photos)
        val pictures = intent.getStringArrayListExtra("pictures")
        if (pictures != null) {
            for (picture in pictures){
                val image = ImageView(this)

                Glide.with(image).load(picture).into(image)
                scroll.addView(image)
                val la = image.layoutParams as LinearLayout.LayoutParams
                la.setMargins(10,3,10,3)
                la.height =500
                if(image.height!=0) {
                    la.width = image.width / image.height * la.height
                }

                image.layoutParams=la
            }
        }
    }
    private fun onQualityChangedButtons(s: Int) {
        val pos= player?.currentPosition
        player?.setMediaItem(mediaItemArrayList[s])

        player!!.prepare()
        if (pos != null) {
            player!!.seekTo(pos)
        }
        player?.playWhenReady = true
        dialog.hide()
        qualityBut.text = buildString {
            append("Quality:")
            append(videoQuality?.get(s) ?: " ")
        }
    }
    private fun onQualityButtonPressed() {

        player?.pause()
        dialog.show()

    }

    override fun onDestroy() {
        super.onDestroy()

        finish()
        player?.pause()
    }

}