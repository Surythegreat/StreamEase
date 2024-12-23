package com.example.streamease.FragmentScenes

import android.annotation.SuppressLint
import android.app.ActionBar.LayoutParams
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore


@UnstableApi
class VideoScreen : scenes() {

    private var Min_quality: String? = null
    private var Min_url: String? = null
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
    private lateinit var photosLayout: LinearLayout
    val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private lateinit var likeButton: ImageView
    private lateinit var dislikeButton: ImageView
    private lateinit var likeCount: TextView
    private lateinit var dislikeCount: TextView
    private var likes = 0
    private var dislikes = 0
    val userId = Firebase.auth.currentUser?.uid
    override fun navid(): Int {
        return R.id.navigation_videoplay
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoScreenBinding.inflate(inflater,container,false)

        return binding.root
    }

    @SuppressLint("InflateParams")
    override fun onStart() {
        super.onStart()

        // Inflate the layout for this fragment
        photosLayout = binding.photos
        playerView = binding.videoView
        qualityButton = playerView.findViewById(R.id.quality_button)
        titleTextView = binding.titleofplayer
        trackSelector = DefaultTrackSelector(activity as MainActivity2)
        player =
            ExoPlayer.Builder(activity as MainActivity2).setTrackSelector(trackSelector).build()

        qualityLayout = layoutInflater.inflate(R.layout.qualitytrack, null)
        qualityTrackLayout= qualityLayout.findViewById(R.id.quality_track)
        playerView.findViewById<ImageButton>(R.id.miniplayer_button)
            .setOnClickListener { sendData() }
        binding.Save.setOnClickListener { (activity as MainActivity2 ).SaveCurrentVideo() }
        playerView.player = player
        setupFullscreenHandler()


        likeButton = binding.likeButton
        dislikeButton = binding.dislikeButton
        likeCount = binding.likeCount
        dislikeCount = binding.dislikeCount


    }

    private var isUpdating = false
        set(value) {
            binding.likeDislikeLoading.visibility = if (value) {
                View.VISIBLE
            } else {
                View.GONE
            }
            field = value

        }// Add a flag to prevent multiple updates

