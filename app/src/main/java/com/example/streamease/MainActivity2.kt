package com.example.streamease

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.transition.Scene
import com.example.streamease.FragmentScenes.MainScene
import com.example.streamease.FragmentScenes.VideoScreen
import com.example.streamease.FragmentScenes.scenes
import com.example.streamease.Models.Video
import com.example.streamease.databinding.ActivityMain2Binding
import com.google.android.material.bottomnavigation.BottomNavigationView

@UnstableApi
class MainActivity2 : AppCompatActivity() {

    private lateinit var binding: ActivityMain2Binding
    private lateinit var nav:BottomNavigationView
    var hassearched: Boolean = false
    var lastquery: String = ""
    // Add this to MainActivity2
    var isInFullscreen: Boolean = false
    private val mainScene = MainScene()       // Home fragment
    private val videoScreen = VideoScreen() // Video screen fragment
    private var activeFragment: Fragment = mainScene


    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        nav=binding.navView
        setupFragments()
        nav.selectedItemId =R.id.navigation_home
        showFragment(mainScene)
        nav.setOnItemSelectedListener {
            if (isInFullscreen) {
                // Ignore navigation actions in fullscreen mode
                return@setOnItemSelectedListener false
            }

            when(it.itemId){
                R.id.navigation_videoplay->showFragment(videoScreen)
                R.id.navigation_home->showFragment(mainScene)
            }
            true
        }
    }

    private fun setupFragments() {
        // Add all fragments to the FragmentManager but hide them initially
        supportFragmentManager.beginTransaction()
            .add(R.id.Replacable_frame, videoScreen, "VideoScreen")
            .hide(videoScreen)
            .commit()

        supportFragmentManager.beginTransaction()
            .add(R.id.Replacable_frame, mainScene, "MainScene")
            .commit() // MainScene is the default fragment
    }

    @OptIn(UnstableApi::class)
    fun strartVideoScene( video: Video) {
        if (video.video_files.isEmpty()) {
            Log.e("MainActivity2", "No video files available for playback")
            return
        }
        val bundle = Bundle()
        bundle.putString("url",video.url)
        val videolinklist: ArrayList<String> = arrayListOf()
        val videoqualitylist: ArrayList<String> = arrayListOf()
        val pictures: ArrayList<String> = arrayListOf()
        for (vid in video.video_files) {
            videolinklist.add(vid.link)
            videoqualitylist.add("${vid.quality} : ${vid.width}X${vid.height}")
        }
        for (vid in video.video_pictures) {
            pictures.add(vid.picture)
        }
        bundle.putStringArrayList(KEY_VIDEO_LINKS, videolinklist)
        bundle.putStringArrayList(KEY_VIDEO_QUALITY, videoqualitylist)
        bundle.putStringArrayList(KEY_PICTURES, pictures)
        videoScreen.arguments=bundle
        showFragment(videoScreen)
        nav.selectedItemId =R.id.navigation_videoplay
    }

    private fun showFragment(fragment: scenes) {
        if (fragment == activeFragment) return

        val transaction = supportFragmentManager.beginTransaction()
        transaction.hide(activeFragment)
        transaction.show(fragment)
        transaction.commit()
        fragment.onMovedto()
        activeFragment = fragment
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("hasSearched", hassearched)
        outState.putString("lastQuery", lastquery)
    }

    fun onFullscreen()
    {
        nav.clearAnimation()
        nav.visibility = View.GONE
        binding.topView.visibility=View.GONE
    }
    fun offFullscreen(){
        nav.clearAnimation()
        nav.visibility = View.VISIBLE
        binding.topView.visibility=View.VISIBLE
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        hassearched = savedInstanceState.getBoolean("hasSearched", false)
        lastquery = savedInstanceState.getString("lastQuery", "")
    }

    companion object {
        const val KEY_VIDEO_LINKS = "Video links"
        const val KEY_VIDEO_QUALITY = "video quality"
        const val KEY_PICTURES = "pictures"
    }

}