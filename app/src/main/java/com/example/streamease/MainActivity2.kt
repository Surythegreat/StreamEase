package com.example.streamease

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.streamease.FragmentScenes.MainScene
import com.example.streamease.FragmentScenes.SavedVideos
import com.example.streamease.FragmentScenes.VideoScreen
import com.example.streamease.FragmentScenes.profileView
import com.example.streamease.FragmentScenes.scenes
import com.example.streamease.Models.Video
import com.example.streamease.databinding.ActivityMain2Binding
import com.example.streamease.helper.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import io.github.hyuwah.draggableviewlib.DraggableView
import io.github.hyuwah.draggableviewlib.setupDraggable
import jp.wasabeef.blurry.Blurry
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.awaitResponse

@UnstableApi
class MainActivity2 : AppCompatActivity() {
    val apiKEY: String = "ugpXVoRZqu4YZYA4pIRXwVYP8Mgyn5O3aZBYkTC2Z5CFn7tgZCz4M5ml"

    private lateinit var binding: ActivityMain2Binding
    private lateinit var nav: BottomNavigationView
    var hassearched: Boolean = false
    var lastquery: String = ""

    // Add this to MainActivity2
    var isInFullscreen: Boolean = false
    private val mainScene = MainScene()       // Home fragment
    private val videoScreen = VideoScreen() // Video screen fragment
    private val profileScene = profileView() // Video screen fragment
    private val SavedScene = SavedVideos() // Video screen fragment
    private var activeFragment: scenes = mainScene
    private lateinit var searchContainer: LinearLayout
    private lateinit var videosearch: SearchView
    private lateinit var cancelButton: TextView
    private lateinit var touchInterceptor: View
    private lateinit var blurryView: ImageView
    private lateinit var someDraggableView: DraggableView<PlayerView>
    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    lateinit var Savedvideos:MutableList<Video>
    private lateinit var currentvideo:Video

    val userid = FirebaseAuth.getInstance().currentUser?.uid
    var db = Firebase.firestore

    @SuppressLint("ClickableViewAccessibility")
    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.logo.setOnClickListener { onLOGOPressed() }
        searchContainer = binding.searchContainer
        videosearch = binding.videoSearch
        cancelButton = binding.cancelButton
        touchInterceptor = binding.touchInterceptor
        blurryView = binding.blurryView
        Savedvideos = mutableListOf()
        // Initialize ExoPlayer
        trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()

        // Setup floating player (UI)
        SetupFloatingPlayer()

        nav = binding.navView
        setupFragments()
        nav.selectedItemId = R.id.navigation_home
        nav.menu.findItem(R.id.navigation_videoplay).isEnabled = false
        showFragment(mainScene)
        nav.setOnItemSelectedListener {
            if (isInFullscreen) {
                // Ignore navigation actions in fullscreen mode
                return@setOnItemSelectedListener false
            }

            when (it.itemId) {
                R.id.navigation_videoplay -> {
                    showFragment(videoScreen)
                }

                R.id.navigation_home -> {
                    showFragment(mainScene)
                }
                R.id.navigation_myAcc -> {
                    showFragment(profileScene)
                }
                R.id.navigation_hisNsavV -> {
                    showFragment(SavedScene)
                }
            }
            true
        }
        videosearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                onSearched(query)
                toggleSearch()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
        binding.searchButton.setOnClickListener { toggleSearch() }
        cancelButton.setOnClickListener { toggleSearch() }
        touchInterceptor.setOnTouchListener { _, _ -> true }

