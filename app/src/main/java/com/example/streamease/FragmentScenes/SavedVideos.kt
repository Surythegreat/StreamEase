package com.example.streamease.FragmentScenes

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Scene
import com.example.streamease.MainActivity2
import com.example.streamease.R
import com.example.streamease.databinding.FragmentProfileViewBinding
import com.example.streamease.databinding.FragmentSavedVideosBinding
import com.example.streamease.helper.myAdapter


class SavedVideos : scenes() {

    private lateinit var mainActivity: MainActivity2
    private lateinit var binding: FragmentSavedVideosBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavedVideosBinding.inflate(inflater, container, false)
        mainActivity =activity as MainActivity2
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onMovedto() {
        super.onMovedto()
        if(mainActivity.Savedvideos.size==0){
            binding.noVideo.visibility=View.VISIBLE
        }else{
            binding.noVideo.visibility=View.GONE
        }
        val adapter = myAdapter(mainActivity, mainActivity.Savedvideos)
        binding.recycleview.adapter = adapter
        binding.recycleview.layoutManager = LinearLayoutManager(activity)
        adapter.setOnItemClickListner(object : myAdapter.onItemClickListner {
            @OptIn(UnstableApi::class)
            override fun onItemClick(position: Int) {
                mainActivity.strartVideoScene(mainActivity.Savedvideos[position])
            }

        })
    }
}