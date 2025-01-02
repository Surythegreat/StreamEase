package com.example.streamease.fragmentscenes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.widget.NestedScrollView
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.streamease.MainActivity2
import com.example.streamease.models.PageData
import com.example.streamease.models.Video
import com.example.streamease.R
import com.example.streamease.databinding.FragmentMainSceneBinding
import com.example.streamease.helper.RetrofitClient
import com.example.streamease.helper.MyAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@UnstableApi
class MainScene : Scenes() {

    private lateinit var binding: FragmentMainSceneBinding
    private var page: Int = 1
    private var perPage: Int = 20
    private var totalRes: Int = Int.MAX_VALUE
    private lateinit var notfoundtext: TextView
    private lateinit var loadingPB: ProgressBar
    private var videolist: MutableList<Video> = mutableListOf()
    private lateinit var recycleV: RecyclerView
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var mainActivity: MainActivity2
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun navid(): Int {
        return R.id.navigation_home
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainActivity =activity as MainActivity2
        binding = FragmentMainSceneBinding.inflate(inflater, container, false)
        loadingPB = binding.idPBLoading
        notfoundtext = binding.notfoundtext
        recycleV = binding.recycleview
        nestedScrollView = binding.nestedscrollview
        swipeRefreshLayout = binding.swipeRefreshLayout

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = true
            reset()
            swipeRefreshLayout.postDelayed({
                swipeRefreshLayout.isRefreshing = false
            }, 1500)
        }

        fetchData(page, totalRes)
        setUpPagination()
        return binding.root
    }

    fun reset(query: String? = null) {
        page = 1
        videolist.clear()
        totalRes = Int.MAX_VALUE
        notfoundtext.visibility = View.GONE
        fetchData(page, totalRes, query)
    }

    fun responseHandle(response: Response<PageData>) {
        if (response.body()?.videos.isNullOrEmpty()) {
            failureHandle(Throwable("NO VIDEOS FOUND"))
        } else {
            notfoundtext.visibility = View.GONE
            videolist.addAll(response.body()?.videos ?: emptyList())
            val adapter = MyAdapter(mainActivity, videolist, false)
            recycleV.adapter = adapter
            recycleV.layoutManager = LinearLayoutManager(activity)
            adapter.setOnItemClickListner(object : MyAdapter.OnItemClickListner {
                @OptIn(UnstableApi::class)
                override fun onItemClick(position: Int) {
                    mainActivity.strartVideoScene(videolist[position])
                }
            })
        }
        totalRes = (response.body()?.total_results ?: 0) / perPage
    }

    private var currentCall: Call<PageData>? = null

    private fun fetchData(page: Int, totalpages: Int, query: String? = null) {
        if (page > totalpages) {
            Toast.makeText(activity, "That's all the data.", Toast.LENGTH_SHORT).show()
            loadingPB.visibility = View.GONE
            return
        }

        currentCall?.cancel()

        currentCall = if (query == null) {
            RetrofitClient.instance?.api?.getPopular(MainActivity2.APIKEY, page, perPage)
        } else {
            RetrofitClient.instance?.api?.getSearched(MainActivity2.APIKEY, page, perPage, query)
        }

        currentCall?.enqueue(object : Callback<PageData> {
            override fun onResponse(call: Call<PageData>, response: Response<PageData>) {
                if (call.isCanceled) return
                responseHandle(response)
            }

            override fun onFailure(call: Call<PageData>, t: Throwable) {
                if (call.isCanceled) return
                failureHandle(t)
            }
        })
    }

    private fun setUpPagination() {
        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                page++
                loadingPB.visibility = View.VISIBLE
                fetchData(page, totalRes, mainActivity.lastquery)
            }
        })
    }

    fun failureHandle(t: Throwable) {
        Toast.makeText(activity, t.message, Toast.LENGTH_SHORT).show()
        notfoundtext.visibility = View.VISIBLE
        recycleV.adapter = MyAdapter(mainActivity, listOf(), false)
        loadingPB.visibility = View.GONE
    }
}
