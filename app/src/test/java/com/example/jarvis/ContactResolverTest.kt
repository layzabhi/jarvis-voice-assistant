package com.example.jarvis

import com.example.jarvis.actions.ContactResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactResolverTest {

    @Test
    fun testEditDistance() {
        assertEquals(0, ContactResolver.editDistance("rahul", "rahul"))
        assertEquals(1, ContactResolver.editDistance("rahul", "rahil"))
        assertEquals(2, ContactResolver.editDistance("rahul", "rohil"))
        assertEquals(3, ContactResolver.editDistance("kitten", "sitting"))
    }

    @Test
    fun testScoring() {
        // Exact match -> score 3
        assertEquals(3, ContactResolver.score("rahul", "rahul"))

        // Prefix match or contains word -> score 2
        assertEquals(2, ContactResolver.score("rahul", "rahul sharma"))
        assertEquals(2, ContactResolver.score("sharma", "rahul sharma"))

        // Edit distance <= 2 on leading characters -> score 1
        assertEquals(1, ContactResolver.score("rahil", "rahul"))

        // Non-match -> score 0
        assertEquals(0, ContactResolver.score("john", "amit verma"))
    }
}
