package com.example.streamease.FragmentScenes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.streamease.databinding.FragmentProfileViewBinding

class profileView : scenes() {
    private lateinit var binding:FragmentProfileViewBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileViewBinding.inflate(inflater,container,false)

        return binding.root
    }


}