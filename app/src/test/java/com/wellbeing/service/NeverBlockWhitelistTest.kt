package com.wellbeing.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeverBlockWhitelistTest {

    private val ownPackage = "com.wellbeing"

    @Test
    fun `whitelist - always allows own package`() {
        assertTrue(NeverBlockWhitelist.isWhitelisted(ownPackage, ownPackage))
    }

    @Test
    fun `whitelist - always allows phone dialer and emergency services`() {
        assertTrue(NeverBlockWhitelist.isWhitelisted("com.google.android.dialer", ownPackage))
        assertTrue(NeverBlockWhitelist.isWhitelisted("com.android.dialer", ownPackage))
        assertTrue(NeverBlockWhitelist.isWhitelisted("com.android.emergency", ownPackage))
    }

    @Test
    fun `whitelist - always allows settings and system UI`() {
        assertTrue(NeverBlockWhitelist.isWhitelisted("com.android.settings", ownPackage))
        assertTrue(NeverBlockWhitelist.isWhitelisted("com.android.systemui", ownPackage))
    }

    @Test
    fun `whitelist - disallows regular user apps`() {
        assertFalse(NeverBlockWhitelist.isWhitelisted("com.instagram.android", ownPackage))
        assertFalse(NeverBlockWhitelist.isWhitelisted("com.zhiliaoapp.musically", ownPackage))
        assertFalse(NeverBlockWhitelist.isWhitelisted("com.google.android.youtube", ownPackage))
    }
}
