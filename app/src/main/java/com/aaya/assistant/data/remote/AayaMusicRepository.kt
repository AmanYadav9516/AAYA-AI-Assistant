package com.aaya.assistant.data.remote

import android.util.Log
import com.aaya.assistant.data.model.SongItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AayaMusicRepository {

    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "AayaMusicRepo"

        val DEFAULT_SONGS = listOf(
            SongItem(
                id = "def_1",
                title = "Cyber Lofi Chill",
                artist = "AAYA Beats",
                category = "Lo-Fi & Chill",
                audioUrl = "https://actions.google.com/sounds/v1/weather/rain_heavy.ogg",
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500",
                duration = "03:45"
            ),
            SongItem(
                id = "def_2",
                title = "Deep Focus Study Waves",
                artist = "BrainWave Audio",
                category = "Focus & Study",
                audioUrl = "https://actions.google.com/sounds/v1/science/ambient_space.ogg",
                coverUrl = "https://images.unsplash.com/photo-1507676184212-d03ab07a01bf?w=500",
                duration = "04:12"
            ),
            SongItem(
                id = "def_3",
                title = "Peaceful Morning Flute",
                artist = "Spiritual Harmony",
                category = "Devotional / Bhakti",
                audioUrl = "https://actions.google.com/sounds/v1/water/creek_water_trickling.ogg",
                coverUrl = "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=500",
                duration = "05:00"
            )
        )
    }

    fun getSongsStream(): Flow<List<SongItem>> = callbackFlow {
        val listener = firestore.collection("songs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed, falling back to default songs", error)
                    trySend(DEFAULT_SONGS)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            SongItem(
                                id = doc.id,
                                title = doc.getString("title") ?: "Untitled",
                                artist = doc.getString("artist") ?: "AAYA Artist",
                                category = doc.getString("category") ?: "Chill",
                                audioUrl = doc.getString("audioUrl") ?: "",
                                coverUrl = doc.getString("coverUrl") ?: "",
                                duration = doc.getString("duration") ?: "03:30"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.filter { it.audioUrl.isNotBlank() }

                    trySend(if (list.isNotEmpty()) list else DEFAULT_SONGS)
                } else {
                    trySend(DEFAULT_SONGS)
                }
            }

        awaitClose { listener.remove() }
    }
}
