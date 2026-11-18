package com.example.ailearning

import com.example.ailearning.domain.usecase.FocusLockPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusLockPolicyTest {

    private val blocked = setOf("com.instagram.android", "com.twitter.android")
    private val own = "com.example.ailearning"

    // ---------------------------------------------------------- shouldShowGate

    @Test
    fun gateShowsWhenBlockedAppForegroundAndNotUnlocked() {
        val result = FocusLockPolicy.shouldShowGate(
            focusEnabled = true,
            blockedPackages = blocked,
            packageName = "com.instagram.android",
            keyguardLocked = false,
            unlockSessionActive = false,
            isOwnPackage = false
        )
        assertTrue(result)
    }

    @Test
    fun gateHiddenWhenDisabled() {
        val result = FocusLockPolicy.shouldShowGate(
            focusEnabled = false,
            blockedPackages = blocked,
            packageName = "com.instagram.android",
            keyguardLocked = false,
            unlockSessionActive = false,
            isOwnPackage = false
        )
        assertFalse(result)
    }

    @Test
    fun gateHiddenWhenKeyguardLocked() {
        val result = FocusLockPolicy.shouldShowGate(
            focusEnabled = true,
            blockedPackages = blocked,
            packageName = "com.instagram.android",
            keyguardLocked = true,
            unlockSessionActive = false,
            isOwnPackage = false
        )
        assertFalse(result)
    }

    @Test
    fun gateHiddenWhenUnlockSessionActive() {
        val result = FocusLockPolicy.shouldShowGate(
            focusEnabled = true,
            blockedPackages = blocked,
            packageName = "com.instagram.android",
            keyguardLocked = false,
            unlockSessionActive = true,
            isOwnPackage = false
        )
        assertFalse(result)
    }

    @Test
    fun gateHiddenForOwnPackage() {
        val result = FocusLockPolicy.shouldShowGate(
            focusEnabled = true,
            blockedPackages = blocked + own,
            packageName = own,
            keyguardLocked = false,
            unlockSessionActive = false,
            isOwnPackage = true
        )
        assertFalse(result)
    }

    @Test
    fun gateHiddenForNonBlockedApp() {
        val result = FocusLockPolicy.shouldShowGate(
            focusEnabled = true,
            blockedPackages = blocked,
            packageName = "com.example.somegame",
            keyguardLocked = false,
            unlockSessionActive = false,
            isOwnPackage = false
        )
        assertFalse(result)
    }

    // ---------------------------------------------------------- lock / unlock

    @Test
    fun lockResetsSessionWhenKeyguardShows() {
        assertTrue(FocusLockPolicy.shouldLock(keyguardLocked = true))
        assertFalse(FocusLockPolicy.shouldLock(keyguardLocked = false))
    }

    @Test
    fun unlockTransitionDetected() {
        assertTrue(FocusLockPolicy.isUnlockTransition(wasKeyguardLocked = true, isKeyguardLocked = false))
        assertFalse(FocusLockPolicy.isUnlockTransition(wasKeyguardLocked = false, isKeyguardLocked = false))
        assertFalse(FocusLockPolicy.isUnlockTransition(wasKeyguardLocked = false, isKeyguardLocked = true))
        assertFalse(FocusLockPolicy.isUnlockTransition(wasKeyguardLocked = true, isKeyguardLocked = true))
    }

    // ---------------------------------------------------------- protected packages

    @Test
    fun protectedPackagesCannotBeBlocked() {
        assertTrue(FocusLockPolicy.isProtectedPackage("com.android.settings", own))
        assertTrue(FocusLockPolicy.isProtectedPackage("com.android.systemui", own))
        assertTrue(FocusLockPolicy.isProtectedPackage("com.google.android.dialer", own))
        assertTrue(FocusLockPolicy.isProtectedPackage("com.samsung.android.dialer", own))
        assertTrue(FocusLockPolicy.isProtectedPackage(own, own))
    }

    @Test
    fun normalAppsAreNotProtected() {
        assertFalse(FocusLockPolicy.isProtectedPackage("com.instagram.android", own))
        assertFalse(FocusLockPolicy.isProtectedPackage("jp.naver.line.android", own))
    }

    // ---------------------------------------------------------- pickNext

    @Test
    fun pickNextAvoidsRecentlyShown() {
        val a = "q1"; val b = "q2"; val c = "q3"
        val next = FocusLockPolicy.pickNext(listOf(a, b, c), setOf(a, b)) { it }
        assertEquals("q3", next)
    }

    @Test
    fun pickNextFallsBackToFirstWhenAllShown() {
        val next = FocusLockPolicy.pickNext(listOf("q1", "q2"), setOf("q1", "q2")) { it }
        assertEquals("q1", next)
    }

    @Test
    fun pickNextReturnsNullWhenEmpty() {
        val next = FocusLockPolicy.pickNext(listOf<String>(), emptySet()) { it }
        assertEquals(null, next)
    }
}
