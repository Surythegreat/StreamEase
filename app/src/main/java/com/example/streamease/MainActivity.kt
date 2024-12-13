package com.example.streamease

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.streamease.Models.PageData
import com.example.streamease.Models.Video
import com.example.streamease.databinding.ActivityMainBinding
import jp.wasabeef.blurry.Blurry
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
    private var hassearched:Boolean = false
    private var lastquery:String = ""

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
        videosearch.setOnQueryTextListener(object:SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                onSearched(query)
                toggleSearch()
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })
        // Setup search button
        binding.searchButton.setOnClickListener { toggleSearch() }
        cancelButton.setOnClickListener { toggleSearch() }

        // Initialize other views
        nestedScrollView = binding.nestedscrollview
        recycleV = binding.recycleview
        linearLayoutManager = LinearLayoutManager(this)
        loadingPB = binding.idPBLoading
        videolist = listOf()

        fetchPopularData(page, totalRes/perPage)
        setUpPagination()

        // Set an onClickListener to consume touch events
        touchInterceptor.setOnTouchListener { _, _ -> true }
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
        page=1
        videolist= emptyList()
        hassearched=true
        if (query != null) {
            lastquery=query
        }
        fetchSearchedData(page,totalRes/perPage,query)
    }

    private fun fetchSearchedData(
        page: Int,
        totalpages: Int,
        query: String?
    ) {
        if (page > totalpages) {
            Toast.makeText(this, "That's all the data.", Toast.LENGTH_SHORT).show()
            loadingPB.visibility = View.GONE
            return
        }
        if (query != null) {
            RetrofitClient.instance?.api?.getSearched(apiKEY, page, perPage,query)?.enqueue(object : Callback<PageData> {
                override fun onResponse(call: Call<PageData>, response: Response<PageData>) {
                    for (vid in response.body()?.videos!!) {
                        videolist += vid
                    }
                    val adapter = myAdapter(this@MainActivity, videolist)
                    recycleV.adapter = adapter
                    recycleV.layoutManager = linearLayoutManager
                    adapter.setOnItemClickListner(object : myAdapter.onItemClickListner {
                        override fun onItemClick(position: Int) {
                            val intent = Intent(this@MainActivity, VideoPlayscreen::class.java)
                            intent.putExtra("url", videolist[position].url)
                            val videolinklist: ArrayList<String> = arrayListOf()
                            val videoqualitylist: ArrayList<String> = arrayListOf()
                            for (vid in videolist[position].video_files) {
                                videolinklist.add(vid.link)
                                videoqualitylist.add("${vid.quality} : ${vid.width}X${vid.height}")
                            }
                            intent.putExtra("Videolinks", videolinklist)
                            intent.putExtra("videoquality", videoqualitylist)
                            startActivity(intent)
                        }
                    })
                    this@MainActivity.totalRes = response.body()?.total_results!!
                }

                override fun onFailure(call: Call<PageData>, t: Throwable) {
                    Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
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
                if(hassearched==false){
                fetchPopularData( page, totalRes/perPage)}
                else{
                    fetchSearchedData(page,totalRes/perPage,lastquery)
                }
            }
        })
    }

    private fun fetchPopularData( i: Int, tot: Int) {
        if (i > tot) {
            Toast.makeText(this, "That's all the data.", Toast.LENGTH_SHORT).show()
            loadingPB.visibility = View.GONE
            return
        }
        RetrofitClient.instance?.api?.getPopular(apiKEY, i, perPage)?.enqueue(object : Callback<PageData> {
            override fun onResponse(call: Call<PageData>, response: Response<PageData>) {
                for (vid in response.body()?.videos!!) {
                    videolist += vid
                }
                val adapter = myAdapter(this@MainActivity, videolist)
                recycleV.adapter = adapter
                recycleV.layoutManager = linearLayoutManager
                adapter.setOnItemClickListner(object : myAdapter.onItemClickListner {
                    override fun onItemClick(position: Int) {
                        val intent = Intent(this@MainActivity, VideoPlayscreen::class.java)
                        intent.putExtra("url", videolist[position].url)
                        val videolinklist: ArrayList<String> = arrayListOf()
                        val videoqualitylist: ArrayList<String> = arrayListOf()
                        for (vid in videolist[position].video_files) {
                            videolinklist.add(vid.link)
                            videoqualitylist.add("${vid.quality} : ${vid.width}X${vid.height}")
                        }
                        intent.putExtra("Videolinks", videolinklist)
                        intent.putExtra("videoquality", videoqualitylist)
                        startActivity(intent)
                    }
                })
                this@MainActivity.totalRes = response.body()?.total_results!!
            }

            override fun onFailure(call: Call<PageData>, t: Throwable) {
                Toast.makeText(this@MainActivity, t.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
}
