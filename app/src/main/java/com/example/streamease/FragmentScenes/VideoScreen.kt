package com.example.streamease.FragmentScenes

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
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
import com.example.streamease.MainActivity2
import com.example.streamease.R
import com.example.streamease.databinding.FragmentVideoScreenBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.bottomnavigation.BottomNavigationView


@UnstableApi
class VideoScreen : scenes() {

    private lateinit var binding: FragmentVideoScreenBinding
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var qualityButton: Button
    private lateinit var qualityDialog: AlertDialog
    private val mediaItemList = arrayListOf<MediaItem>()
    private var videoQualities: ArrayList<String>? = null
    private var videoUrls: ArrayList<String>? = null
    private var currentUrl: String = ""
    private lateinit var trackSelector: DefaultTrackSelector
    private var isAudioOnly=false
    private lateinit var titleTextView:TextView
    private lateinit var qualityTrackLayout:LinearLayout
    private lateinit var  qualityLayout:View
    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoScreenBinding.inflate(inflater,container,false)

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // Inflate the layout for this fragment

        playerView = binding.videoView
        qualityButton = playerView.findViewById(R.id.quality_button)
        titleTextView = binding.titleofplayer
        trackSelector = DefaultTrackSelector(activity as MainActivity2)
        player = ExoPlayer.Builder(activity as MainActivity2).setTrackSelector(trackSelector).build()
        qualityLayout = layoutInflater.inflate(R.layout.qualitytrack, null)
        qualityTrackLayout= qualityLayout.findViewById(R.id.quality_track)
        playerView.player = player
        setupFullscreenHandler()

    }
    override fun onMovedto(){
        setupPlayer()
        setupVideoTitle()
        setupQualitySelector()
        setupPreviewImages()
    }

        @OptIn(UnstableApi::class)
        private fun setupPlayer() {

//            playerView.findViewById<ImageButton>(R.id.miniplayer_button).setOnClickListener { sendData() }


            // Get video URLs and qualities
            videoUrls = arguments?.getStringArrayList(MainActivity2.KEY_VIDEO_LINKS)
            videoQualities = arguments?.getStringArrayList(MainActivity2.KEY_VIDEO_QUALITY)
            Log.d("main", videoUrls?.size.toString())

            if (videoUrls.isNullOrEmpty()) {
                Log.e("VideoScreen", "No video URLs provided")
                return // Exit the method early if no videos are available
            }


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

//        private fun sendData() {
//            val resultIntent = Intent()
//            resultIntent.putExtra("videoUrl", videoUrls?.get(0))
//            resultIntent.putExtra("playbackPosition", player?.currentPosition ?: 0)
//            resultIntent.putExtra("isPlaying", player?.isPlaying ?: false)
//            resultIntent.putExtra("wasAudioOnly",isAudioOnly )
//            setResult(RESULT_OK, resultIntent)
//            finish()
//        }

        private fun setupVideoTitle() {
            val url: String? = arguments?.getString("url")


            // Extract video title
            val title = url?.substring(29)?.replace("-", " ")?.replaceFirstChar { it.uppercase() }
            titleTextView.text = title
        }

        private fun setupQualitySelector() {

            "Quality: ${videoQualities?.firstOrNull() ?: "N/A"}".also { qualityButton.text = it }



            // Populate quality options
            videoQualities?.forEachIndexed { index, quality ->
                val qualityButton = layoutInflater.inflate(
                    R.layout.qualitybutton_layout,
                    qualityTrackLayout,
                    false
                ) as Button
                qualityButton.text = quality
                qualityButton.setOnClickListener { changeQuality(index) }
                qualityTrackLayout.addView(qualityButton)
            }
            val audioOnlyButton = layoutInflater.inflate(
                R.layout.qualitybutton_layout,
                qualityTrackLayout,
                false
            ) as Button
            "Audio-Only".also { audioOnlyButton.text = it }
            audioOnlyButton.setOnClickListener { audioOnlyButtonPressed() }
            qualityTrackLayout.addView(audioOnlyButton)

            // Create quality dialog
            qualityDialog = AlertDialog.Builder(activity as MainActivity2)
                .setView(qualityLayout)
                .create()

            qualityButton.setOnClickListener {
                player?.pause()
                qualityDialog.show()
            }
        }

        @OptIn(UnstableApi::class)
        private fun audioOnlyButtonPressed() {

            val trackSelectionParameters = TrackSelectionParameters.Builder(activity as MainActivity2)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true) // Disable video tracks
                .build()
            trackSelector.setParameters(trackSelectionParameters)
            isAudioOnly=true

            qualityDialog.dismiss()
            player?.play()

            "Quality: AUDIO-ONLY".also { qualityButton.text = it }

        }


        @OptIn(UnstableApi::class)
        private fun changeQuality(index: Int) {
            val trackSelectionParameters = TrackSelectionParameters.Builder(activity as MainActivity2)
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false) // Disable video tracks
                .build()
            trackSelector.setParameters(trackSelectionParameters)
            isAudioOnly=false

            val position = player?.currentPosition ?: 0
            player?.setMediaItem(mediaItemList[index])
            player?.prepare()
            player?.seekTo(position)
            player?.playWhenReady = true
            qualityDialog.dismiss()
            currentUrl = videoUrls?.get(index) ?: ""


            "Quality: ${videoQualities?.get(index) ?: "N/A"}".also { qualityButton.text = it }
        }

        private fun setupFullscreenHandler() {
            val fullscreenButton = playerView.findViewById<ImageView>(R.id.exo_fullscreen_icon)
            val collapsingToolbar: CollapsingToolbarLayout = binding.collapsingToolbar
            var isFullscreen = false

            fullscreenButton.setOnClickListener {
                val windowInsetsController = WindowCompat.getInsetsController((activity as MainActivity2).window, (activity as MainActivity2).window.decorView)
                if (isFullscreen) {
                    exitFullscreen(windowInsetsController, collapsingToolbar, fullscreenButton)
                } else {
                    enterFullscreen(windowInsetsController, collapsingToolbar, fullscreenButton)
                }
                isFullscreen = !isFullscreen
            }
        }

        private fun enterFullscreen(
            controller: WindowInsetsControllerCompat,
            toolbar: CollapsingToolbarLayout,
            button: ImageView
        ) {
            (activity as MainActivity2).isInFullscreen = true
            button.setImageDrawable(ContextCompat.getDrawable(activity as MainActivity2, R.drawable.ic_fullscreen_close))
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            (activity as MainActivity2).supportActionBar?.hide()
            toolbar.layoutParams = (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
                scrollFlags = 0
            }
            (activity as MainActivity2).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            playerView.layoutParams = playerView.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                val displayMetrics = resources.displayMetrics
                val screenHeight = displayMetrics.widthPixels
                height = screenHeight
            }
            (activity as MainActivity2).onFullscreen()

        }

        private fun exitFullscreen(
            controller: WindowInsetsControllerCompat,
            toolbar: CollapsingToolbarLayout,
            button: ImageView
        ) {
            (activity as MainActivity2).isInFullscreen = false
            button.setImageDrawable(ContextCompat.getDrawable((activity as MainActivity2), R.drawable.ic_fullscreen_open))
            controller.show(WindowInsetsCompat.Type.systemBars())
            (activity as MainActivity2).supportActionBar?.show()
            toolbar.layoutParams = (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
                scrollFlags =
                    AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            }
            (activity as MainActivity2).requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            playerView.layoutParams = playerView.layoutParams.apply {
                height = 300.dp
            }
            (activity as MainActivity2).offFullscreen()
        }

        private fun setupPreviewImages() {
            val photosLayout: LinearLayout = binding.photos
            val pictures = arguments?.getStringArrayList(MainActivity2.KEY_PICTURES)

            pictures?.forEach { picture ->
                val imageView = ImageView(activity).apply {
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


