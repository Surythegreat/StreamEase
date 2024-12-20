package com.example.streamease.FragmentScenes

import android.content.Context
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
import com.example.streamease.MainActivity2
import com.example.streamease.Models.PageData
import com.example.streamease.Models.Video
import com.example.streamease.databinding.FragmentMainSceneBinding
import com.example.streamease.helper.RetrofitClient
import com.example.streamease.helper.myAdapter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@UnstableApi
class MainScene : scenes() {

    private val apiKEY: String = "ugpXVoRZqu4YZYA4pIRXwVYP8Mgyn5O3aZBYkTC2Z5CFn7tgZCz4M5ml"

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity2) {
            mainActivity = context
        } else {
            throw ClassCastException("$context must be MainActivity2")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainSceneBinding.inflate(inflater,container,false)
        loadingPB = binding.idPBLoading
        notfoundtext = binding.notfoundtext
        recycleV = binding.recycleview
        nestedScrollView = binding.nestedscrollview


        fetchData(page,totalRes)
        setUpPagination()
        return binding.root
    }

    fun Reset(query: String? = null){
        page = 1
        videolist = mutableListOf()
        totalRes = Int.MAX_VALUE
        fetchData(page, totalRes,query)
    }

    fun responseHandle(response: Response<PageData>) {
        if ((response.body()?.videos?.size ?: 0) == 0) {
            val t = Throwable("no Videos Found")
            failureHandle(t)
        } else {
            notfoundtext.visibility = View.GONE
        }
        videolist.addAll(response.body()?.videos ?: emptyList())
        val adapter = myAdapter(mainActivity, videolist)
        recycleV.adapter = adapter
        recycleV.layoutManager = LinearLayoutManager(activity)
        adapter.setOnItemClickListner(object : myAdapter.onItemClickListner {
            @OptIn(UnstableApi::class)
            override fun onItemClick(position: Int) {
                mainActivity.strartVideoScene(videolist[position])
            }

        })
        totalRes = response.body()?.total_results!! / perPage
    }
    private fun fetchData(
        page: Int,
        totalpages: Int,
        query: String? = null
    ) {
        if (page > totalpages) {
            Toast.makeText(activity, "That's all the data.", Toast.LENGTH_SHORT).show()
            loadingPB.visibility = View.GONE
            return
        }

        val call = if (query == null) {
            RetrofitClient.instance?.api?.getPopular(apiKEY, page, perPage)
        } else {
            RetrofitClient.instance?.api?.getSearched(apiKEY, page, perPage, query)
        }

        call?.enqueue(object : Callback<PageData> {
            override fun onResponse(call: Call<PageData>, response: Response<PageData>) {
                responseHandle(response)
            }

            override fun onFailure(call: Call<PageData>, t: Throwable) {
                failureHandle(t)
            }
        })
    }

    private fun setUpPagination() {
        nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                page++
                loadingPB.visibility = View.VISIBLE
                if (!mainActivity.hassearched) {
                    fetchData(page, totalRes)
                } else {
                    fetchData(page, totalRes, mainActivity.lastquery)
                }
            }
        })
    }

    fun failureHandle(t: Throwable) {
        Toast.makeText(activity, t.message, Toast.LENGTH_SHORT).show()
        notfoundtext.text = t.message
        notfoundtext.visibility = View.VISIBLE
        loadingPB.visibility = View.GONE
    }
}