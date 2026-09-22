package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Visual tier of a badge, matching an Apple-Fitness-style medallion ring color.
 * Higher tiers = rarer / harder to earn, and get a brighter ring + stronger shine animation
 * in BadgeGalleryScreen.
 */
enum class BadgeTier {
    BRONZE, SILVER, GOLD, PLATINUM
}

data class BadgeDefinition(
    val id: String, // must exactly match the string stored in UserProfile.badges
    val displayName: String,
    val description: String,
    val icon: ImageVector,
    val points: Int,
    val tier: BadgeTier = BadgeTier.BRONZE
)

val ALL_BADGES: List<BadgeDefinition> = listOf(
    // --- Original badges ---
    BadgeDefinition(
        id = "Hydration Hero",
        displayName = "Hydration Hero",
        description = "Hit your daily water goal.",
        icon = Icons.Default.WaterDrop,
        points = 50,
        tier = BadgeTier.SILVER
    ),
    BadgeDefinition(
        id = "10k Steps Master",
        displayName = "10k Steps Master",
        description = "Log 10,000 steps in a single day.",
        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
        points = 100,
        tier = BadgeTier.GOLD
    ),
    BadgeDefinition(
        id = "Consistency Starter",
        displayName = "Consistency Starter",
        description = "Log your health data to get started.",
        icon = Icons.Default.EventAvailable,
        points = 20,
        tier = BadgeTier.BRONZE
    ),

    // --- Steps & distance ---
    BadgeDefinition("First Steps", "First Steps", "Log your very first steps.", Icons.AutoMirrored.Filled.DirectionsWalk, 10, BadgeTier.BRONZE),
    BadgeDefinition("5K Strider", "5K Strider", "Log 5,000 steps in a single day.", Icons.AutoMirrored.Filled.DirectionsWalk, 30, BadgeTier.BRONZE),
    BadgeDefinition("15K Steps Champion", "15K Steps Champion", "Log 15,000 steps in a single day.", Icons.AutoMirrored.Filled.DirectionsWalk, 150, BadgeTier.GOLD),
    BadgeDefinition("20K Steps Legend", "20K Steps Legend", "Log 20,000 steps in a single day.", Icons.AutoMirrored.Filled.DirectionsWalk, 250, BadgeTier.PLATINUM),
    BadgeDefinition("Distance Walker", "Distance Walker", "Cover 5 km in a single day.", Icons.Default.Straighten, 60, BadgeTier.SILVER),
    BadgeDefinition("Marathon Mover", "Marathon Mover", "Cover 10 km in a single day.", Icons.Default.EmojiEvents, 150, BadgeTier.GOLD),
    BadgeDefinition("Half Marathon Hero", "Half Marathon Hero", "Cover 21 km in a single day.", Icons.Default.EmojiEvents, 300, BadgeTier.PLATINUM),

    // --- Hydration ---
    BadgeDefinition("Water Sipper", "Water Sipper", "Log at least 1L of water in a day.", Icons.Default.LocalDrink, 15, BadgeTier.BRONZE),
    BadgeDefinition("Halfway Hydrated", "Halfway Hydrated", "Reach half your daily water goal.", Icons.Default.WaterDrop, 25, BadgeTier.BRONZE),
    BadgeDefinition("Double Hydration", "Double Hydration", "Log double your daily water goal.", Icons.Default.Waves, 70, BadgeTier.SILVER),

    // --- Sleep ---
    BadgeDefinition("Power Rest", "Power Rest", "Log 7+ hours of sleep.", Icons.Default.Bedtime, 40, BadgeTier.SILVER),
    BadgeDefinition("Well Rested", "Well Rested", "Log 8+ hours of sleep.", Icons.Default.Bedtime, 60, BadgeTier.SILVER),
    BadgeDefinition("Deep Sleeper", "Deep Sleeper", "Log 9+ hours of sleep.", Icons.Default.Bedtime, 90, BadgeTier.GOLD),

    // --- Heart & vitals ---
    BadgeDefinition("Heart Check-In", "Heart Check-In", "Log your resting heart rate.", Icons.Default.MonitorHeart, 15, BadgeTier.BRONZE),
    BadgeDefinition("Steady Heart", "Steady Heart", "Log a resting heart rate in the healthy 40-70 bpm range.", Icons.Default.Favorite, 80, BadgeTier.GOLD),
    BadgeDefinition("Cardio Zone", "Cardio Zone", "Reach a heart rate of 140+ bpm during activity.", Icons.Default.MonitorHeart, 70, BadgeTier.SILVER),
    BadgeDefinition("HRV Tracker", "HRV Tracker", "Log your heart rate variability.", Icons.Default.Timeline, 20, BadgeTier.BRONZE),
    BadgeDefinition("Oxygen Ace", "Oxygen Ace", "Log blood oxygen saturation of 95%+.", Icons.Default.Air, 50, BadgeTier.SILVER),
    BadgeDefinition("Breath Aware", "Breath Aware", "Log your respiratory rate.", Icons.Default.Air, 15, BadgeTier.BRONZE),
    BadgeDefinition("Temperature Check", "Temperature Check", "Log your skin temperature.", Icons.Default.Thermostat, 10, BadgeTier.BRONZE),

    // --- Workouts ---
    BadgeDefinition("Workout Warrior", "Workout Warrior", "Exercise for 30+ minutes in a day.", Icons.Default.FitnessCenter, 60, BadgeTier.SILVER),
    BadgeDefinition("Iron Will", "Iron Will", "Exercise for 60+ minutes in a day.", Icons.Default.FitnessCenter, 100, BadgeTier.GOLD),
    BadgeDefinition("Endurance Elite", "Endurance Elite", "Exercise for 90+ minutes in a day.", Icons.Default.FitnessCenter, 180, BadgeTier.PLATINUM),
    BadgeDefinition("Calorie Crusher", "Calorie Crusher", "Burn 500+ active calories in a day.", Icons.Default.LocalFireDepartment, 100, BadgeTier.GOLD),
    BadgeDefinition("Fat Burner", "Fat Burner", "Burn 300+ active calories in a day.", Icons.Default.LocalFireDepartment, 60, BadgeTier.SILVER),

    // --- Mindfulness ---
    BadgeDefinition("Mindful Minute", "Mindful Minute", "Log your first mindfulness session.", Icons.Default.SelfImprovement, 10, BadgeTier.BRONZE),
    BadgeDefinition("Calm Mind", "Calm Mind", "Log 10+ minutes of mindfulness in a day.", Icons.Default.SelfImprovement, 50, BadgeTier.SILVER),
    BadgeDefinition("Zen Master", "Zen Master", "Log 20+ minutes of mindfulness in a day.", Icons.Default.SelfImprovement, 90, BadgeTier.GOLD),

    // --- Nutrition ---
    BadgeDefinition("Protein Powerhouse", "Protein Powerhouse", "Log 100g+ of protein in a day.", Icons.Default.RestaurantMenu, 60, BadgeTier.SILVER),
    BadgeDefinition("Balanced Plate", "Balanced Plate", "Log carbs, protein, and fat in the same day.", Icons.Default.Restaurant, 50, BadgeTier.SILVER),
    BadgeDefinition("Macro Master", "Macro Master", "Log 120g+ protein, 150g+ carbs, and 50g+ fat in one day.", Icons.Default.Restaurant, 150, BadgeTier.PLATINUM),
    BadgeDefinition("Calorie Conscious", "Calorie Conscious", "Stay within your daily calorie limit.", Icons.Default.LocalDining, 80, BadgeTier.GOLD),
    BadgeDefinition("Mindful Eater", "Mindful Eater", "Log a meal through the food scanner or log.", Icons.Default.DinnerDining, 20, BadgeTier.BRONZE),

    // --- Body & metrics logging ---
    BadgeDefinition("Weigh-In Warrior", "Weigh-In Warrior", "Log your weight.", Icons.Default.MonitorWeight, 15, BadgeTier.BRONZE),
    BadgeDefinition("Glucose Guardian", "Glucose Guardian", "Log a blood glucose reading.", Icons.Default.Insights, 20, BadgeTier.BRONZE),
    BadgeDefinition("In-Range Champion", "In-Range Champion", "Log a glucose reading within your target range.", Icons.Default.Insights, 90, BadgeTier.GOLD),
    BadgeDefinition("BP Tracker", "BP Tracker", "Log your blood pressure.", Icons.Default.MonitorHeart, 20, BadgeTier.BRONZE),
    BadgeDefinition("Healthy Pressure", "Healthy Pressure", "Log a blood pressure reading at or below 120/80.", Icons.Default.Favorite, 90, BadgeTier.GOLD),

    // --- Streaks ---
    BadgeDefinition("Week Warrior", "Week Warrior", "Reach a 7-day logging streak.", Icons.Default.DateRange, 100, BadgeTier.GOLD),
    BadgeDefinition("Fortnight Fighter", "Fortnight Fighter", "Reach a 14-day logging streak.", Icons.Default.DateRange, 150, BadgeTier.GOLD),
    BadgeDefinition("Monthly Master", "Monthly Master", "Reach a 30-day logging streak.", Icons.Default.EventAvailable, 300, BadgeTier.PLATINUM),
    BadgeDefinition("Quarter Champion", "Quarter Champion", "Reach a 90-day logging streak.", Icons.Default.EventAvailable, 500, BadgeTier.PLATINUM),
    BadgeDefinition("Century Streak", "Century Streak", "Reach a 100-day logging streak.", Icons.Default.Star, 600, BadgeTier.PLATINUM),
    BadgeDefinition("Half-Year Hero", "Half-Year Hero", "Reach a 180-day logging streak.", Icons.Default.Star, 800, BadgeTier.PLATINUM),
    BadgeDefinition("Year-Long Legend", "Year-Long Legend", "Reach a 365-day logging streak.", Icons.Default.EmojiEvents, 1000, BadgeTier.PLATINUM),

    // --- Combo / all-rounder ---
    BadgeDefinition("Triple Threat", "Triple Threat", "Hit your steps, water, and sleep goals on the same day.", Icons.Default.AutoAwesome, 200, BadgeTier.PLATINUM),
    BadgeDefinition("Full Log Day", "Full Log Day", "Log steps, water, sleep, weight, and exercise in one day.", Icons.Default.Analytics, 120, BadgeTier.GOLD),
    BadgeDefinition("Data Devotee", "Data Devotee", "Log 5 or more different health metrics in one day.", Icons.Default.Analytics, 100, BadgeTier.GOLD),
    BadgeDefinition("Two Birds", "Two Birds", "Log 30+ minutes of exercise and 10+ minutes of mindfulness in one day.", Icons.Default.AutoAwesome, 100, BadgeTier.GOLD),
    BadgeDefinition("Vitals Check", "Vitals Check", "Log blood pressure, respiratory rate, and oxygen saturation in one day.", Icons.Default.Shield, 90, BadgeTier.GOLD),

    // --- Points milestones ---
    BadgeDefinition("Point Collector", "Point Collector", "Earn 500 total reward points.", Icons.Default.Star, 0, BadgeTier.GOLD),
    BadgeDefinition("Point Master", "Point Master", "Earn 1,000 total reward points.", Icons.Default.EmojiEvents, 0, BadgeTier.PLATINUM),
    BadgeDefinition("Point Legend", "Point Legend", "Earn 2,500 total reward points.", Icons.Default.EmojiEvents, 0, BadgeTier.PLATINUM)
)
