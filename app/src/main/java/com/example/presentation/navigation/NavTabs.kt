package com.example.presentation.navigation

object NavTabs {
    val CHOOSABLE_ROUTES = listOf("fitness", "nutrition", "sleep", "health", "friends", "settings")
    val DEFAULT_BAR = listOf("fitness", "nutrition", "health")
    const val MAX_BAR = 3

    fun parse(csv: String?): List<String> {
        if (csv.isNullOrBlank()) return DEFAULT_BAR
        val routes = csv.split(",")
            .map { it.trim().lowercase() }
            .filter { it in CHOOSABLE_ROUTES }
            .distinct()
            .take(MAX_BAR)
        return if (routes.isEmpty()) DEFAULT_BAR else routes
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
            if (bar.size > 1) {
                bar.filter { it != route }
            } else {
                bar
            }
        } else {
            if (bar.size < MAX_BAR) {
                bar + route
            } else {
                bar
            }
        }
    }

    fun moveUp(bar: List<String>, route: String): List<String> {
        val index = bar.indexOf(route)
        if (index <= 0) return bar
        val newList = bar.toMutableList()
        val temp = newList[index]
        newList[index] = newList[index - 1]
        newList[index - 1] = temp
        return newList
    }

    fun moveDown(bar: List<String>, route: String): List<String> {
        val index = bar.indexOf(route)
        if (index < 0 || index >= bar.size - 1) return bar
        val newList = bar.toMutableList()
        val temp = newList[index]
        newList[index] = newList[index + 1]
        newList[index + 1] = temp
        return newList
    }
}
