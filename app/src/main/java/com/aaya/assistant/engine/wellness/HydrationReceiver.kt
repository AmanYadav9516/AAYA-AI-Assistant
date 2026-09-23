package com.aaya.assistant.engine.wellness

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aaya.assistant.AayaApplication
import com.aaya.assistant.engine.web.WebIntelligenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

class HydrationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = AayaApplication.instance.preferenceManager
        if (!prefs.isWaterReminderEnabled) return

        val userName = prefs.userName.ifBlank { "there" }
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        // Asynchronous processing using goAsync()
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (hour) {
                    in 6..9 -> {
                        // Morning Briefing & Weather Wakeup
                        val weatherSummary = WebIntelligenceEngine.getInstantAnswer("weather") ?: ""
                        val weatherText = if (weatherSummary.isNotBlank()) " $weatherSummary" else ""
                        AayaApplication.instance.showSystemNotification(
                            title = "🌅 Good Morning, $userName!",
                            message = "A fresh day begins!$weatherText Start your morning with a warm glass of water to energize your mind."
                        )
                    }
                    in 20..22 -> {
                        // Evening Wind-Down
                        AayaApplication.instance.showSystemNotification(
                            title = "🌙 Good Evening, $userName",
                            message = "You did great today! Take a deep breath, drink some water, and relax your eyes away from bright screens."
                        )
                    }
                    in 10..19 -> {
                        // Dynamic Non-Repetitive Daytime Caring Prompts
                        val (title, message) = getRandomHydrationPrompt(userName)
                        AayaApplication.instance.showSystemNotification(
                            title = title,
                            message = message
                        )
                    }
                }
            } catch (e: Exception) {
                // Ignore failure
            } finally {
                pendingResult.finish()
            }
        }

        // Schedule next reminder in 2 hours
        scheduleNext(context)
    }

    private fun getRandomHydrationPrompt(userName: String): Pair<String, String> {
        val prompts = listOf(
            "💧 Hydro Check, $userName!" to "Paani ka ek glass pee lo! Staying hydrated improves focus and keeps your energy high.",
            "🧠 Brain Boost Time, $userName" to "Even mild dehydration drops productivity. Grab your water bottle and take a refreshing sip!",
            "✨ Quick Sip Reminder, $userName" to "Kaam ke beech me paani peena mat bhoolna. Ek glass paani aur 30 second ki walk ho jaye!",
            "🥤 Stay Fresh, $userName!" to "Water break! Your body and mind will thank you. Sip a little water right now.",
            "🌟 Wellness Alert, $userName" to "Health is priority #1! Ek ghoont paani aur lambi saans lo. You're doing amazing.",
            "💧 Hydration Check, $userName" to "It's been a while since your last glass of water. Keep your hydration streak going!",
            "🧘 2-Minute Rest & Sip" to "Screen se thodi der nazar hataiye, stretch kijiye aur paani pijiye $userName!",
            "🌊 Refresh Yourself, $userName" to "Your brain is 75% water. Fuel it up with a fresh drink right now.",
            "⚡ Recharge Station" to "Energy dipping? Sometimes all you need is a cool glass of water. Drink up, $userName!",
            "💙 Care from AAYA" to "AAYA cares for your health $userName. Please drink a glass of water right away."
        )
        return prompts[Random.nextInt(prompts.size)]
    }

    companion object {
        fun scheduleNext(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HydrationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9991,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val nextTrigger = System.currentTimeMillis() + (2 * 60 * 60 * 1000L) // 2 hours
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            }
        }
    }
}
