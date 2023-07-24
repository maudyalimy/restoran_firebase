package com.pnj.restoran_firebase.chat

import com.google.firebase.firestore.Exclude

data class Chat(val username: String? = null,
                val massage: String? = null,
                val time: String? = null) {
    @Exclude
    fun getMap(): Map<String, Any?> {
        return mapOf(
            "username" to username,
            "massage" to massage,
            "time" to time
        )
    }
}
