package com.ochat.android.ui.home

/**
 * Animation timings, kept in one place so motion stays consistent and is easy to tune down.
 *
 * These are deliberately short. Motion here exists to explain a change - what appeared, what
 * moved, where it went - not to decorate. Anything longer than about a quarter of a second
 * starts to feel like waiting rather than responding, and on a low-end device a long
 * animation is a long time spent compositing frames.
 *
 * Everything animated in OChat is a cheap property: alpha, scale, translation. No blurs, no
 * shadow animation, no animateContentSize inside scrolling lists.
 */
internal const val ANIM_FAST = 120
internal const val ANIM_MEDIUM = 200
internal const val ANIM_SLOW = 280
