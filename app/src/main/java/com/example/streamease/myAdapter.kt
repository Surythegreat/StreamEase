package com.example.streamease

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.streamease.Models.Video
import com.google.android.material.imageview.ShapeableImageView

class myAdapter(val context:Activity, val arrayList: List<Video>?):
    Adapter<myAdapter.MyviewHolder>() {
    class MyviewHolder(val itemview:View):ViewHolder(itemview) {
        var image:ShapeableImageView
        var titletex:TextView
        var Durationtex:TextView
        init {
            image = itemview.findViewById(R.id.thumbnailimage)
            titletex = itemview.findViewById(R.id.title)
            Durationtex = itemview.findViewById(R.id.DESC)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): myAdapter.MyviewHolder {
        val card = LayoutInflater.from(context).inflate(R.layout.eachvideothumb,parent,false)
        return MyviewHolder(card)
    }

    override fun onBindViewHolder(holder: myAdapter.MyviewHolder, position: Int) {
        holder.titletex.text = arrayList?.get(position)?.url?.substring(29)?.replace("-"," ") ?: "0"
        holder.Durationtex.text = buildString {
            append("Duration:")
            arrayList?.get(position)?.let { append(it.duration) }
        }
        Glide.with(context).load(arrayList?.get(position)?.image).into(holder.image)
    }

    override fun getItemCount(): Int {
        return arrayList?.size?:0;
    }
}