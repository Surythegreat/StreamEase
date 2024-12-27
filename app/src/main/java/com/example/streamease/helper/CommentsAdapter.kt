package com.example.streamease.helper

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.streamease.R
import com.example.streamease.models.Comment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class CommentsAdapter(private val comments: List<Comment>,val listner: OnItemClickListner) :
    RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    class CommentViewHolder(itemView: View,listner: OnItemClickListner) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val commentTextView: TextView = itemView.findViewById(R.id.commentTextView)
        val timetextview: TextView = itemView.findViewById(R.id.time)
        val closeButton:ImageButton = itemView.findViewById(R.id.commentClose)
        init {
            closeButton.setOnClickListener{listner.onItemClick(absoluteAdapterPosition)}
        }
    }


    interface OnItemClickListner{
        fun onItemClick(position: Int)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view, listner )
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.userNameTextView.text = comment.userName
        holder.commentTextView.text = comment.commentText
        holder.timetextview.text = comment.timestamp
        holder.closeButton.visibility = if((FirebaseAuth.getInstance().currentUser?.uid
                ?: 0) == comment.userId
        ){
            View.VISIBLE
        }else{
            View.GONE
        }
    }

    override fun getItemCount(): Int = comments.size
}
