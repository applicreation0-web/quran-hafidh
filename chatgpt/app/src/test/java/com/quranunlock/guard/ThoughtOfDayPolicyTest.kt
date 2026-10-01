package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ThoughtOfDayPolicyTest {
    private val hadith = DailyReminder(
        id = "h1",
        type = ReminderType.HADITH,
        theme = "douceur",
        arabicText = "ع",
        frenchText = "f1",
        author = "a",
        book = "b",
        reference = "r",
        authenticity = "Sahih",
        tags = setOf("douceur")
    )
    private val hikma = DailyReminder(
        id = "h2",
        type = ReminderType.HIKAM,
        theme = "sincérité",
        arabicText = "ع",
        frenchText = "f2",
        author = "a",
        book = "b",
        reference = "r",
        authenticity = null,
        tags = setOf("sincérité")
    )

    @Test
    fun thoughtIsStableForSameDateAndSameCorpus() {
        val date = LocalDate.of(2026, 9, 1)
        val epoch = date.toEpochDay()
        val all = listOf(hadith, hikma)

        val first = DailyReminderManager.chooseReminderFromList(
            all = all,
            epochDay = epoch,
            theme = "douceur",
            desiredType = ReminderType.HADITH,
            recentIds = emptySet()
        )
        val second = DailyReminderManager.chooseReminderFromList(
            all = all,
            epochDay = epoch,
            theme = "douceur",
            desiredType = ReminderType.HADITH,
            recentIds = emptySet()
        )

        assertEquals(first.id, second.id)
    }

    @Test
    fun onlyOneThoughtNotificationIsAllowedPerEpochDay() {
        val today = LocalDate.of(2026, 9, 1).toEpochDay()
        assertTrue(ThoughtOfDayPolicy.shouldNotify(Long.MIN_VALUE, today))
        assertFalse(ThoughtOfDayPolicy.shouldNotify(today, today))
        assertTrue(ThoughtOfDayPolicy.shouldNotify(today, today + 1))
    }

    @Test
    fun notificationOpensTheDedicatedFullThoughtCard() {
        assertEquals(
            "ThoughtOfDayActivity",
            ThoughtOfDayPolicy.FULL_CARD_ACTIVITY_SIMPLE_NAME
        )
    }

    @Test
    fun thoughtNotificationIsScheduledAtTwentyLocal() {
        assertEquals(20, ThoughtOfDayPolicy.REMINDER_HOUR)
        assertEquals(8200, ThoughtOfDayPolicy.NOTIFICATION_ID)
    }
}
