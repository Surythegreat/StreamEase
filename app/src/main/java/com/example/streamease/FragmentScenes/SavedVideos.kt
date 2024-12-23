package com.example.streamease.FragmentScenes

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.streamease.MainActivity2
import com.example.streamease.databinding.FragmentSavedVideosBinding
import com.example.streamease.helper.myAdapter


@UnstableApi
class SavedVideos : scenes() {

    private lateinit var mainActivity: MainActivity2
    private lateinit var binding: FragmentSavedVideosBinding

    @OptIn(UnstableApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavedVideosBinding.inflate(inflater, container, false)
        mainActivity =activity as MainActivity2
        // Inflate the layout for this fragment
        return binding.root
    }

    fun UpdateSaved() {
        if(mainActivity.Savedvideos.size==0){
            binding.noVideo.visibility=View.VISIBLE
            binding.recycleview.adapter = myAdapter(mainActivity, listOf(), true)
        }else {
            binding.noVideo.visibility = View.GONE

            val adapter = myAdapter(mainActivity, mainActivity.Savedvideos, true)
            adapter.setOnItemcloseClickListner(object : myAdapter.onItemClickListner {
                override fun onItemClick(position: Int) {
                    mainActivity.removeSavedVideo(position)
                }
            })
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
}