package com.example.streamease

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.streamease.databinding.ActivityMain2Binding
import com.example.streamease.fragmentscenes.MainScene
import com.example.streamease.fragmentscenes.ProfileView
import com.example.streamease.fragmentscenes.SavedVideos
import com.example.streamease.fragmentscenes.Scenes
import com.example.streamease.fragmentscenes.VideoScreen
import com.example.streamease.helper.RetrofitClient
import com.example.streamease.models.Video
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.DynamicLink.AndroidParameters
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.ShortDynamicLink
import com.google.firebase.firestore.FirebaseFirestore
import io.github.hyuwah.draggableviewlib.DraggableView
import io.github.hyuwah.draggableviewlib.setupDraggable
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.launch
import retrofit2.awaitResponse


@OptIn(UnstableApi::class)
class MainActivity2 : AppCompatActivity() {

    var miniplayerurl: String = ""
    private lateinit var binding: ActivityMain2Binding
    private lateinit var nav: BottomNavigationView
    private val mainScene = MainScene()
    private val videoScreen = VideoScreen()
    private val profileScene = ProfileView()
    private val savedScene = SavedVideos()
    private var activeFragment: Scenes = mainScene
    private val searchContainer by lazy { binding.searchContainer }
    private val videosearch by lazy { binding.videoSearch }
    private val cancelButton by lazy { binding.cancelButton }
    private val touchInterceptor by lazy { binding.touchInterceptor }
    private val blurryView by lazy { binding.blurryView }
    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private var currentvideo: Video? = null
    var isInFullscreen = false
    val db = FirebaseFirestore.getInstance()
    val userid = FirebaseAuth.getInstance().currentUser?.uid
    var lastquery:String? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        initializeBindings()
        initializePlayer()
        setupNavigation()
        setupSearch()
        setupBackPressHandler()
        setupSharing()
    }

    private fun setupSharing() {
        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener { pendingDynamicLinkData ->

                if (pendingDynamicLinkData != null) {
                    val deepLink: Uri? = pendingDynamicLinkData.getLink()
                    if (deepLink != null) {
                        val videoId = deepLink . getQueryParameter ("id")
                        if (videoId != null) {
                            lifecycleScope.launch {
                                val video: Video? = getVideoById(videoId.toInt())
                                if (video != null) {
                                    strartVideoScene(video)
                                }
                            }
                        }
                        val userId = deepLink.getQueryParameter("userid")
                        if (userId != null) {
                            showUser(userId)
                        }
                    }
                }
            }
            .addOnFailureListener{
                    e -> Log.w("DynamicLink", "Error retrieving link", e)}

    }

    private fun showUser(userId: String) {
        showFragment(profileScene)
        profileScene.SearchId(userId)
    }

    fun shareUser() {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse("https://streamease.com/user?userid=$userid"))
            .setDomainUriPrefix("https://streamease.page.link")
            .setAndroidParameters(AndroidParameters.Builder().build())
            .buildShortDynamicLink()
            .addOnSuccessListener { shortDynamicLink: ShortDynamicLink ->
                val shortLink = shortDynamicLink.shortLink
                val shareIntent = Intent()
                shareIntent.setAction(Intent.ACTION_SEND)
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "See Me on StreamEase: " + shortLink.toString()
                )
                shareIntent.setType("text/plain")
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
            .addOnFailureListener { e: java.lang.Exception? ->
                Log.e(
                    "DynamicLink",
                    "Error creating link",
                    e
                )
            }
    }

    fun shareVideoLink(videoId: Int) {
        FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse("https://streamease.com/video?id=$videoId"))
            .setDomainUriPrefix("https://streamease.page.link")
            .setAndroidParameters(AndroidParameters.Builder().build())
            .buildShortDynamicLink()
            .addOnSuccessListener { shortDynamicLink: ShortDynamicLink ->
                val shortLink = shortDynamicLink.shortLink
                val shareIntent = Intent()
                shareIntent.setAction(Intent.ACTION_SEND)
                shareIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this video: " + shortLink.toString()
                )
                shareIntent.setType("text/plain")
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
            .addOnFailureListener { e: java.lang.Exception? ->
                Log.e(
                    "DynamicLink",
                    "Error creating link",
                    e
                )
            }
    }

    private fun initializeBindings() {
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.logo.setOnClickListener { onLOGOPressed() }

    }

    private fun initializePlayer() {
        trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        setupFloatingPlayer()
    }

    private fun setupNavigation() {
        nav = binding.navView
        setupFragments()
        showFragment(mainScene)
        nav.setOnItemSelectedListener {
            if (isInFullscreen || nav.selectedItemId == it.itemId) return@setOnItemSelectedListener false
            when (it.itemId) {
                R.id.navigation_videoplay -> showFragment(videoScreen)
                R.id.navigation_home -> showFragment(mainScene)
                R.id.navigation_myAcc -> showFragment(profileScene)
                R.id.navigation_hisNsavV -> showFragment(savedScene)
            }
            true
        }
    }

    private fun setupSearch() {
        videosearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                onSearched(query)
                toggleSearch()
                return false
            }

            override fun onQueryTextChange(newText: String?) = false
        })
        binding.searchButton.setOnClickListener { toggleSearch() }
        cancelButton.setOnClickListener { toggleSearch() }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    searchContainer.visibility == View.VISIBLE -> toggleSearch()
                    isInFullscreen -> videoScreen.fullscreenButton.callOnClick()
                    activeFragment != mainScene ->showFragment(mainScene)
                    else -> finish()
                }
            }
        })
    }



    suspend fun getVideoById(videoId: Int): Video? {
        return try {
            RetrofitClient.instance?.api?.getVideo(APIKEY, videoId)?.awaitResponse()
                ?.takeIf { it.isSuccessful }?.body()
        } catch (e: Exception) {
            null
        }
    }

    private fun onLOGOPressed() {
        savedScene.updateSaved()
        lastquery=null
        showFragment(mainScene)
        mainScene.reset()
    }

    private fun toggleSearch() {
        if (searchContainer.visibility == View.GONE) {
            searchContainer.visibility = View.VISIBLE
            touchInterceptor.visibility = View.VISIBLE
            blurryView.requestLayout()
            captureBlur()
            videosearch.apply {
                isIconifiedByDefault = false
                isIconified = false
                requestFocusFromTouch()
            }
            showKeyboard(videosearch)
        } else {
            searchContainer.visibility = View.GONE
            blurryView.visibility = View.INVISIBLE
            touchInterceptor.visibility = View.GONE
            hideKeyboard()
        }
    }

    private fun setupFloatingPlayer() {
        binding.floatingPlayer.visibility = View.GONE
        binding.floatingPlayer.player = player
        binding.floatingPlayer.findViewById<ImageButton>(R.id.close).apply {
            setOnClickListener { closePlayer() }
            visibility = View.VISIBLE
        }
        binding.floatingPlayer.findViewById<LinearLayout>(R.id.Bottom_bar).visibility = View.GONE
        binding.floatingPlayer.findViewById<ImageButton>(R.id.miniplayer_button).visibility =
            View.GONE
        binding.floatingPlayer.setupDraggable().setStickyMode(DraggableView.Mode.NON_STICKY)
            .setAnimated(true).build()
    }

    private fun closePlayer() {
        videoScreen.isMiniPlayerActive = false
        videoScreen.setupPlayer()
        binding.floatingPlayer.visibility = View.GONE
        player.stop()
    }

    fun onPlayerLaunch(
        videoUrl: String,
        playbackPosition: Long,
        isPlaying: Boolean,
        isAudionly: Boolean
    ) {
        miniplayerurl = videoUrl
        player.apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            seekTo(playbackPosition)
            prepare()
            playWhenReady = isPlaying
        }
        binding.floatingPlayer.visibility = View.VISIBLE
        val trackSelectionParameters = TrackSelectionParameters.Builder(this)
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isAudionly)
            .build()
        trackSelector.setParameters(trackSelectionParameters)
    }

    private fun captureBlur() {
        blurryView.postDelayed({
            Blurry.with(this)
                .radius(25)
                .sampling(2)
                .capture(binding.main)
                .into(blurryView)
            blurryView.visibility = View.VISIBLE
        }, 50)
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }





    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(videosearch.windowToken, 0)
    }

    private fun onSearched(query: String?) {
        showFragment(mainScene)
        mainScene.reset(query)
        lastquery = query!!
    }

    private fun setupFragments() {
        supportFragmentManager.beginTransaction()
            .apply {
                add(R.id.Replacable_frame, videoScreen, "VideoScreen")
                add(R.id.Replacable_frame, profileScene, "ProfileScene")
                add(R.id.Replacable_frame, savedScene, "SavedVideoScene")
                add(R.id.Replacable_frame, mainScene, "MainScene")
                hide(videoScreen).hide(profileScene).hide(savedScene)
            }
            .commit()
        savedScene.arguments = Bundle().apply { putString("id",userid)
        putBoolean("isfree",true)}
    }

    fun strartVideoScene(video: Video) {
        video.video_files.takeIf { it.isNotEmpty() }?.let {
            val bundle = Bundle().apply {
                putString("url", video.url)
                putInt(KEY_VIDEO_IDS, video.id)
                putStringArrayList(KEY_VIDEO_LINKS, ArrayList(it.map { vid -> vid.link }))
                putString(KEY_MIN_VIDEO, it.minByOrNull { vid -> vid.height }?.link.orEmpty())
                putStringArrayList(
                    KEY_VIDEO_QUALITY,
                    ArrayList(it.map { "${it.quality} : ${it.width}X${it.height}" })
                )
                putStringArrayList(KEY_PICTURES, ArrayList(video.video_pictures.map { it.picture }))
            }
            videoScreen.arguments = bundle
            currentvideo = video
            showFragment(videoScreen)
        }
    }

    fun saveCurrentVideo() {
        currentvideo?.let { savedScene.saveCurrentVideo(it) }
    }

    private fun showFragment(fragment: Scenes) {
        if (fragment == activeFragment) return
        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            show(fragment)
        }.commit()
        activeFragment.onMovedFrom()
        activeFragment = fragment
        activeFragment.onMovedto()
        nav.selectedItemId = fragment.navid()
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    fun toggleFullscreen(isFullscreen: Boolean) {
        if (isFullscreen) {
            nav.visibility = View.GONE
            binding.topView.visibility = View.GONE
        } else {
            nav.visibility = View.VISIBLE
            binding.topView.visibility = View.VISIBLE
        }
    }



    fun onVideoSaved(video: Video) {
       if(video.id== currentvideo?.id){
            videoScreen.onVideoSaved()
        }
    }

    fun isSaved(): Boolean {
        return savedScene.savedvideos.contains(currentvideo)
    }

    fun RemoveVideo() {
        currentvideo?.let { savedScene.removeSavedVideoID(it) }
    }

    fun onVideoRemoved(video: Video) {
        if(video==currentvideo){
            videoScreen.onVideoRemoved()
        }
    }

    companion object {
        const val APIKEY = "ugpXVoRZqu4YZYA4pIRXwVYP8Mgyn5O3aZBYkTC2Z5CFn7tgZCz4M5ml"
        const val KEY_VIDEO_LINKS = "Video links"
        const val KEY_VIDEO_IDS = "Video_ID"
        const val KEY_MIN_VIDEO = "Video min"
        const val KEY_VIDEO_QUALITY = "video quality"
        const val KEY_PICTURES = "pictures"
    }
}