        fetchVideos()
    }

    private fun fetchVideos() {

        if (userid != null) {
            db.collection("User").document(userid).collection("SAVED").get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                            for (document in documents) {
                                val videoId = document.getLong("videoId")?.toInt()
                                if (videoId != null) {
                                    lifecycleScope.launch {
                                        val video = getVideoById(videoId)
                                        if (video != null) {
                                            Log.d("MainActivity", "Fetched video: ${video.id}")
                                            Savedvideos+=video
                                            SavedScene.UpdateSaved()

                                        }

                                    }
                                }
                            }

                    }
                    SavedScene.UpdateSaved()
                }
        }
    }

    private suspend fun getVideoById(videoId: Int): Video? {
        Log.d("getVideoById", "Fetching video with ID: $videoId")


        return try {
            val res = RetrofitClient.instance?.api?.getVideo(apiKEY, videoId)?.awaitResponse()
            if(res!=null && res.isSuccessful){
                res.body()

            }else {
                Log.e("getVideoById", "Failed with response code: ${res?.code()}")
                null
            }
//                override fun onResponse(call: Call<Video>, response: Response<Video>) {
//                    if (response.isSuccessful && response.body() != null) {
//                        video = response.body()
//                        Log.d("getVideoById", "Video fetched successfully: ${video?.id}")
//                    } else {
//                        Log.e("getVideoById", "Failed with response code: ${response.code()}, message: ${response.message()}")
//                    }
//                }
//
//                override fun onFailure(call: Call<Video>, t: Throwable) {
//                    Log.e("getVideoById", "API call failed: ${t.message}", t)
//                }
//            })
        } catch (e: Exception) {
            Log.e("getVideoById", "Exception while fetching video: ${e.message}", e)
            null
        }

    }


     fun onLOGOPressed() {
        SavedScene.UpdateSaved()
        hassearched = false
        nav.selectedItemId = R.id.navigation_home
        showFragment(mainScene)
        mainScene.Reset()
//        player.stop()
//        binding.floatingPlayer.visibility = View.GONE

    }

    private fun toggleSearch() {
        if (searchContainer.visibility == View.GONE) {
            searchContainer.visibility = View.VISIBLE
            touchInterceptor.visibility = View.VISIBLE
            blurryView.requestLayout() // Force layout update
            captureBlur()
            videosearch.isIconifiedByDefault = false
            videosearch.isIconified = false
            videosearch.requestFocusFromTouch()
            showKeyboard(videosearch)
        } else {
            searchContainer.visibility = View.GONE
            blurryView.visibility = View.INVISIBLE
            touchInterceptor.visibility = View.GONE
            hideKeyboard()
        }
    }

    private fun SetupFloatingPlayer() {
        binding.floatingPlayer.visibility = View.GONE
        binding.floatingPlayer.player = player
        val but = binding.floatingPlayer.findViewById<ImageButton>(R.id.close)
        but.setOnClickListener { closeplayer() }
        but.visibility = View.VISIBLE
        binding.floatingPlayer.findViewById<LinearLayout>(R.id.Bottom_bar).visibility = View.GONE
        binding.floatingPlayer.findViewById<ImageButton>(R.id.miniplayer_button).visibility =
            View.GONE
        someDraggableView = binding.floatingPlayer.setupDraggable()
            .setStickyMode(DraggableView.Mode.STICKY_X)
            .setAnimated(true)
            .build()
    }

    private fun closeplayer() {
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


        // Resume playback in MainActivity's ExoPlayer
        player.setMediaItem(MediaItem.fromUri(videoUrl))
        player.seekTo(playbackPosition)
        player.prepare()
        player.playWhenReady = isPlaying
        binding.floatingPlayer.visibility = View.VISIBLE
        val trackSelectionParameters = TrackSelectionParameters.Builder(this)
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isAudionly) // Disable video tracks
            .build()
        trackSelector.setParameters(trackSelectionParameters)
    }


    private fun captureBlur() {
        blurryView.post {
            binding.main.postDelayed({
                Log.d(
                    "MainActivity",
                    "Main dimensions: ${binding.main.width}x${binding.main.height}"
                )
                Log.d(
                    "MainActivity",
                    "BlurryView dimensions: ${blurryView.width}x${blurryView.height}"
                )
                Blurry.with(this)
                    .radius(25)
                    .sampling(2)
                    .capture(binding.main)
                    .into(blurryView)
                blurryView.visibility = View.VISIBLE
            }, 50)
        }

    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        view.post {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            Log.d("MainActivity", "Keyboard shown")
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(videosearch.windowToken, 0)
        Log.d("MainActivity", "Keyboard hidden")
    }

    private fun onSearched(query: String?) {
        showFragment(mainScene)
        nav.selectedItemId = R.id.navigation_home
        mainScene.Reset(query)
        hassearched = true

        if (query != null) {
            lastquery = query
        }

    }

    private fun setupFragments() {
        // Add all fragments to the FragmentManager but hide them initially
        supportFragmentManager.beginTransaction()
            .add(R.id.Replacable_frame, videoScreen, "VideoScreen")
            .hide(videoScreen)
            .commit()
        supportFragmentManager.beginTransaction()
            .add(R.id.Replacable_frame, profileScene, "ProfileScene")
            .commit()
        supportFragmentManager.beginTransaction()
            .add(R.id.Replacable_frame, SavedScene, "SavedVideoScene")
            .commit()


        supportFragmentManager.beginTransaction()
            .add(R.id.Replacable_frame, mainScene, "MainScene")
            .hide(videoScreen)
            .hide(profileScene)
            .hide(SavedScene)
            .commit()
         // MainScene is the default fragment

    }

    @OptIn(UnstableApi::class)
    fun strartVideoScene(video: Video) {
        if (video.video_files.isEmpty()) {
            Log.e("MainActivity2", "No video files available for playback")
            return
        }
        val bundle = Bundle()
        bundle.putString("url", video.url)
        val videolinklist: ArrayList<String> = arrayListOf()
        val videoqualitylist: ArrayList<String> = arrayListOf()
        val pictures: ArrayList<String> = arrayListOf()
        var minVideoLink = ""
        var minVideoheight = Int.MAX_VALUE
        for (vid in video.video_files) {
            videolinklist.add(vid.link)
            if (vid.height < minVideoheight) {
                minVideoheight = vid.height
                minVideoLink = vid.link
            }
            videoqualitylist.add("${vid.quality} : ${vid.width}X${vid.height}")
        }
        for (vid in video.video_pictures) {
            pictures.add(vid.picture)
        }
        bundle.putStringArrayList(KEY_VIDEO_LINKS, videolinklist)
        bundle.putString(KEY_MIN_video, minVideoLink)
        bundle.putStringArrayList(KEY_VIDEO_QUALITY, videoqualitylist)
        bundle.putStringArrayList(KEY_PICTURES, pictures)
        videoScreen.arguments = bundle
        showFragment(videoScreen)
        nav.selectedItemId = R.id.navigation_videoplay
        currentvideo=video

        nav.menu.findItem(R.id.navigation_videoplay).isEnabled = true
    }
    fun SaveCurrentVideo(){
        if (Savedvideos.any { it.id == currentvideo.id }) {
            Log.d("MainActivity", "Video already exists in the saved list.")
            return
        }

        val videoId = currentvideo.id // Assuming `id` is an Int

        // Save the video ID as an Int in FirSavedScene.UpdateSaved()ebase
        val videoData = hashMapOf("videoId" to videoId)
        db.collection("User").document(userid!!).collection("SAVED").document(videoId.toString())
            .set(videoData)
            .addOnSuccessListener {
                Log.d("MainActivity", "Video saved to Firebase successfully!")
                // Add the video to the local Savedvideos list
                if (Savedvideos.any { it.id == currentvideo.id }) {
                    Log.d("MainActivity", "Video already exists in the saved list.")
                }
                Savedvideos.add(currentvideo)
                SavedScene.UpdateSaved()
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error saving video: ", exception)
            }
    }
    private fun showFragment(fragment: scenes) {
        if (fragment == activeFragment) return


        val transaction = supportFragmentManager.beginTransaction()
        transaction.hide(activeFragment)
        transaction.show(fragment)
        transaction.commit()
        fragment.onMovedto()
        activeFragment.onMovedFrom()
        activeFragment = fragment
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasSearched", hassearched)
        outState.putString("lastQuery", lastquery)
    }

    fun onFullscreen() {
        nav.clearAnimation()
        nav.visibility = View.GONE
        binding.topView.visibility = View.GONE
    }

    fun offFullscreen() {
        nav.clearAnimation()
        nav.visibility = View.VISIBLE
        binding.topView.visibility = View.VISIBLE
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        hassearched = savedInstanceState.getBoolean("hasSearched", false)
        lastquery = savedInstanceState.getString("lastQuery", "")
    }

    fun removeSavedVideo(position: Int) {
        if (position < 0 || position >= Savedvideos.size) {
            Log.e("MainActivity2", "Invalid position: $position. List size: ${Savedvideos.size}")
            return
        }
        db.collection("User").document(userid!!).collection("SAVED").document(Savedvideos[position].id.toString()).delete()
            .addOnSuccessListener {
                if (position < 0 || position >= Savedvideos.size) {
                    Log.e("MainActivity2", "Invalid position: $position. List size: ${Savedvideos.size}")
                }else{
                Savedvideos.removeAt(position)
                SavedScene.UpdateSaved()}
            }
    }

    fun Refresh() {
        Toast.makeText(this,"refreshed",Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val KEY_VIDEO_LINKS = "Video links"
        const val KEY_MIN_video = "Video min"
        const val KEY_VIDEO_QUALITY = "video quality"
        const val KEY_PICTURES = "pictures"
    }

}