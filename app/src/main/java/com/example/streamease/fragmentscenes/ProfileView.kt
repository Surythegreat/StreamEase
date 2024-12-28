package com.example.streamease.fragmentscenes

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.media3.common.util.UnstableApi
import com.example.streamease.MainActivity2
import com.example.streamease.R
import com.example.streamease.databinding.FragmentProfileViewBinding
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


@UnstableApi
class ProfileView : Scenes() {
    private lateinit var binding: FragmentProfileViewBinding
    private lateinit var profileeditDialog: AlertDialog
    private lateinit var name: TextView
    private lateinit var place: TextView
    private lateinit var branch: TextView

    private lateinit var nameL: TextView
    private lateinit var placeL: TextView
    private lateinit var branchL: TextView
    private lateinit var submitButton: Button
    private var namet = ""
    private var placet = ""
    private var brancht = ""
    private lateinit var mainActivity2: MainActivity2
    override fun navid(): Int {
        return R.id.navigation_myAcc
    }

    @OptIn(UnstableApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mainActivity2=activity as MainActivity2
        binding = FragmentProfileViewBinding.inflate(inflater, container, false)
        setupProfileEditDialog()
        updateLocalData()

        nameL = binding.userNameN
        placeL = binding.userPlaceN
        branchL = binding.userBranchN
        binding.SeeSaved.setOnClickListener { mainActivity2.showSavedScene() }
        binding.SignOutButton.setOnClickListener { mainActivity2.logout() }
        binding.copyID.setOnClickListener{copyTheID()}
        binding.SearchButton.setOnClickListener{SearchId()}

        return binding.root
    }

    private fun SearchId() {
        if (binding.QueryEdit.text.isNullOrEmpty()){return}
        val idRegex = Regex("^[a-zA-Z0-9_-]{1,}$") // Alphanumeric, underscores, and hyphens
        if (!idRegex.matches(binding.QueryEdit.text.toString())) {
            Toast.makeText(mainActivity2, "Invalid User ID format", Toast.LENGTH_SHORT).show()
            return
        }
        Firebase.firestore.collection("User").document(binding.QueryEdit.text.toString()).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.userInfoSection.visibility= View.VISIBLE
                    val name = document.getString("Name") ?: "Unknown"
                    val place = document.getString("Place") ?: "Unknown"
                    val branch = document.getString("Branch") ?: "Unknown"

                    "Name: $name".also { binding.tvUserName.text = it }
                    "Place: $place".also { binding.tvUserPlace.text = it }
                    "Branch: $branch".also { binding.tvUserBranch.text = it }

                    val childFragment: Fragment = SavedVideos()
                    childFragment.arguments =Bundle().apply { putString("id",binding.QueryEdit.text.toString())
                    putBoolean("isfree",false)}
                    val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
                    transaction.replace(R.id.userHolder, childFragment).commit()
                }else{
                    Toast.makeText(mainActivity2, "NO USER FOUND", Toast.LENGTH_SHORT).show()

                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(mainActivity2, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun copyTheID(){
        val clipboard = getSystemService(mainActivity2,ClipboardManager::class.java)
        val clip = ClipData.newPlainText("ID", mainActivity2.userid)
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip)
        }

    }
    private fun setupProfileEditDialog() {
        val lay = layoutInflater.inflate(R.layout.edit_details, null)
        name = lay.findViewById(R.id.user_name)
        place = lay.findViewById(R.id.user_place)
        branch = lay.findViewById(R.id.user_branch)
        submitButton = lay.findViewById(R.id.submit_button)
        profileeditDialog = AlertDialog.Builder(activity as MainActivity2).setView(lay).create()
        binding.editButton.setOnClickListener {
            profileeditDialog.show()
            name.text = namet
            place.text = placet
            branch.text = brancht
        }
        submitButton.setOnClickListener { updateData() }
    }

    private fun updateData() {
        val namet = name.text.toString()
        val placet = place.text.toString()
        val brancht = branch.text.toString()

        val usermap = hashMapOf(
            "Name" to namet,
            "Place" to placet,
            "Branch" to brancht
        )
        if (mainActivity2.userid != null) {
            mainActivity2.db.collection("User").document(mainActivity2.userid!!).set(usermap)
                .addOnSuccessListener {
                    profileeditDialog.dismiss()
                    updateLocalData()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Failure", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateLocalData() {
        if (mainActivity2.userid != null) {
            mainActivity2.db.collection("User").document(mainActivity2.userid!!).get()
                .addOnSuccessListener { it ->
                    if (it != null) {
                        namet = it.data?.get("Name")?.toString()?:""
                        placet = it.data?.get("Place")?.toString()?:""
                        brancht = it.data?.get("Branch")?.toString()?:""

                        "Name: $namet".also { nameL.text = it }
                        "Place: $placet".also { placeL.text = it }
                        "Branch: $brancht".also { branchL.text = it }
                    }
                }
        }
    }
}
