package com.example.streamease

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.streamease.Models.PageData
import com.example.streamease.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private val APIKEY:String = "ugpXVoRZqu4YZYA4pIRXwVYP8Mgyn5O3aZBYkTC2Z5CFn7tgZCz4M5ml"
    private lateinit var binding:ActivityMainBinding;
    private lateinit var recycleV:RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)

        recycleV = binding.recycleview
        fetchData(binding)
    }

   private fun fetchData(binding: ActivityMainBinding) {
        RetrofitClient.instance?.api?.getPopular(APIKEY)?.enqueue(object : Callback<PageData> {
            override fun onResponse(p0: Call<PageData>, p1: Response<PageData>) {
                val videoslist = p1.body()?.videos;
                val adapter = myAdapter(this@MainActivity,videoslist)
                recycleV.adapter = adapter
                recycleV.layoutManager = LinearLayoutManager(this@MainActivity)

                binding.PageNo.text = buildString {
                    append("Page No.:")
                    append(p1.body()?.page.toString())
                }
                binding.totalRES.text = buildString {
                    append("TOTAL RESULTS:")
                    append(p1.body()?.total_results.toString())
                }
            }

            override fun onFailure(p0: Call<PageData>, p1: Throwable) {
                val tV:TextView = binding.PageNo;
                tV.text = buildString {
                    append("Error")
                    append(p1.message)
                }
            }

        })
    }
}