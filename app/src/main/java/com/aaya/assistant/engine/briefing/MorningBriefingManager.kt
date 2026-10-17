package com.aaya.assistant.engine.briefing

import android.content.Context
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.data.local.QuoteLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object MorningBriefingManager {

    suspend fun generateMorningBriefing(context: Context, isHindi: Boolean): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? AayaApplication
        val prefs = app?.preferenceManager
        val db = app?.database

        val userName = prefs?.userName?.ifBlank { "Aman" } ?: "Aman"
        val now = Calendar.getInstance()

        val dayFormat = SimpleDateFormat("EEEE", if (isHindi) Locale("hi", "IN") else Locale.US)
        val dateFormat = SimpleDateFormat("d MMMM", if (isHindi) Locale("hi", "IN") else Locale.US)

        val dayName = dayFormat.format(now.time)
        val dateName = dateFormat.format(now.time)

        // Read today's tasks
        val tasks = db?.aayaDao()?.getPendingScheduledTasks()?.firstOrNull() ?: emptyList()
        val quote = QuoteLibrary.getQuoteOfTheDay()

        val timeOfDay = when (now.get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> if (isHindi) "Shubh prabhat" else "Good morning"
            in 12..16 -> if (isHindi) "Shubh dopahar" else "Good afternoon"
            in 17..21 -> if (isHindi) "Shubh sandhya" else "Good evening"
            else -> if (isHindi) "Namaste" else "Hello"
        }

        return@withContext buildString {
            if (isHindi) {
                append("$timeOfDay, Mr. $userName! ")
                append("Aaj $dayName, $dateName hai. ")
                if (tasks.isNotEmpty()) {
                    val firstTask = tasks.first()
                    append("Aaj aapke ${tasks.size} zaroori tasks hain, pehla hai '${firstTask.title}'. ")
                } else {
                    append("Aaj aapke sabhi reminders clear hain. ")
                }
                append("Aaj ka suvichar hai: “${quote.hindiText}”. ")
                append("Aapka din mangalmay ho!")
            } else {
                append("$timeOfDay, Mr. $userName! ")
                append("Today is $dayName, $dateName. ")
                if (tasks.isNotEmpty()) {
                    val firstTask = tasks.first()
                    append("You have ${tasks.size} pending task${if (tasks.size > 1) "s" else ""}, starting with '${firstTask.title}'. ")
                } else {
                    append("All your reminders for today are clear. ")
                }
                append("Here is your daily inspiration: “${quote.englishText}”. ")
                append("Wishing you a great day ahead!")
            }
        }
    }
}
