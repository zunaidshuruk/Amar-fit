package com.example.presentation.mealplan

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.ui.theme.*

data class Meal(val type: String, val title: String, val instructions: String, val videoQuery: String)
data class DailyPlan(val day: Int, val meals: List<Meal>)
data class ShoppingItem(val name: String, val quantity: String)

val shoppingListItems = listOf(
    ShoppingItem("Desi Eggs", "18-24 pcs"),
    ShoppingItem("Grass-fed Beef (Ribs, Curry cuts)", "1.5 kg"),
    ShoppingItem("Desi Chicken", "1 kg"),
    ShoppingItem("Mutton", "1 kg"),
    ShoppingItem("Local Fish (Rui, Ilish)", "1.5 kg"),
    ShoppingItem("Fresh Prawns", "500g"),
    ShoppingItem("Spinach (Lal Shak, Palak)", "3-4 Bunches"),
    ShoppingItem("Bottle Gourd (Lau)", "1 Medium"),
    ShoppingItem("Cabbage, Cauliflower, Broccoli", "1 each"),
    ShoppingItem("Eggplant, Okra, Bitter/Snake Gourd", "500g each"),
    ShoppingItem("Salad Veg (Cucumber, Tomato, Bell Pepper)", "1.5 kg total"),
    ShoppingItem("Mushrooms", "250g"),
    ShoppingItem("Pure Ghee / Butter", "1 Jar (500g)"),
    ShoppingItem("Mustard Oil / Extra Virgin Olive Oil", "1 Bottle each"),
    ShoppingItem("Cold-pressed Coconut Oil", "1 Bottle"),
    ShoppingItem("Coconut Milk (Unsweetened)", "2 Cans"),
    ShoppingItem("Almonds, Walnuts, Chia Seeds", "100g each"),
    ShoppingItem("Apple Cider Vinegar (with Mother)", "1 Bottle"),
    ShoppingItem("Black Coffee, Raw Cacao Powder", "1 Pack each")
)

val mealPlans = listOf(
    DailyPlan(1, listOf(
        Meal("Breakfast", "2-3 Desi Egg Omelet + Sauteed Spinach", "Sauté spinach in olive oil until wilted. Whisk eggs, pour over spinach, and cook in butter.", "How to cook spinach omelet keto"),
        Meal("Lunch", "Pan-Seared Desi Fish + Large Mixed Salad", "Marinate fish with turmeric/salt, pan-fry in mustard oil. Toss cucumber, tomatoes, green chili for salad.", "How to cook pan seared Rui fish"),
        Meal("Dinner", "Beef Lau-Gosh (Bottle Gourd Beef)", "Sauté spices in mustard oil. Sear grass-fed beef, simmer until tender. Add bottle gourd chunks and cook until soft.", "How to cook beef with bottle gourd lau gosh")
    )),
    DailyPlan(2, listOf(
        Meal("Breakfast", "Bulletproof Coffee + Almonds/Walnuts", "Blend 1 cup black coffee, 1 tbsp ghee, 1 tbsp coconut oil, 1 tsp MCT oil for 30s until frothy. Serve with soaked nuts.", "How to make bulletproof coffee"),
        Meal("Lunch", "Grilled Desi Chicken + Stir-fried Cabbage", "Marinate chicken and grill. Thinly slice cabbage and stir-fry rapidly in coconut oil with mild spices.", "How to cook keto grilled chicken and cabbage"),
        Meal("Dinner", "Prawn Curry with Snake Gourd", "Sauté prawns lightly. Cook snake gourd (chichinga) with spices, add coconut milk, and simmer with prawns.", "How to cook prawn curry with coconut milk keto")
    )),
    DailyPlan(3, listOf(
        Meal("Breakfast", "Poached Desi Eggs + Mashed Eggplant", "Poach eggs. Fire-roast eggplant, peel, and mash. Sauté garlic, onions, red chilies in mustard oil and mix with eggplant.", "How to make begun bhorta traditional"),
        Meal("Lunch", "Home-cooked Beef Ribs + ACV Salad", "Slow-cook beef ribs until tender. Toss fresh salad with organic Apple Cider Vinegar dressing.", "How to cook beef ribs keto"),
        Meal("Dinner", "Baked Desi Fish (Rui/Ilish) + Steamed Cauliflower", "Season fish with turmeric and bake. Serve alongside lightly steamed cauliflower florets.", "How to bake whole fish traditional")
    )),
    DailyPlan(4, listOf(
        Meal("Breakfast", "3-Egg Scramble with Mushrooms", "Sauté onions, chilies, and fresh mushrooms in pure ghee. Add beaten eggs and scramble gently.", "How to make mushroom egg scramble"),
        Meal("Lunch", "Mutton Curry + Stir-fried Okra", "Cook mutton in ghee with traditional spices. Stir-fry okra (dherosh) separately in oil until non-sticky.", "How to cook dherosh bhaji okra"),
        Meal("Dinner", "Mixed Shak + Pan-fried Fish", "Sauté sliced garlic and dry red chilies in mustard oil. Toss leafy greens and stir-fry for 3-4 mins. Serve with pan-fried fish.", "How to cook lal shak bhaji")
    )),
    DailyPlan(5, listOf(
        Meal("Breakfast", "Desi Egg Omelet with Bell Peppers", "Dice red and green bell peppers. Sauté in ghee, then pour whisked eggs to form a thick omelet.", "How to cook bell pepper omelet"),
        Meal("Lunch", "Pan-Fried Desi Fish + Lal Shak (Red Spinach)", "Pan-fry fish in mustard oil. Stir-fry red spinach with garlic and chilies on low heat to preserve micronutrients.", "How to cook lal shak bangladeshi style"),
        Meal("Dinner", "Beef Curry with Bitter Gourd (Korola)", "Cook beef curry traditionally. Add bitter gourd slices towards the end and simmer until cooked but firm.", "How to cook korola beef curry")
    )),
    DailyPlan(6, listOf(
        Meal("Breakfast", "LCHF Smoothie", "Blend unsweetened coconut milk, 1/2 tsp raw cacao powder, and soaked chia seeds. Do not add sugar.", "How to make keto chia chocolate smoothie"),
        Meal("Lunch", "Grilled Chicken + Stir-fried Broccoli", "Grill chicken breast or thighs. Blanch broccoli, then quickly stir-fry in coconut oil with garlic.", "How to cook broccoli stir fry garlic"),
        Meal("Dinner", "Fresh Prawns in Coconut Milk", "Simmer fresh prawns in thick coconut milk seasoned with turmeric, green chilies, and traditional spices.", "How to cook coconut prawn curry")
    )),
    DailyPlan(7, listOf(
        Meal("Breakfast", "Fried Desi Eggs + Mixed Veg", "Fry eggs in homemade butter. Sauté a mix of low-carb vegetables (zucchini, broccoli, cauliflower) in olive oil.", "How to cook keto mixed vegetables breakfast"),
        Meal("Lunch", "Mutton Lau-Gosh", "Prepare mutton curry and add bottle gourd (lau) chunks. Simmer until the lau absorbs the rich mutton broth.", "How to cook mutton with bottle gourd"),
        Meal("Dinner", "Lemon Turmeric Baked Fish", "Marinate fish with fresh lemon juice, turmeric, salt, and green chilies. Bake until flaky and tender.", "How to bake fish with lemon and turmeric")
    ))
)

