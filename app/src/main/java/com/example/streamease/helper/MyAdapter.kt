package com.example.streamease.helper

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.example.streamease.models.Video
import com.example.streamease.R
import com.google.android.material.imageview.ShapeableImageView


class MyAdapter(private val context:Activity, private val arrayList: List<Video>?, private val issaved:Boolean):
    Adapter<MyAdapter.MyviewHolder>() {
    private lateinit var myListner: OnItemClickListner
    private var closeListner: OnItemClickListner?=null
    interface OnItemClickListner{
        fun onItemClick(position: Int)
    }
    fun setOnItemClickListner(listner: OnItemClickListner){
        myListner = listner
    }
    fun setOnItemcloseClickListner(listner: OnItemClickListner){
        closeListner = listner
    }


    class MyviewHolder(private val itemview:View, listner: OnItemClickListner, private val issaved: Boolean, listener2:OnItemClickListner?):ViewHolder(itemview) {
        var image:ShapeableImageView
        var titletex:TextView
        var durationtex:TextView
        init {
            itemView.setOnClickListener {
                listner.onItemClick(absoluteAdapterPosition)
            }
            image = itemview.findViewById(R.id.thumbnailimage)
            titletex = itemview.findViewById(R.id.title)
            durationtex = itemview.findViewById(R.id.DESC)
            if (issaved){
                val clo = itemview.findViewById<ImageButton>(R.id.close_button)
                 clo.visibility=View.VISIBLE
                clo.setOnClickListener{
                    listener2?.onItemClick(absoluteAdapterPosition)
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyviewHolder {
        val card = LayoutInflater.from(context).inflate(R.layout.eachvideothumb,parent,false)
        return MyviewHolder(card,myListner,issaved,closeListner)

    }

    override fun onBindViewHolder(holder: MyviewHolder, position: Int) {
        val s = arrayList?.get(position)?.url?.substring(29)?.replace("-"," ")
        if (s != null) {
            holder.titletex.text = buildString {
                append(s.uppercase()[0])
                append(s.substring(1))
            }
        }
        holder.durationtex.text = buildString {
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