package com.example.streamease.helper

import com.example.streamease.models.PageData
import com.example.streamease.models.Video
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface Api  {
    @GET("popular")
    fun getPopular(
        @Header("Authorization")
        credential:String,
        @Query("page")
        page:Int,
        @Query("per_page")
        perPage:Int
    ): Call<PageData>
    @GET("search")
    fun getSearched(
        @Header("Authorization")
        credential:String,
        @Query("page")
        page:Int,
        @Query("per_page")
        perPage:Int,
        @Query("query")
        query:String
    ): Call<PageData>
    @GET("videos/{id}")
    fun getVideo(
        @Header("Authorization") credential: String,
        @Path("id") id: Int
    ): Call<Video>

}