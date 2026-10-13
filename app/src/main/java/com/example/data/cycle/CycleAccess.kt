package com.example.data.cycle

/**
 * Who can use the cycle tracker. It is offered only to profiles set to Female and only after the person switched it on and agreed
 * on the consent screen. The gender field is a gate for the screens, not a security control.
 */
object CycleAccess {
    fun isFemale(gender: String?): Boolean = gender?.trim()?.equals("female", ignoreCase = true) == true

    /** The Settings entry, cards and shortcuts are shown. */
    fun isAvailable(gender: String?): Boolean = isFemale(gender)

    /** Tracking is on: reminders, cards, cloud sync and AI use all require this. */
    fun isActive(gender: String?, enabled: Boolean, consentAt: Long): Boolean =
        isFemale(gender) && enabled && consentAt > 0L
}
