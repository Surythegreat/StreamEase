package com.example.streamease.fragmentscenes

import android.os.Bundle

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.streamease.MainActivity2
import com.example.streamease.R
import com.example.streamease.databinding.FragmentSavedVideosBinding
import com.example.streamease.helper.MyAdapter
import com.example.streamease.models.Video
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch


@UnstableApi
class SavedVideos : Scenes() {

    private lateinit var mainActivity: MainActivity2
    private lateinit var binding: FragmentSavedVideosBinding

    lateinit var savedvideos: MutableList<Video>
    private var userid:String?=null
    private var isFree:Boolean=true

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
        savedvideos = mutableListOf()
        userid = arguments?.getString("id")
        isFree= arguments?.getBoolean("isfree") ?:false
        fetchVideos( )
        // Inflate the layout for this fragment
        return binding.root
    }
    fun removeSavedVideo(position: Int) {
        if (position in savedvideos.indices) {
            val video = savedvideos[position]
            Firebase.firestore.collection("User").document(userid!!).collection("SAVED")
                .document(video.id.toString()).delete()
                .addOnSuccessListener {

                    savedvideos.removeAt(position)
                    updateSaved()
                    // Refresh the saved scene to reflect changes
                    Toast.makeText(mainActivity, "Video Removed", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(mainActivity, "Failed to remove video", Toast.LENGTH_SHORT).show()
                }
        }
    }
    private fun fetchVideos() {
        userid?.let { userId ->
            Firebase.firestore.collection("User").document(userId).collection("SAVED").get()
                .addOnSuccessListener { documents ->
                    if (!(documents.isEmpty)) {
                        lifecycleScope.launch {
                            val videoIds = documents.mapNotNull { it.getLong("videoId")?.toInt() }
                            val videos = videoIds.map { async { mainActivity.getVideoById(it) } }.awaitAll()
                            savedvideos.addAll(videos.filterNotNull())
                            updateSaved()
                        }
                    } else updateSaved()
                }
        }
    }
    private var isup=false
    fun updateSaved() {
        if (isup)return
        isup=true
        if(savedvideos.size==0){
            binding.noVideo.visibility=View.VISIBLE
            binding.recycleview.adapter = MyAdapter(mainActivity, listOf(), true)
        }else {
            binding.noVideo.visibility = View.GONE

            val adapter = MyAdapter(mainActivity,savedvideos,isFree )
            adapter.setOnItemcloseClickListner(object : MyAdapter.OnItemClickListner {
                override fun onItemClick(position: Int) {
                    removeSavedVideo(position)
                }
            })
            binding.recycleview.adapter = adapter
            binding.recycleview.layoutManager = LinearLayoutManager(activity)
            adapter.setOnItemClickListner(object : MyAdapter.OnItemClickListner {
                @OptIn(UnstableApi::class)
                override fun onItemClick(position: Int) {
                    mainActivity.strartVideoScene(savedvideos[position])
                }

            })
        }
        isup=false
    }
    private var updating = false
    fun saveCurrentVideo(currentvideo: Video) {
        if (!savedvideos.contains(currentvideo) && !updating) {
            currentvideo.let {
                updating = true
                val videoId = it.id

                // Optimistically add video and update UI
                savedvideos.add(it)
                updateSaved()

                Firebase.firestore.collection("User").document(userid!!).collection("SAVED")
                    .document(videoId.toString())
                    .set(mapOf("videoId" to videoId))
                    .addOnSuccessListener {
                        Toast.makeText(mainActivity, "Video Saved", Toast.LENGTH_SHORT).show()

                        mainActivity.onVideoSaved(currentvideo)
                        updating = false
                    }
                    .addOnFailureListener {
                        // Revert optimistic changes
                        savedvideos.remove(currentvideo)
                        updateSaved()
                        mainActivity.onVideoRemoved(currentvideo)
                        Toast.makeText(mainActivity, "Failed to save video", Toast.LENGTH_SHORT).show()
                        updating = false
                    }
            }
        } else if (!updating) {
            mainActivity.onVideoSaved(currentvideo)
        }
    }


    fun removeSavedVideoID(video: Video) {
        if (savedvideos.contains(video)) {
            // Optimistically remove from list and update UI
            savedvideos.remove(video)
            updateSaved()


            Firebase.firestore.collection("User").document(userid!!).collection("SAVED")
                .document(video.id.toString()).delete()
                .addOnSuccessListener {
                    Toast.makeText(mainActivity, "Video Removed", Toast.LENGTH_SHORT).show()
                    mainActivity.onVideoRemoved(video)
                }
                .addOnFailureListener {
                    // Revert removal if delete failed
                    savedvideos.add(video)
                    updateSaved()
                    mainActivity.onVideoSaved(video)
                    Toast.makeText(mainActivity, "Failed to remove video", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(mainActivity, "Video not found in saved list", Toast.LENGTH_SHORT).show()
        }
    }

}