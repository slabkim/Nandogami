package com.example.nandogami.model
import com.google.firebase.firestore.DocumentId

<<<<<<< Updated upstream
class User {
}
=======
data class User(
    val uid: String = "",
    val username: String = "",
    val handle: String = "",
    val bio: String = "",
    val email: String = "",
    val photoUrl: String = "", // URL foto profil user
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val readingCount: Int = 0,
    val completedCount: Int = 0,
    val planToReadCount: Int = 0,
    val isPrivate: Boolean = false, // Apakah profil private
    val joinDate: Long = 0,
    val lastActive: Long = 0
)
>>>>>>> Stashed changes
