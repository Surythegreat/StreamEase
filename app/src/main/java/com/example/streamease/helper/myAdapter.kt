package com.example.streamease.helper

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.streamease.Models.Video
import com.example.streamease.R
import com.google.android.material.imageview.ShapeableImageView


class myAdapter(val context:Activity, val arrayList: List<Video>?):
    Adapter<myAdapter.MyviewHolder>() {
    private lateinit var myListner: onItemClickListner
    interface onItemClickListner{
        fun onItemClick(position: Int)
    }
    fun setOnItemClickListner(Listner: onItemClickListner){
        myListner = Listner
    }


    class MyviewHolder(private val itemview:View, Listner: onItemClickListner):ViewHolder(itemview) {
        var image:ShapeableImageView
        var titletex:TextView
        var Durationtex:TextView
        init {
            itemView.setOnClickListener {
                Listner.onItemClick(absoluteAdapterPosition)
            }
            image = itemview.findViewById(R.id.thumbnailimage)
            titletex = itemview.findViewById(R.id.title)
            Durationtex = itemview.findViewById(R.id.DESC)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyviewHolder {
        val card = LayoutInflater.from(context).inflate(R.layout.eachvideothumb,parent,false)
        return MyviewHolder(card,myListner)
    }

    override fun onBindViewHolder(holder: MyviewHolder, position: Int) {
        val s = arrayList?.get(position)?.url?.substring(29)?.replace("-"," ")
        if (s != null) {
            holder.titletex.text = buildString {
                append(s.uppercase()[0])
                append(s.substring(1))
            }
        }
        holder.Durationtex.text = buildString {
            arrayList?.get(position)?.let { append(it.duration/60)
                                            append(":")
                                            append(it.duration%60)}
        }
        Glide.with(holder.image).load(arrayList?.get(position)?.image).into(holder.image)

    }

    override fun getItemCount(): Int {
        return arrayList?.size?:0
    }
}