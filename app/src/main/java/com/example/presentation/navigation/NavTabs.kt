package com.example.presentation.navigation

object NavTabs {
    val CHOOSABLE_ROUTES = listOf("fitness", "nutrition", "sleep", "health", "friends", "settings")
    val DEFAULT_BAR = listOf("fitness", "nutrition", "health")
    const val MAX_BAR = 3

    fun parse(csv: String?): List<String> {
        if (csv.isNullOrBlank()) return DEFAULT_BAR
        val parsed = csv.split(",")
            .map { it.trim().lowercase() }
            .filter { it in CHOOSABLE_ROUTES }
            .distinct()
            .take(MAX_BAR)
        return if (parsed.isEmpty()) DEFAULT_BAR else parsed
    }

    fun toCsv(routes: List<String>): String {
        return routes.joinToString(",")
    }

    fun hidden(bar: List<String>): List<String> {
        return CHOOSABLE_ROUTES.filter { it !in bar }
    }

    fun toggle(bar: List<String>, route: String): List<String> {
        if (route !in CHOOSABLE_ROUTES) return bar
        return if (route in bar) {
            if (bar.size <= 1) bar else bar.filter { it != route }
        } else {
            if (bar.size >= MAX_BAR) bar else bar + route
        }
    }

    fun moveUp(bar: List<String>, route: String): List<String> {
        val index = bar.indexOf(route)
        if (index <= 0) return bar
        val mutable = bar.toMutableList()
        val temp = mutable[index]
        mutable[index] = mutable[index - 1]
        mutable[index - 1] = temp
        return mutable
    }

    fun moveDown(bar: List<String>, route: String): List<String> {
        val index = bar.indexOf(route)
        if (index < 0 || index >= bar.size - 1) return bar
        val mutable = bar.toMutableList()
        val temp = mutable[index]
        mutable[index] = mutable[index + 1]
        mutable[index + 1] = temp
        return mutable
    }
}
