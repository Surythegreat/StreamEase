package com.example.streamease

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.streamease.Models.PageData
import com.example.streamease.Models.Video
import com.example.streamease.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private val apiKEY: String = "ugpXVoRZqu4YZYA4pIRXwVYP8Mgyn5O3aZBYkTC2Z5CFn7tgZCz4M5ml"
    private lateinit var binding: ActivityMainBinding
    private lateinit var recycleV: RecyclerView
    private var page: Int = 1
    private var perPage: Int = 20
    private var totalRes:Int= Int.MAX_VALUE
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var linearLayoutManager:LinearLayoutManager
    private  lateinit var loadingPB:ProgressBar
    private lateinit var videolist:List<Video>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        videolist = listOf()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        nestedScrollView = binding.nestedscrollview
        recycleV = binding.recycleview
        linearLayoutManager=LinearLayoutManager(this@MainActivity)
        loadingPB =binding.idPBLoading
        fetchData(binding, page, totalRes)
        setUpPagination()
    }

    private fun setUpPagination() {
            nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                // on scroll change we are checking when users scroll as bottom.
                if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                    // in this method we are incrementing page number,
                    // making progress bar visible and calling get data method.
                    page++
                    loadingPB.visibility = View.VISIBLE
                    fetchData(binding,page,totalRes)
                }
            })



    }

    private fun fetchData(binding: ActivityMainBinding, i: Int, tot: Int) {
        if (i > tot) {
            // checking if the page number is greater than limit.
            // displaying toast message in this case when page>limit.
            Toast.makeText(this, "That's all the data..", Toast.LENGTH_SHORT).show()

            // hiding our progress bar.
            loadingPB.visibility = View.GONE
            return
        }
        RetrofitClient.instance?.api?.getPopular(apiKEY,i,perPage)?.enqueue(object : Callback<PageData> {
            override fun onResponse(p0: Call<PageData>, p1: Response<PageData>) {
                for (vid in p1.body()?.videos!!){
                    videolist+=vid
                }
                val adapter = myAdapter(this@MainActivity, videolist)
                recycleV.adapter = adapter
                recycleV.layoutManager = linearLayoutManager
                adapter.setOnItemClickListner(object :myAdapter.onItemClickListner{
                    override fun onItemClick(position: Int) {
                        val intent = Intent(this@MainActivity,VideoPlayscreen::class.java)
                        intent.putExtra("url",videolist[position].url)
//                        intent.putExtra("hdvideourl",videolist[position].video_files[0].link)
//                        intent.putExtra("sdvideourl",videolist[position].video_files[1].link)
                        val videolinklist: ArrayList<String> = arrayListOf()
                        for ( vid in videolist[position].video_files){
                            videolinklist.add(vid.link)
                        }
                        intent.putExtra("Videolinks",videolinklist)
                        startActivity(intent)
                    }
                })

                binding.PageNo.text = buildString {
                    append("Page No.:")
                    append(p1.body()?.page.toString())
                }
                this@MainActivity.totalRes = p1.body()?.total_results!!
                binding.totalRES.text = buildString {
                    append("TOTAL RESULTS:")
                    append(p1.body()?.total_results.toString())
                }
            }

            override fun onFailure(p0: Call<PageData>, p1: Throwable) {
                val tV: TextView = binding.PageNo
                tV.text = buildString {
                    append("Error")
                    append(p1.message)
                }
            }

        })
    }
}