@Composable
fun MealPlanScreen(
    viewModel: ShasthoViewModel? = null,
    onNavigateBack: () -> Unit = {}
) {
    val vm: ShasthoViewModel? = viewModel ?: runCatching { androidx.lifecycle.viewmodel.compose.viewModel<ShasthoViewModel>() }.getOrNull()
    val profile by (vm?.userProfile ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
    val isDark = profile?.isDarkMode ?: isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Emerald900
                    )
                }
                Text(
                    text = "7-Day Fat Adaptation Plan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald900
                )
            }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Follow this structural 7-day schedule for the first week. Eat ONLY when genuinely hungry.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            item {
                ShoppingListCard(isDark = isDark)
            }
            
            items(mealPlans) { plan ->
                DailyPlanCard(plan)
            }
            
            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun DailyPlanCard(plan: DailyPlan) {
    var expanded by remember { mutableStateOf(plan.day == 1) } // First day open by default
    val context = LocalContext.current
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Day ${plan.day}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                    contentDescription = "Toggle", 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                plan.meals.forEach { meal ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Text(meal.type, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(meal.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Prep: ${meal.instructions}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(meal.videoQuery)}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Red500),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Watch", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watch Recipe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingListCard(isDark: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    val checkedState = remember { mutableStateMapOf<String, Boolean>() }
    val stepsAccent = AccentTokens.stepsAccent(isDark)
    
    Card(
        colors = CardDefaults.cardColors(containerColor = stepsAccent.bg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Shopping List", tint = if (isDark) stepsAccent.onBg else Emerald700)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("7-Day Shopping List", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (isDark) stepsAccent.onBg else Emerald900)
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, 
                    contentDescription = "Toggle", 
                    tint = if (isDark) stepsAccent.onBg else Emerald700
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                shoppingListItems.forEach { item ->
                    val isChecked = checkedState[item.name] ?: false
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checkedState[item.name] = !isChecked }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checkedState[item.name] = it },
                            colors = CheckboxDefaults.colors(checkedColor = Emerald600)
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = item.name, 
                                fontSize = 16.sp, 
                                color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else (if (isDark) stepsAccent.onBg else MaterialTheme.colorScheme.onSurface),
                                textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                            )
                            Text(text = item.quantity, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
