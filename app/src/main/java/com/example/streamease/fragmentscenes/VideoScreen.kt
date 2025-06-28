package com.example.streamease.fragmentscenes

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
import androidx.coordinatorlayout.widget.CoordinatorLayout
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.streamease.MainActivity2
import com.example.streamease.R
import com.example.streamease.databinding.FragmentVideoScreenBinding
import com.example.streamease.helper.CommentsAdapter
import com.example.streamease.helper.RetrofitClient
import com.example.streamease.models.Comment
import com.example.streamease.models.PageData
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar

@UnstableApi
class VideoScreen : Scenes() {

    private var minQuality: String? = null
    private var minUrl: String? = null
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
    private var isAudioOnly = false
    private lateinit var titleTextView: TextView
    private lateinit var qualityTrackLayout: LinearLayout
    private lateinit var qualityLayout: View
    private lateinit var photosLayout: LinearLayout
    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private lateinit var likeButton: ImageView
    private lateinit var dislikeButton: ImageView
    private lateinit var likeCount: TextView
    private lateinit var dislikeCount: TextView
    private var likes = 0
    private var dislikes = 0
    private val userId = FirebaseAuth.getInstance().currentUser?.uid
    override fun navid(): Int {
        return R.id.navigation_videoplay
    }
    private val commentsList = mutableListOf<Comment>()
    private var isSaved:Boolean=false
    private var videoId = 0

    private lateinit var mainActivity: MainActivity2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVideoScreenBinding.inflate(inflater, container, false)

        mainActivity = activity as MainActivity2
        photosLayout = binding.photos
        playerView = binding.videoView
        qualityButton = playerView.findViewById(R.id.quality_button)
        titleTextView = binding.titleofplayer
        trackSelector = DefaultTrackSelector(mainActivity)
        player = ExoPlayer.Builder(mainActivity).setTrackSelector(trackSelector).build()

        qualityLayout = LayoutInflater.from(mainActivity).inflate(R.layout.qualitytrack, null)
        qualityTrackLayout = qualityLayout.findViewById(R.id.quality_track)
        playerView.findViewById<ImageButton>(R.id.miniplayer_button).setOnClickListener { sendData() }
        binding.Save.setOnClickListener {

            if (!isSaved) {
                mainActivity.saveCurrentVideo()
            } else {
                mainActivity.RemoveVideo()
            }
            isUpdating=true
        }
        playerView.player = player
        setupFullscreenHandler()

        likeButton = binding.likeButton
        dislikeButton = binding.dislikeButton
        likeCount = binding.likeCount
        dislikeCount = binding.dislikeCount

