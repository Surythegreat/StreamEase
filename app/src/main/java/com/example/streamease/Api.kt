package com.example.streamease

import com.example.streamease.Models.PageData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.http.GET
import retrofit2.http.Header

interface Api  {
    @GET("popular")
    fun getPopular(
        @Header("Authorization")
        Credential:String

    ): Call<PageData>
}