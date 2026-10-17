package com.aaya.assistant.engine.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.aaya.assistant.data.model.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * High-performance scanner for on-device local audio files.
 * Supports: MP3, MPEG, M4A, AAC, WAV, FLAC, OGG, and OPUS.
 */
object LocalMusicScanner {

    private const val TAG = "LocalMusicScanner"

    // Supported audio extensions as requested by user
    private val SUPPORTED_EXTENSIONS = setOf(
        "mp3", "mpeg", "mpg", "mp4a", "m4a", "aac", "wav", "flac", "ogg", "opus"
    )

    suspend fun scanLocalAudioFiles(context: Context): List<SongItem> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<SongItem>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val durationMs = c.getLong(durationCol)
                    val albumId = c.getLong(albumIdCol)
                    val displayName = c.getString(nameCol) ?: ""

                    // Validate file extension (supports MP3, MPEG, M4A, WAV, etc.)
                    val ext = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    if (ext.isNotEmpty() && !SUPPORTED_EXTENSIONS.contains(ext)) {
                        continue
                    }

                    // Ignore ringtones or tiny voice recordings under 15 seconds
                    if (durationMs < 15000L) {
                        continue
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val albumArtUri = "content://media/external/audio/albumart/$albumId"

                    val minutes = (durationMs / 1000) / 60
                    val seconds = (durationMs / 1000) % 60
                    val formattedDuration = String.format(Locale.US, "%02d:%02d", minutes, seconds)

                    songList.add(
                        SongItem(
                            id = "local_$id",
                            title = title,
                            artist = if (artist.contains("<unknown>", ignoreCase = true)) "Device Audio" else artist,
                            category = "Local Device",
                            audioUrl = contentUri,
                            coverUrl = albumArtUri,
                            duration = formattedDuration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning local audio library", e)
        }

        return@withContext songList
    }
}
