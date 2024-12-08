package com.example.streamease

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.streamease.Models.PageData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private val APIKEY:String = "ugpXVoRZqu4YZYA4pIRXwVYP8Mgyn5O3aZBYkTC2Z5CFn7tgZCz4M5ml"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        fetchData()
    }

    private fun fetchData() {
        RetrofitClient.instance?.api?.getPopular(APIKEY)?.enqueue(object : Callback<PageData> {
            override fun onResponse(p0: Call<PageData>, p1: Response<PageData>) {
                val tV:TextView = findViewById(R.id.tV)
                tV.text = p1.body()?.url
            }

            override fun onFailure(p0: Call<PageData>, p1: Throwable) {
                val tV:TextView = findViewById(R.id.tV)
                tV.text = buildString {
                    append("Error")
                    append(p1.message)
                }
            }

        })
    }
}