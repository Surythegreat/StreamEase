package com.example.streamease.fragmentscenes

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.streamease.MainActivity2
import com.example.streamease.R
import com.example.streamease.databinding.FragmentSavedVideosBinding
import com.example.streamease.helper.MyAdapter


@UnstableApi
class SavedVideos : Scenes() {

    private lateinit var mainActivity: MainActivity2
    private lateinit var binding: FragmentSavedVideosBinding

    override fun navid(): Int {
        return R.id.navigation_hisNsavV
    }

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

    fun updateSaved() {
        if(mainActivity.savedvideos.size==0){
            binding.noVideo.visibility=View.VISIBLE
            binding.recycleview.adapter = MyAdapter(mainActivity, listOf(), true)
        }else {
            binding.noVideo.visibility = View.GONE

            val adapter = MyAdapter(mainActivity, mainActivity.savedvideos, true)
            adapter.setOnItemcloseClickListner(object : MyAdapter.OnItemClickListner {
                override fun onItemClick(position: Int) {
                    mainActivity.removeSavedVideo(position)
                }
            })
            binding.recycleview.adapter = adapter
            binding.recycleview.layoutManager = LinearLayoutManager(activity)
            adapter.setOnItemClickListner(object : MyAdapter.OnItemClickListner {
                @OptIn(UnstableApi::class)
                override fun onItemClick(position: Int) {
                    mainActivity.strartVideoScene(mainActivity.savedvideos[position])
                }

            })
        }
    }
}