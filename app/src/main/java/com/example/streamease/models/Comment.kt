package com.example.streamease.models

data class Comment(
    val userId: String = "",
    val userName: String = "",
    val commentText: String = "",
    val timestamp: String = "",
    var documentId: String? = null
)