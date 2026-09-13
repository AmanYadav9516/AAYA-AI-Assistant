package com.aaya.assistant.engine.contacts

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import com.aaya.assistant.data.local.AayaDatabase

data class ContactMatchResult(
    val contactName: String,
    val phoneNumber: String,
    val confidence: Float,
    val relationshipMatched: String? = null,
    val needsConfirmation: Boolean = false,
    val alternativeMatches: List<String> = emptyList()
)

class MultilingualContactMatcher(private val context: Context) {

    private val relationshipAliases = mapOf(
        "mother" to listOf("mummy", "maa", "mom", "mama", "mother", "ammi", "mataji"),
        "father" to listOf("papa", "dad", "daddy", "father", "abbu", "pitaji"),
        "brother" to listOf("bhai", "bhaiya", "bro", "brother", "bhaijaan"),
        "sister" to listOf("didi", "behan", "sis", "sister", "aapi"),
        "spouse" to listOf("wife", "husband", "jaan")
    )

    suspend fun resolveAndFindContact(spokenTarget: String): ContactMatchResult? {
        val cleanedTarget = extractTargetName(spokenTarget).lowercase().trim()
        if (cleanedTarget.isEmpty()) return null

        val db = AayaDatabase.getInstance(context)

        // 1. Check custom relationship memory in Room DB
        val memoryRelation = db.aayaDao().getMemoryByKey(cleanedTarget)
        if (memoryRelation != null) {
            val phone = findPhoneNumberByName(memoryRelation.value)
            if (phone != null) {
                return ContactMatchResult(
                    contactName = memoryRelation.value,
                    phoneNumber = phone,
                    confidence = 1.0f,
                    relationshipMatched = cleanedTarget
                )
            }
        }

        // 2. Check VIP Contacts table
        val vipList = db.aayaDao().getAllVipContacts()
        // Check direct alias mapping
        var matchedRelationKey: String? = null
        val aliasSearchKeys = mutableListOf(cleanedTarget)

        for ((relationKey, aliases) in relationshipAliases) {
            if (aliases.any { it.equals(cleanedTarget, ignoreCase = true) || cleanedTarget.contains(it) }) {
                matchedRelationKey = relationKey
                aliasSearchKeys.addAll(aliases)
                break
            }
        }

        // 3. Search Android Phone Contacts Provider
        val allContacts = queryPhoneContacts()
        if (allContacts.isEmpty()) return null

        // Pass 1: Exact matches against target or aliases
        for (searchKey in aliasSearchKeys) {
            val exact = allContacts.firstOrNull { it.first.equals(searchKey, ignoreCase = true) }
            if (exact != null) {
                return ContactMatchResult(
                    contactName = exact.first,
                    phoneNumber = exact.second,
                    confidence = 0.98f,
                    relationshipMatched = matchedRelationKey
                )
            }
        }

        // Pass 2: Starts with or Contains match
        val partialMatches = allContacts.filter {
            aliasSearchKeys.any { key -> it.first.contains(key, ignoreCase = true) }
        }

        if (partialMatches.size == 1) {
            val single = partialMatches.first()
            return ContactMatchResult(
                contactName = single.first,
                phoneNumber = single.second,
                confidence = 0.92f,
                relationshipMatched = matchedRelationKey,
                needsConfirmation = true
            )
        } else if (partialMatches.size > 1) {
            val first = partialMatches.first()
            return ContactMatchResult(
                contactName = first.first,
                phoneNumber = first.second,
                confidence = 0.75f,
                relationshipMatched = matchedRelationKey,
                needsConfirmation = true,
                alternativeMatches = partialMatches.take(3).map { it.first }
            )
        }

        // Pass 3: Fuzzy Levenshtein Distance
        var closestContact: Pair<String, String>? = null
        var bestScore = 0.0f

        for (contact in allContacts) {
            for (key in aliasSearchKeys) {
                val score = calculateSimilarity(key, contact.first.lowercase())
                if (score > bestScore && score >= 0.70f) {
                    bestScore = score
                    closestContact = contact
                }
            }
        }

        return closestContact?.let {
            ContactMatchResult(
                contactName = it.first,
                phoneNumber = it.second,
                confidence = bestScore,
                relationshipMatched = matchedRelationKey,
                needsConfirmation = bestScore < 0.95f
            )
        }
    }

    /**
     * Strips filler words from Hindi, Hinglish, and English voice commands
     * e.g. "Mummy ko call karo" -> "Mummy"
     * e.g. "Papa ko phone lagao" -> "Papa"
     * e.g. "Call my mother please" -> "Mother"
     */
    fun extractTargetName(rawSpokenText: String): String {
        var text = rawSpokenText.lowercase()

        // Strip Hindi/Hinglish action suffixes
        val hindiSuffixes = listOf(
            "ko phone lagao", "ko phone karo", "ko call karo", "ko call kar",
            "ko call lagao", "ko call ghumao", "ko bolo", "ko message karo"
        )
        for (suffix in hindiSuffixes) {
            if (text.contains(suffix)) {
                text = text.replace(suffix, "")
            }
        }

        // Strip English command prefixes
        val englishPrefixes = listOf(
            "call to", "call my", "call", "dial", "phone", "ring", "please call"
        )
        for (prefix in englishPrefixes) {
            if (text.startsWith(prefix)) {
                text = text.removePrefix(prefix)
            }
        }

        return text.trim()
    }

    private fun findPhoneNumberByName(name: String): String? {
        val contacts = queryPhoneContacts()
        return contacts.firstOrNull { it.first.equals(name, ignoreCase = true) }?.second
    }

    private fun queryPhoneContacts(): List<Pair<String, String>> {
        val contactList = mutableListOf<Pair<String, String>>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: continue
                    val number = it.getString(numberIndex)?.replace("\\s+".toRegex(), "") ?: continue
                    contactList.add(name to number)
                }
            }
        } catch (e: SecurityException) {
            // Permission not yet granted
        }
        return contactList
    }

    private fun calculateSimilarity(s1: String, s2: String): Float {
        val distance = levenshteinDistance(s1, s2)
        val maxLen = maxOf(s1.length, s2.length)
        return if (maxLen == 0) 1.0f else (1.0f - distance.toFloat() / maxLen.toFloat())
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = Array(lhsLength + 1) { it }
        var newCost = Array(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}
