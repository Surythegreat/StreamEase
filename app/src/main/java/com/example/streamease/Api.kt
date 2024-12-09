package com.example.streamease

import com.example.streamease.Models.PageData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface Api  {
    @GET("popular")
    fun getPopular(
        @Header("Authorization")
        Credential:String,
        @Query("page")
        page:Int,
        @Query("per_page")
        per_page:Int
    ): Call<PageData>
}