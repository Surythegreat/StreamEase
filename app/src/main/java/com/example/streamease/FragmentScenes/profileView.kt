package com.example.streamease.FragmentScenes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.streamease.MainActivity2
import com.example.streamease.R
import com.example.streamease.databinding.FragmentProfileViewBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class profileView : scenes() {
    private lateinit var binding: FragmentProfileViewBinding
    private lateinit var profileeditDialog: AlertDialog
    private lateinit var name: TextView
    private lateinit var place: TextView
    private lateinit var branch: TextView

    private lateinit var nameL: TextView
    private lateinit var placeL: TextView
    private lateinit var branchL: TextView
    private lateinit var SubmitButton: Button
    private var namet = ""
    private var placet = ""
    private var brancht = ""

    val userid = FirebaseAuth.getInstance().currentUser?.uid
    private var db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileViewBinding.inflate(inflater, container, false)
        SetupProfileEditDialog()
        UpdateLocalData()
        nameL = binding.userNameN
        placeL = binding.userPlaceN
        branchL = binding.userBranchN

        return binding.root
    }

    private fun SetupProfileEditDialog() {
        val lay = layoutInflater.inflate(R.layout.edit_details, null)
        name = lay.findViewById(R.id.user_name)
        place = lay.findViewById(R.id.user_place)
        branch = lay.findViewById(R.id.user_branch)
        SubmitButton = lay.findViewById(R.id.submit_button)
        profileeditDialog = AlertDialog.Builder(activity as MainActivity2).setView(lay).create()
        binding.editButton.setOnClickListener {
            profileeditDialog.show()
            name.text = namet
            place.text = placet
            branch.text = brancht
        }
        SubmitButton.setOnClickListener { UpdateData() }
    }

    private fun UpdateData() {
        val namet = name.text.toString()
        val placet = place.text.toString()
        val brancht = branch.text.toString()

        val usermap = hashMapOf(
            "Name" to namet,
            "Place" to placet,
            "Branch" to brancht
        )
        if (userid != null) {
            db.collection("User").document(userid).set(usermap)
                .addOnSuccessListener {
                    profileeditDialog.dismiss()
                    UpdateLocalData()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Failure", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun UpdateLocalData() {
        if (userid != null) {
            db.collection("User").document(userid).get()
                .addOnSuccessListener {
                    if (it != null) {
                        namet = it.data?.get("Name")?.toString().toString()
                        placet = it.data?.get("Place")?.toString().toString()
                        brancht = it.data?.get("Branch")?.toString().toString()

                        nameL.text = "Name: $namet"
                        placeL.text = "Place: $placet"
                        branchL.text = "Branch: $brancht"
                    }
                }
        }
    }
}
