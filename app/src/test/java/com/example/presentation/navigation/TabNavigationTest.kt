package com.example.presentation.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.TabScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class TabNavigationTest {

    private lateinit var navController: NavHostController

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(androidx.navigation.compose.ComposeNavigator())
        
        val graph = navController.createGraph(startDestination = TabScreen.Today.route) {
            addDestination(
                androidx.navigation.compose.ComposeNavigator().createDestination().apply {
                    route = TabScreen.Today.route
                }
            )
            addDestination(
                androidx.navigation.compose.ComposeNavigator().createDestination().apply {
                    route = TabScreen.Nutrition.route
                }
            )
            addDestination(
                androidx.navigation.compose.ComposeNavigator().createDestination().apply {
                    route = TabScreen.Fitness.route
                }
            )
            addDestination(
                androidx.navigation.compose.ComposeNavigator().createDestination().apply {
                    route = TabScreen.Sleep.route
                }
            )
            addDestination(
                androidx.navigation.compose.ComposeNavigator().createDestination().apply {
                    route = TabScreen.Health.route
                }
            )
        }
        navController.graph = graph
    }

    @Test
    fun testStartDestinationIsToday() {
        assertEquals(TabScreen.Today.route, navController.currentDestination?.route)
    }

    @Test
    fun testNavigationFromNutritionToToday() {
        navigateToTab(navController, TabScreen.Nutrition.route)
        assertEquals(TabScreen.Nutrition.route, navController.currentDestination?.route)

        navigateToTab(navController, TabScreen.Today.route)
        assertEquals(TabScreen.Today.route, navController.currentDestination?.route)
    }

    @Test
    fun testNavigationFromFitnessToToday() {
        navigateToTab(navController, TabScreen.Fitness.route)
        assertEquals(TabScreen.Fitness.route, navController.currentDestination?.route)

        navigateToTab(navController, TabScreen.Today.route)
        assertEquals(TabScreen.Today.route, navController.currentDestination?.route)
    }

    @Test
    fun testNavigationFromSleepToToday() {
        navigateToTab(navController, TabScreen.Sleep.route)
        assertEquals(TabScreen.Sleep.route, navController.currentDestination?.route)

        navigateToTab(navController, TabScreen.Today.route)
        assertEquals(TabScreen.Today.route, navController.currentDestination?.route)
    }

    @Test
    fun testNavigationFromHealthToToday() {
        navigateToTab(navController, TabScreen.Health.route)
        assertEquals(TabScreen.Health.route, navController.currentDestination?.route)

        navigateToTab(navController, TabScreen.Today.route)
        assertEquals(TabScreen.Today.route, navController.currentDestination?.route)
    }

    @Test
    fun testSwitchingBetweenAllTabs() {
        val tabs = listOf(
            TabScreen.Nutrition,
            TabScreen.Fitness,
            TabScreen.Sleep,
            TabScreen.Health,
            TabScreen.Today
        )

        for (tab in tabs) {
            navigateToTab(navController, tab.route)
            assertEquals(tab.route, navController.currentDestination?.route)
        }
    }

    @Test
    fun testBackStackReturnsDirectlyToStartDestinationOnBackPress() {
        // Navigate to nutrition via navigateToTab
        navigateToTab(navController, TabScreen.Nutrition.route)
        assertEquals(TabScreen.Nutrition.route, navController.currentDestination?.route)

        // System back should pop back directly to Today (start destination)
        val popped = navController.popBackStack()
        assertEquals(true, popped)
        assertEquals(TabScreen.Today.route, navController.currentDestination?.route)

        // Further back pops off graph / exits app
        val poppedAgain = navController.popBackStack()
        assertEquals(false, poppedAgain)
    }

    @Test
    fun testRepeatedTabNavigationDoesNotDuplicateBackStack() {
        // Repeatedly navigate to fitness and nutrition via navigateToTab
        navigateToTab(navController, TabScreen.Fitness.route)
        navigateToTab(navController, TabScreen.Nutrition.route)
        navigateToTab(navController, TabScreen.Fitness.route)
        assertEquals(TabScreen.Fitness.route, navController.currentDestination?.route)

        // A single back press should return to start destination (Today), not a duplicate Fitness or Nutrition instance
        val popped = navController.popBackStack()
        assertEquals(true, popped)
        assertEquals(TabScreen.Today.route, navController.currentDestination?.route)
    }
}
