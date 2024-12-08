package com.example.streamease

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClient {
    private val builder = OkHttpClient.Builder()

    private val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor()


    init {
        val gson = GsonBuilder()
            .setLenient()
            .create()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(interceptor)


        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(builder.build())
            .build()
    }


    val api: Api
        get() = retrofit.create(Api::class.java)

    companion object {
        private const val BASE_URL = "https://api.pexels.com/videos/"
        private var retrofitClient: RetrofitClient? = null
        private lateinit var retrofit: Retrofit

        @get:Synchronized
        val instance: RetrofitClient?
            get() {
                if (retrofitClient == null) {
                    retrofitClient = RetrofitClient()
                }
                return retrofitClient
            }
    }
}