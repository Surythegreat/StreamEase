package com.example.streamease

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.streamease.Models.PageData
import com.example.streamease.Models.Video
import com.example.streamease.databinding.ActivityMainBinding
import io.github.hyuwah.draggableviewlib.DraggableView
import io.github.hyuwah.draggableviewlib.setupDraggable
import jp.wasabeef.blurry.Blurry
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@UnstableApi
class MainActivity : AppCompatActivity() {

    private val apiKEY: String = "ugpXVoRZqu4YZYA4pIRXwVYP8Mgyn5O3aZBYkTC2Z5CFn7tgZCz4M5ml"
    private lateinit var binding: ActivityMainBinding
    private lateinit var recycleV: RecyclerView
    private var page: Int = 1
    private var perPage: Int = 20
    private var totalRes: Int = Int.MAX_VALUE
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var loadingPB: ProgressBar
    private lateinit var videolist: List<Video>

    private lateinit var blurryView: ImageView
    private lateinit var searchContainer: LinearLayout
    private lateinit var videosearch: SearchView
    private lateinit var cancelButton: TextView
    private lateinit var touchInterceptor: View
    private lateinit var notfoundtext: TextView
    private var hassearched: Boolean = false
    private var lastquery: String = ""
    private lateinit var someDraggableView: DraggableView<PlayerView>
    private lateinit var videoPlayerLauncher: ActivityResultLauncher<Intent>
    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize view binding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        blurryView = binding.blurryView
        searchContainer = binding.searchContainer
        videosearch = binding.videoSearch
        cancelButton = binding.cancelButton
        touchInterceptor = binding.touchInterceptor
        notfoundtext = binding.notfoundtext
        nestedScrollView = binding.nestedscrollview
        recycleV = binding.recycleview
        linearLayoutManager = LinearLayoutManager(this)
        loadingPB = binding.idPBLoading
        videolist = listOf()
        binding.logo.setOnClickListener { onLOGOPressed() }

        // Initialize ExoPlayer
        trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()

        // Setup floating player (UI)
        SetupFloatingPlayer()

