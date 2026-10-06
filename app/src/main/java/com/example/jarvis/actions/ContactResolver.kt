package com.example.jarvis.actions

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class ContactMatch(val name: String, val number: String, val score: Int)

class ContactResolver(private val ctx: Context) {

    suspend fun find(query: String): List<ContactMatch> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return@withContext emptyList()

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val candidates = mutableListOf<ContactMatch>()
        try {
            val cursor = ctx.contentResolver.query(uri, projection, null, null, null)
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    val name = if (nameIdx != -1) c.getString(nameIdx) ?: "" else ""
                    val num = if (numIdx != -1) c.getString(numIdx) ?: "" else ""
                    if (name.isNotBlank() && num.isNotBlank()) {
                        val s = score(q, name.lowercase(Locale.ROOT))
                        if (s > 0) {
                            candidates.add(ContactMatch(name, num, s))
                        }
                    }
                }
            }
        } catch (_: SecurityException) {
            return@withContext emptyList()
        } catch (_: Exception) {
            return@withContext emptyList()
        }

        if (candidates.isEmpty()) return@withContext emptyList()

        val maxScore = candidates.maxOf { it.score }
        // De-duplicate by normalized phone number (digits only or trimmed)
        candidates.filter { it.score == maxScore }
            .distinctBy { it.number.replace(Regex("[^0-9+]"), "") }
    }

    companion object {
        fun score(q: String, name: String): Int = when {
            name == q -> 3
            name.startsWith(q) || name.split(' ').any { it == q } -> 2
            editDistance(q, name.take(q.length)) <= 2 -> 1
            else -> 0
        }

        fun editDistance(s1: String, s2: String): Int {
            val dp = IntArray(s2.length + 1) { it }
            for (i in 1..s1.length) {
                var prev = dp[0]
                dp[0] = i
                for (j in 1..s2.length) {
                    val temp = dp[j]
                    dp[j] = if (s1[i - 1] == s2[j - 1]) {
                        prev
                    } else {
                        1 + minOf(prev, dp[j], dp[j - 1])
                    }
                    prev = temp
                }
            }
            return dp[s2.length]
        }
    }
}
