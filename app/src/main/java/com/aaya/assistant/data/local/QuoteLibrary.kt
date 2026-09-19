package com.aaya.assistant.data.local

import java.util.Calendar

data class Quote(
    val englishText: String,
    val hindiText: String,
    val author: String
)

object QuoteLibrary {

    val quotes = listOf(
        Quote(
            englishText = "If you want to shine like a sun, first burn like a sun.",
            hindiText = "अगर सूरज की तरह चमकना चाहते हो, तो पहले सूरज की तरह जलना सीखो।",
            author = "Dr. A.P.J. Abdul Kalam"
        ),
        Quote(
            englishText = "Arise, awake, and stop not till the goal is reached.",
            hindiText = "उठो, जागो और तब तक मत रुको जब तक लक्ष्य प्राप्त न हो जाए।",
            author = "Swami Vivekananda"
        ),
        Quote(
            englishText = "You have the right to work, but never to the fruit of work.",
            hindiText = "कर्मण्येवाधिकारस्ते मा फलेषु कदाचन। (कर्म पर तुम्हारा अधिकार है, फल पर नहीं।)",
            author = "Bhagavad Gita"
        ),
        Quote(
            englishText = "The only way to do great work is to love what you do.",
            hindiText = "महान कार्य करने का एकमात्र तरीका यह है कि आप जो करते हैं उससे प्यार करें।",
            author = "Steve Jobs"
        ),
        Quote(
            englishText = "In the middle of difficulty lies opportunity.",
            hindiText = "कठिनाइयों के बीच ही अवसर छिपे होते हैं।",
            author = "Albert Einstein"
        ),
        Quote(
            englishText = "Live as if you were to die tomorrow. Learn as if you were to live forever.",
            hindiText = "ऐसे जियो जैसे कि तुम कल मरने वाले हो। ऐसे सीखो जैसे कि तुम हमेशा जीने वाले हो।",
            author = "Mahatma Gandhi"
        ),
        Quote(
            englishText = "Failure will never overtake me if my determination to succeed is strong enough.",
            hindiText = "असफलता मुझे कभी पछाड़ नहीं सकती यदि सफल होने का मेरा संकल्प काफी मजबूत है।",
            author = "Dr. A.P.J. Abdul Kalam"
        ),
        Quote(
            englishText = "Believe in yourself and all that you are.",
            hindiText = "खुद पर विश्वास रखो और अपनी शक्तियों को पहचानो।",
            author = "Swami Vivekananda"
        ),
        Quote(
            englishText = "Change is the law of the universe. What you think of as death is indeed life.",
            hindiText = "परिवर्तन ही संसार का नियम है। जिसे तुम मृत्यु समझते हो, वही तो जीवन है।",
            author = "Bhagavad Gita"
        ),
        Quote(
            englishText = "Your time is limited, so don't waste it living someone else's life.",
            hindiText = "आपका समय सीमित है, इसलिए इसे किसी और की ज़िंदगी जी कर बर्बाद न करें।",
            author = "Steve Jobs"
        ),
        Quote(
            englishText = "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            hindiText = "सफलता अंतिम नहीं है, असफलता घातक नहीं है: आगे बढ़ते रहने का साहस ही मायने रखता है।",
            author = "Winston Churchill"
        ),
        Quote(
            englishText = "Dream, dream, dream. Dreams transform into thoughts and thoughts result in action.",
            hindiText = "सपने, सपने, सपने। सपने विचारों में बदलते हैं और विचार क्रियान्वित होते हैं।",
            author = "Dr. A.P.J. Abdul Kalam"
        )
    )

    fun getQuoteOfTheDay(): Quote {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return quotes[dayOfYear % quotes.size]
    }

    fun getRandomQuote(): Quote {
        return quotes.random()
    }
}