        // Register activity result launcher
        videoPlayerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            Log.d("bkjked", result.resultCode.toString())
            if (result.resultCode == RESULT_OK && result.data != null) {
                handleActivityResult(result.data!!)
            }
        }

        // Setup SearchView Listener
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

        // Setup search buttons
        binding.searchButton.setOnClickListener { toggleSearch() }
        cancelButton.setOnClickListener { toggleSearch() }
        touchInterceptor.setOnTouchListener { _, _ -> true }

        // Fetch popular data and setup pagination
        fetchPopularData(page, totalRes)
        setUpPagination()
    }

    private fun onLOGOPressed() {
        page = 1
        videolist = emptyList()
        hassearched = false
        totalRes = Int.MAX_VALUE
        player.stop()
        binding.floatingPlayer.visibility = View.GONE
        fetchPopularData(page, totalRes)
    }

    private fun SetupFloatingPlayer() {
        binding.floatingPlayer.visibility = View.GONE
        binding.floatingPlayer.player = player
        val but = binding.floatingPlayer.findViewById<ImageButton>(R.id.close)
        but.setOnClickListener { closeplayer() }
        but.visibility = View.VISIBLE
        binding.floatingPlayer.findViewById<LinearLayout>(R.id.Bottom_bar).visibility = View.GONE
        binding.floatingPlayer.findViewById<ImageButton>(R.id.miniplayer_button).visibility = View.GONE
        someDraggableView = binding.floatingPlayer.setupDraggable()
            .setStickyMode(DraggableView.Mode.STICKY_X)
            .setAnimated(true)
            .build()
    }

    private fun closeplayer() {
        binding.floatingPlayer.visibility = View.GONE
        player.stop()
    }

    private fun handleActivityResult(data: Intent) {
        val videoUrl = data.getStringExtra("videoUrl")
        val playbackPosition = data.getLongExtra("playbackPosition", 0)
        val isPlaying = data.getBooleanExtra("isPlaying", false)
        val isAudionly = data.getBooleanExtra("wasAudioOnly", false)

        // Resume playback in MainActivity's ExoPlayer
        if (videoUrl != null) {
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
    }

    private fun toggleSearch() {
        if (searchContainer.visibility == View.GONE) {
            searchContainer.visibility = View.VISIBLE
            touchInterceptor.visibility = View.VISIBLE
            captureBlur()
            videosearch.isIconifiedByDefault = false
            videosearch.isIconified = false  // Ensure it is expanded and ready for input
            videosearch.requestFocusFromTouch()
            showKeyboard(videosearch)
        } else {
            searchContainer.visibility = View.GONE
            blurryView.visibility = View.GONE
            touchInterceptor.visibility = View.GONE
            hideKeyboard()
        }
    }

    private fun onSearched(query: String?) {
        page = 1
        videolist = emptyList()
        hassearched = true
        totalRes = Int.MAX_VALUE
        if (query != null) {
            lastquery = query
        }
        fetchSearchedData(page, totalRes, query)
    }

    private fun fetchSearchedData(page: Int, totalpages: Int, query: String?) {
        if (page > totalpages) {
            Toast.makeText(this, "That's all the data.", Toast.LENGTH_SHORT).show()
            loadingPB.visibility = View.GONE
            return
        }
        if (query != null) {
            RetrofitClient.instance?.api?.getSearched(apiKEY, page, perPage, query)
                ?.enqueue(object : Callback<PageData> {
                    override fun onResponse(call: Call<PageData>, response: Response<PageData>) {
                        responseHandle(response)
                    }

                    override fun onFailure(call: Call<PageData>, t: Throwable) {
                        failureHandle(t)
                    }
                }
                )
        }
    }


    private fun captureBlur() {
        blurryView.post {
            Blurry.with(this)
                .radius(25)
                .sampling(2)
                .capture(binding.main)
                .into(blurryView)
            blurryView.visibility = View.VISIBLE
            Log.d("MainActivity", "Background blurred")
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

    private fun setUpPagination() {
        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                page++
                loadingPB.visibility = View.VISIBLE
                if (!hassearched) {
                    fetchPopularData(page, totalRes)
                } else {
                    fetchSearchedData(page, totalRes, lastquery)
                }
            }
        })
    }

    private fun fetchPopularData(i: Int, tot: Int) {
        if (i > tot) {
            Toast.makeText(this, "That's all the data.", Toast.LENGTH_SHORT).show()
            loadingPB.visibility = View.GONE
            return
        }
        RetrofitClient.instance?.api?.getPopular(apiKEY, i, perPage)
            ?.enqueue(object : Callback<PageData> {
                override fun onResponse(call: Call<PageData>, response: Response<PageData>) {
                    responseHandle(response)
                }

                override fun onFailure(call: Call<PageData>, t: Throwable) {
                    failureHandle(t)
                }
            })
    }

    fun responseHandle(response: Response<PageData>) {
        if ((response.body()?.videos?.size ?: 0) == 0) {
            val t = Throwable("no Videos Found")
            failureHandle(t)
        } else {
            notfoundtext.visibility = View.GONE
        }
        for (vid in response.body()?.videos!!) {
            videolist += vid
        }
        val adapter = myAdapter(this@MainActivity, videolist)
        recycleV.adapter = adapter
        recycleV.layoutManager = linearLayoutManager
        adapter.setOnItemClickListner(object : myAdapter.onItemClickListner {
            override fun onItemClick(position: Int) {
                strartVideoScene(position)
            }

        })
        this@MainActivity.totalRes = response.body()?.total_results!! / perPage
    }

    @OptIn(UnstableApi::class)
    private fun strartVideoScene(position: Int) {
        val intent = Intent(this@MainActivity, VideoPlayscreen::class.java)
        intent.putExtra("url", videolist[position].url)
        val videolinklist: ArrayList<String> = arrayListOf()
        val videoqualitylist: ArrayList<String> = arrayListOf()
        val pictures: ArrayList<String> = arrayListOf()
        for (vid in videolist[position].video_files) {
            videolinklist.add(vid.link)
            videoqualitylist.add("${vid.quality} : ${vid.width}X${vid.height}")
        }
        for (vid in videolist[position].video_pictures) {
            pictures.add(vid.picture)
        }

        intent.putExtra("Videolinks", videolinklist)
        intent.putExtra("videoquality", videoqualitylist)
        intent.putExtra("pictures", pictures)
        videoPlayerLauncher.launch(intent)
    }

    fun failureHandle(t: Throwable) {
        Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
        notfoundtext.text = t.message
        notfoundtext.visibility = View.VISIBLE
        loadingPB.visibility = View.GONE
    }
}


