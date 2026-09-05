package com.example.presentation.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.*

@Composable
fun NutritionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Nutrition & Diet", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        
        NutritionCard(
            title = "Food Log & Scanner",
            subtitle = "Track your daily meals",
            icon = Icons.Default.Restaurant,
            color = Emerald500,
            bgColor = Emerald50,
            onClick = { navController.navigate("foodlog") }
        )
        
        NutritionCard(
            title = "Meal Plan",
            subtitle = "Your customized diet plan",
            icon = Icons.Default.RestaurantMenu,
            color = Blue500,
            bgColor = BlueBg,
            onClick = { navController.navigate("mealplan") }
        )
        
        NutritionCard(
            title = "Diet Chart",
            subtitle = "Weekly diet breakdown",
            icon = Icons.Default.Assignment,
            color = Indigo600,
            bgColor = IndigoBg,
            onClick = { navController.navigate("dietplan") }
        )
        
        NutritionCard(
            title = "Medicinal Recipes",
            subtitle = "Healthy recipes for your goals",
            icon = Icons.Default.LocalDining,
            color = Red700,
            bgColor = Red100,
            onClick = { navController.navigate("recipe") }
        )
    }
}

@Composable
fun NutritionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, bgColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = subtitle, fontSize = 14.sp, color = Slate500)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
        }
    }
}
