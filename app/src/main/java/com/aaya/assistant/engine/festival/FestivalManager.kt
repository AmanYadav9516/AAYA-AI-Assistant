package com.aaya.assistant.engine.festival

import android.content.Context
import android.content.Intent
import com.aaya.assistant.data.model.FestivalModel
import java.util.Calendar

object FestivalManager {

    val indianFestivals = listOf(
        FestivalModel(
            id = "diwali",
            name = "Diwali",
            hindiName = "दीपावली",
            month = 11, // November approximate
            dayOfMonth = 1,
            isMajor = true,
            description = "Festival of Lights celebrating victory of light over darkness and hope over despair.",
            familyWish = "Wishing my beloved family endless joy, prosperity, and divine blessings on this auspicious Diwali! May Maa Lakshmi illuminate our home forever.",
            siblingWish = "Happy Diwali to my dearest partner-in-crime! May your life shine brighter than the grandest fireworks and your dreams soar high!",
            friendWish = "Happy Diwali, yaar! May this year bring you unstoppable success, endless parties, wealth, and top-tier happiness!",
            generalWishHindi = "दीपावली की हार्दिक शुभकामनाएं! आपके जीवन में सुख, समृद्धि और खुशियों का दीप सदा प्रज्वलित रहे।",
            generalWishEnglish = "Wishing you and your loved ones a joyous, sparkling, and prosperous Diwali!"
        ),
        FestivalModel(
            id = "holi",
            name = "Holi",
            hindiName = "होली",
            month = 3, // March
            dayOfMonth = 25,
            isMajor = true,
            description = "Festival of Colors celebrating love, springtime, and playful togetherness.",
            familyWish = "May the divine colors of Holi fill our family with peace, health, and endless warmth. Happy Holi to everyone!",
            siblingWish = "Happy Holi! Get ready to be soaked in colors and love. Let's make this Holi unforgettable!",
            friendWish = "Bura na mano, Holi hai! Wishing you a vibrant, energetic, and colorful Holi filled with laughter and sweet gujiyas!",
            generalWishHindi = "रंगों के पावन पर्व होली की हार्दिक शुभकामनाएं! आपका जीवन खुशियों के हर रंग से सराबोर रहे।",
            generalWishEnglish = "Wishing you a vibrant, joyful, and colorful Holi filled with laughter!"
        ),
        FestivalModel(
            id = "raksha_bandhan",
            name = "Raksha Bandhan",
            hindiName = "रक्षा बंधन",
            month = 8,
            dayOfMonth = 19,
            isMajor = true,
            description = "Sacred bond of love, protection, and eternal support between brothers and sisters.",
            familyWish = "Happy Raksha Bandhan! Celebrating the timeless bond that anchors our family together in love and care.",
            siblingWish = "To my dearest brother/sister: You are my greatest protector, best critic, and forever friend. Happy Raksha Bandhan!",
            friendWish = "Wishing everyone a joyful Raksha Bandhan filled with sweet treats and warm family memories!",
            generalWishHindi = "रक्षाबंधन के पावन पर्व की हार्दिक शुभकामनाएं! भाई-बहन का पवित्र प्रेम सदा अटूट रहे।",
            generalWishEnglish = "Happy Raksha Bandhan! Celebrating the eternal bond of love and protection."
        ),
        FestivalModel(
            id = "navratri_dussehra",
            name = "Navratri & Dussehra",
            hindiName = "नवरात्रि व दशहरा",
            month = 10,
            dayOfMonth = 12,
            isMajor = true,
            description = "Nine divine nights honoring Maa Durga followed by the triumph of good over evil.",
            familyWish = "May Maa Durga shower our family with courage, health, and peace. Happy Navratri and Vijayadashami!",
            siblingWish = "Happy Dussehra! May all obstacles vanish and your endeavors turn into triumphant victories!",
            friendWish = "Wishing you high energy garba nights and grand success on Dussehra!",
            generalWishHindi = "विजयदशमी और नवरात्रि की हार्दिक शुभकामनाएं! अधर्म पर धर्म और असत्य पर सत्य की सदा विजय हो।",
            generalWishEnglish = "May the divine blessings of Maa Durga bring courage, happiness, and prosperity to your life!"
        ),
        FestivalModel(
            id = "janmashtami",
            name = "Krishna Janmashtami",
            hindiName = "श्रीकृष्ण जन्माष्टमी",
            month = 8,
            dayOfMonth = 26,
            isMajor = true,
            description = "Celebration of the birth of Lord Krishna, embodiment of divine wisdom and love.",
            familyWish = "May Lord Krishna steal all our worries and bless our household with serenity and abundance. Happy Janmashtami!",
            siblingWish = "Happy Janmashtami! May your flute play tunes of happiness and success all year long!",
            friendWish = "Makhan chor ke aashirwad se tumhari saari wishes puri ho! Happy Janmashtami, bhai!",
            generalWishHindi = "श्रीकृष्ण जन्माष्टमी की हार्दिक बधाई! भगवान श्रीकृष्ण की कृपा आप और आपके परिवार पर सदा बनी रहे।",
            generalWishEnglish = "Wishing you a blessed and joyous Krishna Janmashtami filled with devotion and happiness!"
        ),
        FestivalModel(
            id = "makar_sankranti",
            name = "Makar Sankranti / Pongal",
            hindiName = "मकर संक्रांति",
            month = 1,
            dayOfMonth = 14,
            isMajor = true,
            description = "Harvest festival marking the sun's transition and arrival of longer spring days.",
            familyWish = "May the harvest festival bring bountiful health, sweet moments, and golden prosperity to our family!",
            siblingWish = "May your dreams soar higher than the kites in the sky! Happy Makar Sankranti!",
            friendWish = "Til-gur khaao aur meethi meethi baatein bolo! Happy Makar Sankranti and Pongal!",
            generalWishHindi = "मकर संक्रांति और पोंगल की हार्दिक शुभकामनाएं! पतंगों की तरह आपकी खुशियां गगन चूमें।",
            generalWishEnglish = "Wishing you a bright, healthy, and prosperous Makar Sankranti!"
        )
    )

    fun getUpcomingFestival(): FestivalModel {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        // Find closest festival in future
        val upcoming = indianFestivals.firstOrNull { fest ->
            fest.month > currentMonth || (fest.month == currentMonth && fest.dayOfMonth >= currentDay)
        }
        return upcoming ?: indianFestivals.first()
    }

    fun shareGreetingOnWhatsApp(context: Context, festival: FestivalModel, targetGroup: String) {
        val wish = when (targetGroup.lowercase()) {
            "family", "parents" -> festival.familyWish
            "sibling", "brother", "sister" -> festival.siblingWish
            "friend", "group" -> festival.friendWish
            else -> "${festival.generalWishHindi}\n\n${festival.generalWishEnglish}"
        }

        val message = "🌟 *Happy ${festival.name} (${festival.hindiName})!*\n\n$wish\n\n— *Sent with love via AAYA AI Assistant*"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // If WhatsApp not installed, open standard share chooser
            val chooser = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }, "Share Festive Greeting")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }

    fun shareFestivalGreeting(context: Context, festival: FestivalModel) {
        shareGreetingOnWhatsApp(context, festival, "all")
    }
}
