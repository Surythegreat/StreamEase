package com.example.streamease


import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout


@UnstableApi
class VideoPlayscreen : AppCompatActivity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var qualityButton: Button
    private lateinit var qualityDialog: AlertDialog
    private val mediaItemList = arrayListOf<MediaItem>()
    private var videoQualities: ArrayList<String>? = null
    private var videoUrls: ArrayList<String>? = null
    private lateinit var trackSelector:DefaultTrackSelector


    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_playscreen)
        enableEdgeToEdge()

        setupPlayer()
        setupVideoTitle()
        setupQualitySelector()
        setupFullscreenHandler()
        setupPreviewImages()
    }

    @OptIn(UnstableApi::class)
    private fun setupPlayer() {
        playerView = findViewById(R.id.video_view)

        trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        playerView.player = player




        // Get video URLs and qualities
        videoUrls = intent.getStringArrayListExtra("Videolinks")
        videoQualities = intent.getStringArrayListExtra("videoquality")

        // Populate media items
        videoUrls?.forEachIndexed { index, url ->
            val uri = Uri.parse(url)
            val mediaItem = MediaItem.fromUri(uri)
            mediaItemList.add(mediaItem)
        }


        // Prepare the player
        player?.setMediaItem(mediaItemList.first())
        player?.prepare()
        player?.playWhenReady = true
    }


    private fun setupVideoTitle() {
        val url: String? = intent.getStringExtra("url")
        val titleTextView: TextView = findViewById(R.id.titleofplayer)

        // Extract video title
        val title = url?.substring(29)?.replace("-", " ")?.replaceFirstChar { it.uppercase() }
        titleTextView.text = title
    }

    private fun setupQualitySelector() {
        qualityButton = playerView.findViewById(R.id.quality_button)
        "Quality: ${videoQualities?.firstOrNull() ?: "N/A"}".also { qualityButton.text = it }

        val qualityLayout = layoutInflater.inflate(R.layout.qualitytrack, null)
        val qualityTrackLayout: LinearLayout = qualityLayout.findViewById(R.id.quality_track)

        // Populate quality options
        videoQualities?.forEachIndexed { index, quality ->
            val qualityButton = layoutInflater.inflate(R.layout.qualitybutton_layout, qualityTrackLayout, false) as Button
            qualityButton.text = quality
            qualityButton.setOnClickListener { changeQuality(index) }
            qualityTrackLayout.addView(qualityButton)
        }
        val audioOnlyButton = layoutInflater.inflate(R.layout.qualitybutton_layout,qualityTrackLayout,false) as Button
        "Audio-Only".also { audioOnlyButton.text = it }
        audioOnlyButton.setOnClickListener{audioOnlyButtonPressed() }
        qualityTrackLayout.addView(audioOnlyButton)

        // Create quality dialog
        qualityDialog = AlertDialog.Builder(this)
            .setView(qualityLayout)
            .create()

        qualityButton.setOnClickListener {
            player?.pause()
            qualityDialog.show()
        }
    }

    @OptIn(UnstableApi::class)
    private fun audioOnlyButtonPressed() {

            val trackSelectionParameters = TrackSelectionParameters.Builder(this)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true) // Disable video tracks
                .build()
            trackSelector.setParameters(trackSelectionParameters)


        qualityDialog.dismiss()
        player?.play()

        "Quality: AUDIO-ONLY".also { qualityButton.text = it }

    }


    @OptIn(UnstableApi::class)
    private fun changeQuality(index: Int) {
        val trackSelectionParameters = TrackSelectionParameters.Builder(this)
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false) // Disable video tracks
        .build()
        trackSelector.setParameters(trackSelectionParameters)
        val position = player?.currentPosition ?: 0
        player?.setMediaItem(mediaItemList[index])
        player?.prepare()
        player?.seekTo(position)
        player?.playWhenReady = true
        qualityDialog.dismiss()

        "Quality: ${videoQualities?.get(index) ?: "N/A"}".also { qualityButton.text = it }
    }

    private fun setupFullscreenHandler() {
        val fullscreenButton = playerView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
        val collapsingToolbar: CollapsingToolbarLayout = findViewById(R.id.collapsing_toolbar)
        var isFullscreen = false

        fullscreenButton.setOnClickListener {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
                exitFullscreen(windowInsetsController, collapsingToolbar, fullscreenButton)
            } else {
                enterFullscreen(windowInsetsController, collapsingToolbar, fullscreenButton)
            }
            isFullscreen = !isFullscreen
        }
    }

    private fun enterFullscreen(controller: WindowInsetsControllerCompat, toolbar: CollapsingToolbarLayout, button: ImageView) {
        button.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_close))
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        supportActionBar?.hide()
        toolbar.layoutParams = (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = 0
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        playerView.layoutParams = playerView.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.widthPixels
            height = screenHeight
        }



    }

    private fun exitFullscreen(controller: WindowInsetsControllerCompat, toolbar: CollapsingToolbarLayout, button: ImageView) {
        button.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_fullscreen_open))
        controller.show(WindowInsetsCompat.Type.systemBars())
        supportActionBar?.show()
        toolbar.layoutParams = (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        playerView.layoutParams = playerView.layoutParams.apply {
            height = 300.dp
        }
    }

    private fun setupPreviewImages() {
        val photosLayout: LinearLayout = findViewById(R.id.photos)
        val pictures = intent.getStringArrayListExtra("pictures")

        pictures?.forEach { picture ->
            val imageView = ImageView(this).apply {
                Glide.with(this).load(picture).into(this)
                layoutParams = LinearLayout.LayoutParams(150.dp, 150.dp).apply {
                    setMargins(10.dp, 3.dp, 10.dp, 3.dp)
                }
            }
            photosLayout.addView(imageView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }
}