    private fun updateLikeDislike(videoId: Int, field: String) {
        if (isUpdating) {
            Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        isUpdating = true // Lock the update process
        val videoRef = Firebase.firestore.collection("Videos").document(videoId.toString())

        if (userId == null) {
            isUpdating = false
            return
        }

        val interactionRef = videoRef.collection("interactions").document(userId)

        // Check if the document exists
        videoRef.get().addOnSuccessListener { videoDoc ->
            if (!videoDoc.exists()) {
                // Create the document if it doesn't exist
                videoRef.set(mapOf("likes" to 0, "dislikes" to 0))
                    .addOnSuccessListener {
                        Log.d("updateLike", "Document created successfully.")
                        performUpdate(videoRef, interactionRef, field)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to create document: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.d("updateLike", "Failed to create document: ${e.message}")
                        isUpdating = false
                    }
            } else {
                // Proceed with the update if the document exists
                performUpdate(videoRef, interactionRef, field)
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error checking document: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.d("updateLike", "Error fetching document: ${e.message}")
            isUpdating = false
        }
    }

    private fun performUpdate(
        videoRef: DocumentReference,
        interactionRef: DocumentReference,
        field: String
    ) {
        interactionRef.get().addOnSuccessListener { interactionDoc ->
            val oppositeField = if (field == "likes") "dislikes" else "likes"
            val currentAction = interactionDoc.getString("action")
            val isAlreadyPerformed = currentAction == field

            // Firestore transaction to toggle counts atomically
            Firebase.firestore.runTransaction { transaction ->
                val snapshot = transaction.get(videoRef)
                val currentFieldCount = snapshot.getLong(field) ?: 0
                val oppositeFieldCount = snapshot.getLong(oppositeField) ?: 0

                if (isAlreadyPerformed) {
                    // User is toggling off the current action
                    transaction.update(videoRef, field, currentFieldCount - 1)
                    transaction.delete(interactionRef)
                } else {
                    // User is performing a new action
                    transaction.update(videoRef, field, currentFieldCount + 1)
                    if (currentAction == oppositeField) {
                        // If the user had previously done the opposite action, decrement it
                        transaction.update(videoRef, oppositeField, oppositeFieldCount - 1)
                    }
                    transaction.set(interactionRef, mapOf("action" to field))
                }
            }.addOnSuccessListener {
                // Update UI after successful transaction
                if (isAlreadyPerformed) {
                    // Toggled off
                    if (field == "likes") {
                        likes--
                        likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2,R.color.dark_white))
                        likeCount.text = likes.toString()

                    } else {
                        dislikes--
                        dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2,R.color.dark_white))
                        dislikeCount.text = dislikes.toString()
                    }
                } else {
                    // Toggled on
                    if (field == "likes") {
                        likes++
                        likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2,R.color.blue))
                        likeCount.text = likes.toString()
                        if (currentAction == "dislikes") {
                            dislikes--
                            dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2,R.color.dark_white))
                            dislikeCount.text = dislikes.toString()
                        }
                    } else {
                        dislikes++

                        dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2,R.color.red))
                        dislikeCount.text = dislikes.toString()
                        if (currentAction == "likes") {
                            likes--
                            likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2,R.color.dark_white))
                            likeCount.text = likes.toString()
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to update $field: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.d("updateLike", "Failed to update video data: ${e.message}")
            }.addOnCompleteListener {
                isUpdating = false // Unlock the update process
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error checking interaction: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.d("updateLike", "Error fetching interaction: ${e.message}")
            isUpdating = false
        }
    }





    private fun fetchVideoData(videoId: Int) {
        val videoRef = Firebase.firestore.collection("Videos").document(videoId.toString())

        videoRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    likes = document.getLong("likes")?.toInt() ?: 0
                    dislikes = document.getLong("dislikes")?.toInt() ?: 0

                    likeCount.text = likes.toString()
                    dislikeCount.text = dislikes.toString()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load video data: ${e.message}", Toast.LENGTH_SHORT).show()

            }
        if (userId != null) {
            videoRef.collection("interactions").document(userId).get().addOnSuccessListener { r->
                val currentAction = r.getString("action")
                if(currentAction=="likes"){
                    likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2,R.color.blue))
                }
                if(currentAction == "dislikes"){
                    dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2,R.color.red))
                }
            }
        }
    }


    override fun onMovedto(){
            setupPlayer()
            setupVideoTitle()
            setupQualitySelector()
            setupPreviewImages()

            val videoId = arguments?.getInt(MainActivity2.KEY_VIDEO_IDS)
            if (videoId != null) {
                fetchVideoData(videoId)
            }

            likeButton.setOnClickListener {
                if (videoId != null) {
                    updateLikeDislike(videoId, "likes")
                } else {
                    Toast.makeText(context, "Invalid video ID", Toast.LENGTH_SHORT).show()
                }
            }

            dislikeButton.setOnClickListener {
                if (videoId != null) {
                    updateLikeDislike(videoId, "dislikes")
                } else {
                    Toast.makeText(context, "Invalid video ID", Toast.LENGTH_SHORT).show()
                }
            }
    }

        @OptIn(UnstableApi::class)
        fun setupPlayer() {



            // Get video URLs and qualities
            videoUrls = arguments?.getStringArrayList(MainActivity2.KEY_VIDEO_LINKS)
            videoQualities = arguments?.getStringArrayList(MainActivity2.KEY_VIDEO_QUALITY)
            Log.d("main", videoUrls?.size.toString())

            if (videoUrls.isNullOrEmpty()) {
                Log.e("VideoScreen", "No video URLs provided")
                return // Exit the method early if no videos are available
            }
            Min_url = arguments?.getString(MainActivity2.KEY_MIN_video)
            val uri = Uri.parse(Min_url)
            val mediaItem = MediaItem.fromUri(uri)

            // Populate media items
            videoUrls?.forEachIndexed { index, url ->
                if (Min_url == url) {
                    Min_quality = videoQualities?.get(index)
                }
                val uri = Uri.parse(url)
                val mediaItem = MediaItem.fromUri(uri)
                mediaItemList.add(mediaItem)
            }
            playerView.player = player

            // Prepare the player
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
        }

    private fun sendData() {
        player?.let {
            Min_url.let { it1 ->
                if (it1 != null) {
                    (activity as MainActivity2).onPlayerLaunch(
                        it1,
                        it.currentPosition, player!!.isPlaying, isAudioOnly
                    )
                }
            }
        }
        player?.pause()
        playerView.player = null
    }

    override fun onMovedFrom() {
        super.onMovedFrom()
        player?.pause()
        playerView.player = null
    }

        private fun setupVideoTitle() {
            val url: String? = arguments?.getString("url")


            // Extract video title
            val title = url?.substring(29)?.replace("-", " ")?.replaceFirstChar { it.uppercase() }
            titleTextView.text = title
        }

        private fun setupQualitySelector() {

            "Quality: ${Min_quality}".also { qualityButton.text = it }
            qualityTrackLayout.removeAllViews()


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

            photosLayout.removeAllViews()
            val pictures = arguments?.getStringArrayList(MainActivity2.KEY_PICTURES)

            pictures?.forEach { picture ->
                val imageView = ImageView(activity).apply {
                    Glide.with(this).load(picture).into(this)
                    background= ColorDrawable(resources.getColor(R.color.lighter_black,null))
                    layoutParams = LinearLayout.LayoutParams( 150.dp, LayoutParams.WRAP_CONTENT).apply {
                        setPadding(10.dp,0,10.dp,0)
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


