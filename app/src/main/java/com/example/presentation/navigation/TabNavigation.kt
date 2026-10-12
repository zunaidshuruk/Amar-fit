package com.example.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Opens a top-level tab the same way the bottom bar does: one entry per tab,
 * state saved and restored. Used by the bar, the More sheet and notification links,
 * so hidden tabs (shown under More) open exactly like visible ones.
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