        binding.Share.setOnClickListener { mainActivity.shareVideoLink(videoId) }
        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        player = null
    }

    private var isUpdating = false
        set(value) {
            binding.likeDislikeLoading.visibility = if (value) View.VISIBLE else View.GONE

            Toast.makeText(mainActivity,value.toString(), Toast.LENGTH_SHORT).show()
            field = value
        }

    private fun updateLikeDislike(videoId: Int, field: String) {
        if (isUpdating) {
            Toast.makeText(context, "Please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        isUpdating = true
        val videoRef = FirebaseFirestore.getInstance().collection("Videos").document(videoId.toString())

        if (userId == null) {
            isUpdating = false
            return
        }

        val interactionRef = videoRef.collection("interactions").document(userId)

        interactionRef.get().addOnSuccessListener { interactionDoc ->

            // 🔁 Step 1: Optimistically update the UI

            videoRef.get().addOnSuccessListener { docSnapshot ->
                if (!docSnapshot.exists()) {
                    // Create document with 0 likes/dislikes
                    videoRef.set(mapOf("likes" to 0, "dislikes" to 0))
                        .addOnSuccessListener {
                            // Proceed with the transaction after it's created
                            runLikeDislikeTransaction(videoRef, interactionRef, field,interactionDoc)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to create video entry.", Toast.LENGTH_SHORT).show()
                            isUpdating = false
                        }
                } else {
                    runLikeDislikeTransaction(videoRef, interactionRef, field, interactionDoc)
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error checking interaction: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.d("likeDislike1", e.message.toString())
            isUpdating = false
        }
    }
    private fun runLikeDislikeTransaction(
        videoRef: DocumentReference,
        interactionRef: DocumentReference,
        field: String,
        interactionDoc: DocumentSnapshot
    ) {

        val oppositeField = if (field == "likes") "dislikes" else "likes"

        val currentAction = interactionDoc.getString("action")
        val isAlreadyPerformed = currentAction == field
        val revertedState = applyOptimisticUIUpdate(field, currentAction, isAlreadyPerformed)

        FirebaseFirestore.getInstance().runTransaction { transaction ->
            val snapshot = transaction.get(videoRef)
            val currentFieldCount = snapshot.getLong(field) ?: 0
            val oppositeFieldCount = snapshot.getLong(oppositeField) ?: 0

            if (isAlreadyPerformed) {
                transaction.update(videoRef, field, currentFieldCount - 1)
                transaction.delete(interactionRef)
            } else {
                transaction.update(videoRef, field, currentFieldCount + 1)
                if (currentAction == oppositeField) {
                    transaction.update(videoRef, oppositeField, oppositeFieldCount - 1)
                }
                transaction.set(interactionRef, mapOf("action" to field))
            }


        }.addOnFailureListener { e ->
            // 🔁 Step 3: Revert UI if failed
            Toast.makeText(context, "Failed to update $field: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.d("likeDislike2", e.message.toString())
            revertUIChange(revertedState)
        }.addOnCompleteListener {
            isUpdating = false
        }
        
    }
    data class RevertedState(
        val field: String,
        val currentAction: String?,
        val wasAlreadyPerformed: Boolean
    )

    private fun applyOptimisticUIUpdate(field: String, currentAction: String?, isAlreadyPerformed: Boolean): RevertedState {
        if (isAlreadyPerformed) {
            if (field == "likes") {
                likes--
                likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.dark_white))
                likeCount.text = likes.toString()
            } else {
                dislikes--
                dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.dark_white))
                dislikeCount.text = dislikes.toString()
            }
        } else {
            if (field == "likes") {
                likes++
                likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.blue))
                likeCount.text = likes.toString()
                if (currentAction == "dislikes") {
                    dislikes--
                    dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.dark_white))
                    dislikeCount.text = dislikes.toString()
                }
            } else {
                dislikes++
                dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.red))
                dislikeCount.text = dislikes.toString()
                if (currentAction == "likes") {
                    likes--
                    likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.dark_white))
                    likeCount.text = likes.toString()
                }
            }
        }
        return RevertedState(field, currentAction, isAlreadyPerformed)
    }
    private fun revertUIChange(state: RevertedState) {
        val (field, currentAction, wasAlreadyPerformed) = state

        // Revert the changes made in applyOptimisticUIUpdate
        if (wasAlreadyPerformed) {
            if (field == "likes") {
                likes++
                likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.blue))
                likeCount.text = likes.toString()
            } else {
                dislikes++
                dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.red))
                dislikeCount.text = dislikes.toString()
            }
        } else {
            if (field == "likes") {
                likes--
                likeButton.setColorFilter(
                    ContextCompat.getColor(activity as MainActivity2, if (currentAction == "likes") R.color.blue else R.color.dark_white)
                )
                likeCount.text = likes.toString()
                if (currentAction == "dislikes") {
                    dislikes++
                    dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.red))
                    dislikeCount.text = dislikes.toString()
                }
            } else {
                dislikes--
                dislikeButton.setColorFilter(
                    ContextCompat.getColor(activity as MainActivity2, if (currentAction == "dislikes") R.color.red else R.color.dark_white)
                )
                dislikeCount.text = dislikes.toString()
                if (currentAction == "likes") {
                    likes++
                    likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.blue))
                    likeCount.text = likes.toString()
                }
            }
        }
    }


    private fun fetchVideoData(videoId: Int) {
        val videoRef = FirebaseFirestore.getInstance().collection("Videos").document(videoId.toString())

        videoRef.get()
            .addOnSuccessListener { document ->
                document?.let {
                    likes = it.getLong("likes")?.toInt() ?: 0
                    dislikes = it.getLong("dislikes")?.toInt() ?: 0
                    likeCount.text = likes.toString()
                    dislikeCount.text = dislikes.toString()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load video data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("fetchVideoData",e.message.toString())
            }

        userId?.let {
            videoRef.collection("interactions").document(it).get().addOnSuccessListener { r ->
                val currentAction = r.getString("action")
                updateButtonState(currentAction)
            }
        }
    }

    private fun updateButtonState(currentAction: String?) {
        val likeColor = ContextCompat.getColor(activity as MainActivity2, R.color.dark_white)
        val dislikeColor = ContextCompat.getColor(activity as MainActivity2, R.color.dark_white)

        when (currentAction) {
            "likes" -> {
                likeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.blue))
                dislikeButton.setColorFilter(dislikeColor)
            }
            "dislikes" -> {
                dislikeButton.setColorFilter(ContextCompat.getColor(activity as MainActivity2, R.color.red))
                likeButton.setColorFilter(likeColor)
            }
            else -> {
                likeButton.setColorFilter(likeColor)
                dislikeButton.setColorFilter(dislikeColor)
            }
        }
    }

    private var isSameVideo = false
    override fun onMovedto() {
        videoUrls = arguments?.getStringArrayList(MainActivity2.KEY_VIDEO_LINKS)
        videoQualities = arguments?.getStringArrayList(MainActivity2.KEY_VIDEO_QUALITY)

        val v = arguments?.getInt(MainActivity2.KEY_VIDEO_IDS) ?: 0
        isSameVideo = (v == videoId)
        videoId = v
        binding.appBarLayout.visibility = if (videoUrls.isNullOrEmpty()) View.GONE else View.VISIBLE
        binding.DetailsContainer.visibility = if (videoUrls.isNullOrEmpty()) View.GONE else View.VISIBLE

        setupVideoTitle()
        setupQualitySelector()
        setupPlayer()
        setupPreviewImages()
        setupLikeDislike()
        setupSearchVid()
        setupcomments()
    }

    private fun setupcomments() {
        commentsList.clear()
        val recyclerView = binding.commentsRecyclerView
        val commentEditText = binding.commentEditText
        val postCommentButton = binding.postCommentButton

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val db = FirebaseFirestore.getInstance()
        val adapter = CommentsAdapter(commentsList, object : CommentsAdapter.OnItemClickListner {
            override fun onItemClick(position: Int) {
                closeComment(position)
            }
        })
        recyclerView.adapter = adapter

        // Fetch existing comments
        Log.d("VideoID", "videoId: $videoId")

        db.collection("Videos").document(videoId.toString()).collection("Comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener {
                for (i in it) {
                    val comment = i.getString("userId")?.let { it1 -> Comment(it1,
                        i.getString("userName")!!,
                        i.getString("commentText")!!, i.getString("timestamp")!!, i.getString("time")!!, i.id) }
                    if (comment != null) {
                        commentsList.add(comment)
                    }
                }

                adapter.notifyDataSetChanged()
                binding.commentsRecyclerView.scrollToPosition((binding.commentsRecyclerView.adapter?.itemCount
                    ?: 1) - 1)
            }

        // Post a new comment
        postCommentButton.setOnClickListener {
            val commentText = commentEditText.text.toString().trim()
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null || commentText.isEmpty()) {
                Log.e("Comments", "User is not authenticated or comment is empty.")
                return@setOnClickListener
            }

            // Disable the button to prevent multiple clicks
            postCommentButton.isEnabled = false

            val userId = user.uid
            val ref = db.collection("Videos").document(videoId.toString())
                .collection("Comments")

            db.collection("User").document(userId).get().addOnSuccessListener { userDoc ->
                val userName = userDoc.getString("Name") ?: run {
                    Log.e("Comments", "User name missing in Firestore")
                    "Anonymous"
                }

                val documentId = ref.document().id
                val newComment = Comment(
                    userId = userId,
                    userName = userName,
                    commentText = commentText,
                    timestamp = Timestamp.now().toString(),
                    time = Calendar.getInstance().time.toString().substring(0, 20),
                    documentId = documentId
                )
                val hasm = hashMapOf(
                    "userId" to newComment.userId,
                    "userName" to newComment.userName,
                    "commentText" to newComment.commentText,
                    "timestamp" to newComment.timestamp,
                    "time" to newComment.time
                )

                ref.document(documentId).set(hasm)
                    .addOnSuccessListener {
                        commentEditText.text.clear()
                        commentsList.add(newComment)
                        adapter.notifyItemInserted(commentsList.lastIndex)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Comments", "Error adding comment: ${e.message}", e)
                        Toast.makeText(requireContext(), "Failed to post comment: ${e.message}", Toast.LENGTH_LONG).show()
                    }

                    .addOnCompleteListener {
                        // Re-enable the button after the operation
                        postCommentButton.isEnabled = true
                    }
            }.addOnFailureListener { e ->
                Log.w("Comments", "Error fetching user details", e)
                Toast.makeText(requireContext(), "Failed to fetch user details. Try again.", Toast.LENGTH_SHORT).show()
                postCommentButton.isEnabled = true
            }
            binding.commentsRecyclerView.scrollToPosition((binding.commentsRecyclerView.adapter?.itemCount
                ?: 1) - 1)
        }
    }

    private var isDeleteInProgress = false // Flag to track delete operations
    private var playbackPosition: Long = 0L
    private var previusmediaitempos: Int? = null

    override fun onMovedFrom() {
        super.onMovedFrom()
        playbackPosition = player?.currentPosition ?: 0L
        player?.pause()
        playerView.player = null
    }

    private fun closeComment(position: Int) {
        Log.d("Comments", "Attempting to delete comment at position $position")

        if (isDeleteInProgress) {
            Log.w("Comments", "Delete operation is already in progress. Ignoring subsequent clicks.")
            return
        }

        if (commentsList.isEmpty()) {
            Log.e("Comments", "commentsList is empty. Cannot delete comment.")
            return
        }

        if (position < 0 || position >= commentsList.size) {
            Log.e("Comments", "Invalid position: $position. List size: ${commentsList.size}")
            return
        }

        val comment = commentsList[position]
        if (comment.documentId.isNullOrEmpty()) {
            Log.e("Comments", "Invalid commentId. Cannot delete.")
            return
        }

        val commentId = comment.documentId
        Log.d("Comments", "Deleting comment with ID: $commentId")

        // Set the flag to prevent subsequent clicks
        isDeleteInProgress = true

        if (commentId != null) {
            FirebaseFirestore.getInstance()
                .collection("Videos")
                .document(videoId.toString())
                .collection("Comments")
                .document(commentId)
                .delete()
                .addOnSuccessListener {
                    Log.d("Comments", "Comment deleted successfully.")
                    if (position < commentsList.size) {
                        commentsList.removeAt(position)
                        binding.commentsRecyclerView.adapter?.notifyItemRemoved(position)
                        binding.commentsRecyclerView.adapter?.notifyItemRangeChanged(position, commentsList.size)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Comments", "Failed to delete comment", e)
                }
                .addOnCompleteListener {
                    isDeleteInProgress = false // Reset the flag once the operation is complete
                }
        }

        binding.commentsRecyclerView.scrollToPosition((binding.commentsRecyclerView.adapter?.itemCount
            ?: 1) - 1)
    }

    private fun setupSearchVid() {
        binding.PlayButton.setOnClickListener {
            val query = binding.QueryEdit.text.toString()
            RetrofitClient.instance?.api?.getSearched(
                MainActivity2.APIKEY,
                1,
                1,
                query
            )?.enqueue(object : Callback<PageData> {
                override fun onResponse(p0: Call<PageData>, p1: Response<PageData>) {
                    if (p1.body()?.videos.isNullOrEmpty()) {
                        Toast.makeText(activity as MainActivity2, "NO SUCH VIDEOS FOUND", Toast.LENGTH_SHORT).show()
                    } else {
                        p1.body()?.videos?.firstOrNull()?.let {
                            mainActivity.strartVideoScene(it)
                            onMovedto()
                        }
                    }
                }

                override fun onFailure(p0: Call<PageData>, p1: Throwable) {
                    Toast.makeText(activity as MainActivity2, p1.message, Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun setupLikeDislike() {
        fetchVideoData(videoId)

        likeButton.setOnClickListener {
            updateLikeDislike(videoId, "likes")
        }

        dislikeButton.setOnClickListener {
            updateLikeDislike(videoId, "dislikes")
        }
        isSaved = mainActivity.isSaved()
        binding.Save.setImageResource(if (isSaved) { R.drawable.saved } else { R.drawable.ussaved })
    }

    @OptIn(UnstableApi::class)
    fun setupPlayer() {
        if (videoUrls.isNullOrEmpty()) {
            Log.e("VideoScreen", "No video URLs provided")
            return
        }

        minUrl = arguments?.getString(MainActivity2.KEY_MIN_VIDEO)
        if (mainActivity.miniplayerurl == minUrl && isMiniPlayerActive) return

        val uri = Uri.parse(minUrl)
        val mediaItem = MediaItem.fromUri(uri)
        var minitempos = 0
        mediaItemList.clear()

        videoUrls?.forEachIndexed { index, url ->
            if (minUrl == url) {
                minQuality = videoQualities?.get(index)
                minitempos = index
            }
            mediaItemList.add(MediaItem.fromUri(Uri.parse(url)))
        }
        playerView.player = player
        if (!isSameVideo) {
            player?.setMediaItem(mediaItem)
            previusmediaitempos = minitempos
            "Quality: $minQuality".also { qualityButton.text = it }
        } else {
            player?.setMediaItem(mediaItemList[previusmediaitempos!!])
            qualityButton.text = buildString {
                append("Quality: ")
                append((videoQualities?.get(previusmediaitempos!!) ?: ""))
            }
            player?.seekTo(playbackPosition)
        }
        player?.prepare()
        player?.playWhenReady = true
    }

    var isMiniPlayerActive = false
    private fun sendData() {
        minUrl?.let { it2 ->
            player?.let {
                mainActivity.onPlayerLaunch(it2, it.currentPosition, it.isPlaying, isAudioOnly)
            }
        }
        isMiniPlayerActive = true
        player?.pause()
        playerView.player = null
    }

    private fun setupVideoTitle() {
        val url: String? = arguments?.getString("url")
        val title = url?.substring(29)?.replace("-", " ")?.replaceFirstChar { it.uppercase() }
        titleTextView.text = title
    }

    private fun setupQualitySelector() {
        qualityTrackLayout.removeAllViews()

        videoQualities?.forEachIndexed { index, quality ->
            val qualityButton = layoutInflater.inflate(R.layout.qualitybutton_layout, qualityTrackLayout, false) as Button
            qualityButton.text = quality
            qualityButton.setOnClickListener { changeQuality(index) }
            qualityTrackLayout.addView(qualityButton)
        }

        val audioOnlyButton = layoutInflater.inflate(R.layout.qualitybutton_layout, qualityTrackLayout, false) as Button
        "Audio-Only".also { audioOnlyButton.text = it }
        audioOnlyButton.setOnClickListener { audioOnlyButtonPressed() }
        qualityTrackLayout.addView(audioOnlyButton)
        if (qualityLayout.parent != null) {
            (qualityLayout.parent as ViewGroup).removeView(qualityLayout)
        }
        qualityDialog = AlertDialog.Builder(mainActivity)
            .setView(qualityLayout)
            .create()

        qualityButton.setOnClickListener {
            player?.pause()
            qualityDialog.show()
        }
    }

    @OptIn(UnstableApi::class)
    private fun audioOnlyButtonPressed() {
        val trackSelectionParameters = TrackSelectionParameters.Builder(mainActivity)
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
        trackSelector.setParameters(trackSelectionParameters)
        isAudioOnly = true
        qualityDialog.dismiss()
        player?.play()
        "Quality: AUDIO-ONLY".also { qualityButton.text = it }
    }

    @OptIn(UnstableApi::class)
    private fun changeQuality(index: Int) {
        val trackSelectionParameters = TrackSelectionParameters.Builder(mainActivity)
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .build()
        trackSelector.setParameters(trackSelectionParameters)
        isAudioOnly = false

        val position = player?.currentPosition ?: 0
        player?.setMediaItem(mediaItemList[index])
        previusmediaitempos = index
        player?.prepare()
        player?.seekTo(position)
        player?.playWhenReady = true
        qualityDialog.dismiss()
        currentUrl = videoUrls?.get(index) ?: ""
        "Quality: ${videoQualities?.get(index) ?: "N/A"}".also { qualityButton.text = it }
    }

    lateinit var fullscreenButton: ImageView

    private fun setupFullscreenHandler() {
        fullscreenButton = playerView.findViewById(R.id.exo_fullscreen_icon)
        val collapsingToolbar: CollapsingToolbarLayout = binding.collapsingToolbar
        val activity = activity as MainActivity2
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        fullscreenButton.setOnClickListener {
            Log.d("fullscreenbutton", activity.isInFullscreen.toString())
            val windowInsetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            if (activity.isInFullscreen) {
                exitFullscreen(windowInsetsController, collapsingToolbar, fullscreenButton)
            } else {
                enterFullscreen(windowInsetsController, collapsingToolbar, fullscreenButton)
            }
        }
    }

    private fun enterFullscreen(
        controller: WindowInsetsControllerCompat,
        toolbar: CollapsingToolbarLayout,
        button: ImageView
    ) {
        mainActivity.isInFullscreen = true
        button.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_fullscreen_close))

        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        mainActivity.supportActionBar?.hide()

        toolbar.layoutParams = (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = 0
        }
        (binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).apply {
            behavior = null
        }
        binding.appBarLayout.layoutParams = binding.appBarLayout.layoutParams

        mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        playerView.layoutParams = playerView.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = resources.displayMetrics.widthPixels
        }
        playerView.findViewById<ImageButton>(R.id.miniplayer_button).visibility = View.GONE
        mainActivity.toggleFullscreen(true)
    }

    private fun exitFullscreen(
        controller: WindowInsetsControllerCompat,
        toolbar: CollapsingToolbarLayout,
        button: ImageView
    ) {
        mainActivity.isInFullscreen = false
        button.setImageDrawable(ContextCompat.getDrawable(mainActivity, R.drawable.ic_fullscreen_open))

        controller.show(WindowInsetsCompat.Type.systemBars())
        mainActivity.supportActionBar?.show()

        toolbar.layoutParams = (toolbar.layoutParams as AppBarLayout.LayoutParams).apply {
            scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
        }
        mainActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        (binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams).apply {
            behavior = AppBarLayout.Behavior()
        }
        binding.appBarLayout.layoutParams = binding.appBarLayout.layoutParams

        playerView.layoutParams = playerView.layoutParams.apply {
            height = 300.dp
        }

        playerView.findViewById<ImageButton>(R.id.miniplayer_button).visibility = View.VISIBLE
        mainActivity.toggleFullscreen(false)
    }

    private fun setupPreviewImages() {
        photosLayout.removeAllViews()
        val pictures = arguments?.getStringArrayList(MainActivity2.KEY_PICTURES)

        pictures?.forEach { picture ->
            val imageView = ImageView(activity).apply {
                Glide.with(this).load(picture).into(this)
                background = ColorDrawable(resources.getColor(R.color.lighter_black, null))
                layoutParams = LinearLayout.LayoutParams(150.dp, LayoutParams.WRAP_CONTENT).apply {
                    setPadding(10.dp, 0, 10.dp, 0)
                }
            }
            photosLayout.addView(imageView)
        }
    }

    fun onVideoSaved() {
        isUpdating=false;
        binding.Save.setImageResource(R.drawable.saved)
        isSaved = true
    }

    fun onVideoRemoved() {
        isUpdating=false;
        binding.Save.setImageResource(R.drawable.ussaved)
        isSaved = false
    }
}
