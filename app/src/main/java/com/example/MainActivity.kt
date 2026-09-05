package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.navigation.compose.*
import com.example.presentation.chat.ChatScreen
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.scanner.ScannerScreen
import com.example.presentation.workout.WorkoutScreen
import com.example.presentation.foodlog.FoodLogScreen
import com.example.presentation.settings.SettingsScreen



import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.presentation.notifications.NotificationHelper
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

class MainActivity : ComponentActivity(), SensorEventListener {
  private val viewModel: ShasthoViewModel by viewModels()
  private var sensorManager: SensorManager? = null
  private var stepSensor: Sensor? = null
  private var isSensorRegistered = false


  @OptIn(ExperimentalPermissionsApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    NotificationHelper.createNotificationChannel(this)
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)


    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val permissionState = rememberMultiplePermissionsState(
            permissions = listOf(
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            )
        )
        LaunchedEffect(Unit) {
            permissionState.launchMultiplePermissionRequest()
        }
        
        // Start background workers for reminders
        LaunchedEffect(permissionState.allPermissionsGranted) {
             if (permissionState.allPermissionsGranted) {
                 com.example.presentation.notifications.ReminderManager.scheduleMealReminders(this@MainActivity)
                 viewModel.checkNutritionalDeficiencies(this@MainActivity)
             }
        }
        
        val navController = rememberNavController()
        val userProfile by viewModel.userProfile.collectAsState()
        
        val startDestination = if (userProfile == null) "auth" else "dashboard"
        
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = Background,
          bottomBar = { 
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            if (currentRoute == "dashboard" || currentRoute == "chat" || currentRoute == "lifestyle" || currentRoute == "settings" || currentRoute == "coach" || currentRoute == "workout" || currentRoute == "foodlog") {
              BottomNavBar(navController, currentRoute) 
            }
          }
        ) { innerPadding ->
          NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
          ) {
            composable("auth") {
              com.example.presentation.auth.AuthScreen(onNavigateToOnboarding = {
                navController.navigate("onboarding") {
                  popUpTo("auth") { inclusive = true }
                }
              })
            }

            composable("onboarding") {
              OnboardingScreen(viewModel = viewModel, onComplete = {
                navController.navigate("dashboard") {
                  popUpTo("onboarding") { inclusive = true }
                }
              })
            }
            composable("dashboard") {
              BentoDashboard(viewModel, navController, modifier = Modifier)
            }
            composable("chat") {
              ChatScreen(viewModel = viewModel)
            }
            composable("coach") {
              com.example.presentation.coach.CoachScreen(viewModel = viewModel)
            }
            composable("dietplan") {
                com.example.presentation.mealplan.DietChartScreen(viewModel = viewModel)
            }
            composable("lifestyle") {
              com.example.presentation.lifestyle.LifestyleScreen()
            }
            composable("mealplan") {
              com.example.presentation.mealplan.MealPlanScreen()
            }
            composable("workout") {
              WorkoutScreen(viewModel = viewModel)
            }
            composable("glucoselog") {
              com.example.presentation.metrics.GlucoseLogScreen(viewModel = viewModel)
            }
            composable("weightlog") {
              com.example.presentation.metrics.WeightLogScreen(viewModel = viewModel)
            }
            composable("recipe") {
              com.example.presentation.recipe.RecipeScreen(viewModel = viewModel)
            }
            composable("foodlog") {
              FoodLogScreen(
                viewModel = viewModel,
                onNavigateToScanner = { navController.navigate("scanner") }
              )
            }
            composable("settings") {
              SettingsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onLogout = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } })
            }
            composable("scanner") {
              ScannerScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (!isSensorRegistered) {
        stepSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            isSensorRegistered = true
        }
    }
  }

  override fun onPause() {
    super.onPause()
    if (isSensorRegistered) {
        sensorManager?.unregisterListener(this)
        isSensorRegistered = false
    }
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
        viewModel.addSteps(1)
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

@Composable
fun BentoDashboard(viewModel: ShasthoViewModel, navController: NavHostController, modifier: Modifier = Modifier) {
  val profile by viewModel.userProfile.collectAsState()
  val metrics by viewModel.todayMetrics.collectAsState()
  val todayFoodLogs by viewModel.todayFoodLogs.collectAsState()
  
  var showStepsDialog by remember { mutableStateOf(false) }
  var showStepsOptionDialog by remember { mutableStateOf(false) }
  var showWeightDialog by remember { mutableStateOf(false) }
  var showBpDialog by remember { mutableStateOf(false) }
  var showSleepDialog by remember { mutableStateOf(false) }
  var showBmiDialog by remember { mutableStateOf(false) }
  var showWaterDialog by remember { mutableStateOf(false) }

  val context = androidx.compose.ui.platform.LocalContext.current
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.syncWithHealthConnect(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)


    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }
  
  LaunchedEffect(Unit) {
    while(true) {
      kotlinx.coroutines.delay(15_000) // Poll every 15 seconds while dashboard is visible
      viewModel.syncWithHealthConnect(context)
    }
  }


  
  val name = profile?.name ?: "Guest"
  val initial = name.firstOrNull()?.toString() ?: "G"
  val calorieLimit = profile?.dailyCalorieLimit ?: 2000
  val totalCalories = todayFoodLogs.sumOf { it.calories }
  val calorieProgress = if (calorieLimit > 0) (totalCalories.toFloat() / calorieLimit.toFloat()).coerceIn(0f, 1f) else 0f
  val waterLimit = profile?.dailyWaterLimitLiters ?: 3.0f
  val waterConsumed = metrics?.waterLiters ?: 0f
  val steps = metrics?.steps ?: 0
  val glucose = maxOf(metrics?.bloodGlucoseMorning ?: 0f, metrics?.bloodGlucoseNight ?: 0f)
  val sleepHours = metrics?.sleepHours ?: 0f
  val heartRate = metrics?.heartRate ?: 0
  val bloodPressure = metrics?.bloodPressure ?: ""
  val badges = profile?.badges?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
  val points = profile?.points ?: 0
  val currentStreak = profile?.currentStreak ?: 0

  val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
  val greeting = when (hour) {
      in 0..11 -> "Good Morning"
      12 -> "Good Noon"
      in 13..16 -> "Good Afternoon"
      in 17..20 -> "Good Evening"
      else -> "Good Night"
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
        Text(
          text = "HEALTH & DIET",
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          color = Emerald700,
          letterSpacing = 1.sp
        )
        Text(
          text = "$greeting, $name",
          fontSize = 24.sp,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          maxLines = 1,
          overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var isHealthConnectAvailable by remember { mutableStateOf(androidx.health.connect.client.HealthConnectClient.getSdkStatus(context) == androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE) }
        
        if (isHealthConnectAvailable) {
          IconButton(
            onClick = {
              android.widget.Toast.makeText(context, "Syncing with Health Connect...", android.widget.Toast.LENGTH_SHORT).show()
              viewModel.syncWithHealthConnect(context)
            },
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color.White)
              .border(1.dp, Slate200, CircleShape)
          ) {
             Icon(Icons.Default.Sync, contentDescription = "Sync Health Connect", tint = Emerald600)
          }
        }

        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Emerald200)
            .border(2.dp, Color.White, CircleShape)
            .shadow(2.dp, CircleShape)
            .clickable { navController.navigate("settings") },
          contentAlignment = Alignment.Center
        ) {
          if (!profile?.profilePictureUri.isNullOrEmpty()) {
              coil.compose.AsyncImage(
                  model = profile?.profilePictureUri,
                  contentDescription = "Profile Picture",
                  modifier = Modifier.fillMaxSize(),
                  contentScale = androidx.compose.ui.layout.ContentScale.Crop
              )
          } else {
              Text(
                  text = initial,
                  fontWeight = FontWeight.Bold,
                  color = Emerald800,
                  fontSize = 18.sp
              )
          }
        }
      }
    }
    
    // Gamification Points, Badges, & Streak Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      // Streak
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(OrangeBg)
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.LocalFireDepartment, contentDescription = "Streak", tint = Orange500)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "$currentStreak Days", fontWeight = FontWeight.Bold, color = Orange900, fontSize = 14.sp)
          Text(text = "Streak", fontSize = 10.sp, color = Orange700)
        }
      }
      // Points
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(IndigoBg)
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Star, contentDescription = "Points", tint = Indigo600)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "$points", fontWeight = FontWeight.Bold, color = Indigo900, fontSize = 14.sp)
          Text(text = "Points", fontSize = 10.sp, color = Indigo700)
        }
      }
      // Badges
      Box(
        modifier = Modifier
          .weight(1f)
          .clip(RoundedCornerShape(16.dp))
          .background(Emerald50)
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.EmojiEvents, contentDescription = "Badges", tint = Emerald500)
          Spacer(modifier = Modifier.height(4.dp))
          Text(text = "${badges.size}", fontWeight = FontWeight.Bold, color = Emerald900, fontSize = 14.sp)
          Text(text = "Badges", fontSize = 10.sp, color = Emerald700)
        }
      }
    }

    // Bento Grid Area
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

      // Row 1: Calorie Progress
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Color.White)
          .border(1.dp, Slate200, RoundedCornerShape(24.dp))
          .clickable { navController.navigate("foodlog") }
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("CALORIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(text = "$totalCalories", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              Text(text = " / $calorieLimit kcal", fontSize = 14.sp, color = Slate500, modifier = Modifier.padding(bottom = 6.dp))
            }
          }
          
          Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
            CircularProgressIndicator(
              progress = { 1f },
              modifier = Modifier.fillMaxSize(),
              color = Emerald50,
              strokeWidth = 8.dp,
            )
            CircularProgressIndicator(
              progress = { calorieProgress },
              modifier = Modifier.fillMaxSize(),
              color = Emerald500,
              strokeWidth = 8.dp,
              strokeCap = StrokeCap.Round
            )
            val pct = (calorieProgress * 100).toInt()
            Text(text = "$pct%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emerald700)
          }
        }
      }

      // Row 2: Water & Steps
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Water
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(BlueBg)
            .clickable { showWaterDialog = true }
            .padding(16.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Blue500))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "WATER", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Blue700)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
              Text(text = String.format("%.1f", waterConsumed), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Blue900)
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = "Ltrs", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Blue900, modifier = Modifier.padding(bottom = 2.dp))
            }
            Text(text = "Goal: $waterLimit Ltrs (+ Tap)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Blue700)
          }
        }

        // Steps
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(OrangeBg)
            .clickable { 
                showStepsOptionDialog = true
            }
            .padding(16.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Orange500))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = "STEPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Orange700)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "$steps", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Orange900)
            Text(text = "Keep it up! (+ Tap)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Orange700)
          }
        }
      }

      // Row 3: Glucose & BMI
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Glucose
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(IndigoBg)
            .clickable { navController.navigate("glucoselog") }
            .padding(16.dp)
        ) {
          Column {
            Text(text = "BLOOD GLUCOSE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if(glucose > 0) "$glucose" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Indigo900)
            Text(text = "mmol/L (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Indigo600)
          }
        }

        // BMI
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Emerald50)
            .border(1.dp, Emerald100, RoundedCornerShape(24.dp))
            .clickable { navController.navigate("weightlog") }
            .padding(16.dp)
        ) {
          val weight = profile?.weightKg ?: 70f
          val heightM = (profile?.heightCm ?: 170f) / 100f
          val bmi = if (heightM > 0) weight / (heightM * heightM) else 0f
          
          Column {
            Text(text = "BMI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = String.format("%.1f", bmi), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald900)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Emerald200)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(0.5f)
                  .fillMaxHeight()
                  .background(Emerald600)
              )
            }
          }
        }
      }

      // Row 4: Weight & Blood Pressure
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Weight
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Orange50)
            .border(1.dp, Orange100, RoundedCornerShape(24.dp))
            .clickable { navController.navigate("weightlog") }
            .padding(16.dp)
        ) {
          val weight = profile?.weightKg ?: 70f
          Column {
            Text(text = "WEIGHT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Orange700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "$weight kg", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Orange900)
            Text(text = "(+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Orange700)
          }
        }

        // Blood Pressure
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Red50)
            .border(1.dp, Red100, RoundedCornerShape(24.dp))
            .clickable { showBpDialog = true }
            .padding(16.dp)
        ) {
          Column {
            Text(text = "BLOOD PRESSURE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red700)
            Spacer(modifier = Modifier.height(16.dp))
            val bp = metrics?.bloodPressure?.takeIf { it.isNotBlank() } ?: "--"
            Text(text = bp, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Red900)
            Text(text = "mmHg (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Red700)
          }
        }
      }


      // Row 5: Sleep & Heart Rate
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Sleep
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(IndigoBg)
            .clickable { showSleepDialog = true }
            .padding(16.dp)
        ) {
          Column {
            Text(text = "SLEEP TRACKING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = String.format("%.1f Hours", sleepHours), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Indigo900)
            Text(text = "(+ Tap to log manually)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Indigo700)
          }
        }
        
        // Heart Rate
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Red50)
            .border(1.dp, Red100, RoundedCornerShape(24.dp))
            .padding(16.dp)
        ) {
          Column {
            Text(text = "HEART RATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if(heartRate > 0) "$heartRate bpm" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Red900)
            Text(text = "(Synced automatically)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Red700)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text("Dietary Protocol", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

      // Meal Plan Module
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Emerald900)
          .clickable { navController.navigate("mealplan") }
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Emerald500),
              contentAlignment = Alignment.Center
            ) {
              Icon(imageVector = Icons.Default.Restaurant, contentDescription = "Meal Plan", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "7-Day Fat Adaptation Plan", color = Color.White, fontWeight = FontWeight.Bold)
              Text(text = "View daily meals & video recipes", color = Emerald100, fontSize = 12.sp)
            }
          }
          Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Color.White)
        }
      }


Spacer(modifier = Modifier.height(8.dp))
      Text("AI Modules", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

      // Food Log Module
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Slate900)
          .clickable { navController.navigate("foodlog") }
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Emerald500),
              contentAlignment = Alignment.Center
            ) {
              Icon(imageVector = Icons.Default.Restaurant, contentDescription = "Food", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "AI Food Log & Scanner", color = Color.White, fontWeight = FontWeight.Bold)
              Text(text = "Scan & track your meals", color = Slate400, fontSize = 12.sp)
            }
          }
          IconButton(onClick = { navController.navigate("scanner") }) {
            Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "Scan Food", tint = Emerald500)
          }
        }
      }

      // Coach Module
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Color.White)
          .border(1.dp, Slate200, RoundedCornerShape(24.dp))
          .clickable { navController.navigate("coach") }
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BlueBg),
              contentAlignment = Alignment.Center
            ) {
              Icon(imageVector = Icons.Default.MonitorHeart, contentDescription = "Coach", tint = Blue700)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "AI Wellness & Sleep Coach", color = TextPrimary, fontWeight = FontWeight.Bold)
              Text(text = "Optimize your lifestyle", color = Slate500, fontSize = 12.sp)
            }
          }
          Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
        }
      }

      // Workout Module
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Color.White)
          .border(1.dp, Slate200, RoundedCornerShape(24.dp))
          .clickable { navController.navigate("workout") }
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OrangeBg),
              contentAlignment = Alignment.Center
            ) {
              Icon(imageVector = Icons.Default.FitnessCenter, contentDescription = "Workout", tint = Orange700)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "AI Workout", color = TextPrimary, fontWeight = FontWeight.Bold)
              Text(text = "Generate and track routines", color = Slate500, fontSize = 12.sp)
            }
          }
          Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
        }
      }

      // Medicinal Recipes Module
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(Color.White)
          .border(1.dp, Slate200, RoundedCornerShape(24.dp))
          .clickable { navController.navigate("recipe") }
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Red100),
              contentAlignment = Alignment.Center
            ) {
              Icon(imageVector = Icons.Default.LocalDining, contentDescription = "Recipe", tint = Red700)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(text = "AI Medicinal Recipes", color = TextPrimary, fontWeight = FontWeight.Bold)
              Text(text = "Premium LCHF functional foods", color = Slate500, fontSize = 12.sp)
            }
          }
          Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Go", tint = Slate400)
        }
      }
    }
  }

  if (showWeightDialog) {
    var weightInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showWeightDialog = false },
      title = { Text("Log Weight") },
      text = {
        OutlinedTextField(
          value = weightInput,
          onValueChange = { weightInput = it },
          label = { Text("Weight (kg)") },
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val w = weightInput.toFloatOrNull()
          if (w != null) {
            viewModel.setWeight(w)
          }
          showWeightDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showWeightDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showWaterDialog) {
    var waterInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showWaterDialog = false },
      title = { Text("Log Water") },
      text = {
        OutlinedTextField(
          value = waterInput,
          onValueChange = { waterInput = it },
          label = { Text("Water (Liters)") },
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val w = waterInput.toFloatOrNull()
          if (w != null) {
            viewModel.addWater(w)
          }
          showWaterDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showWaterDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showSleepDialog) {
    var sleepInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showSleepDialog = false },
      title = { Text("Log Sleep") },
      text = {
        OutlinedTextField(
          value = sleepInput,
          onValueChange = { sleepInput = it },
          label = { Text("Sleep (Hours)") },
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val s = sleepInput.toFloatOrNull()
          if (s != null) {
            viewModel.setSleep(s)
          }
          showSleepDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSleepDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showBpDialog) {
    var bpInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showBpDialog = false },
      title = { Text("Log Blood Pressure") },
      text = {
        OutlinedTextField(
          value = bpInput,
          onValueChange = { bpInput = it },
          label = { Text("Systolic/Diastolic (e.g. 120/80)") }
        )
      },
      confirmButton = {
        TextButton(onClick = {
          if (bpInput.isNotBlank()) {
            viewModel.setBloodPressure(bpInput)
          }
          showBpDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showBpDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showBmiDialog) {
    var weightInput by remember { mutableStateOf(profile?.weightKg?.toString() ?: "") }
    var heightInput by remember { mutableStateOf(profile?.heightCm?.toString() ?: "") }
    
    val w = weightInput.toFloatOrNull() ?: 0f
    val h = heightInput.toFloatOrNull()?.div(100f) ?: 0f
    val calcBmi = if (h > 0) w / (h * h) else 0f
    
    AlertDialog(
      onDismissRequest = { showBmiDialog = false },
      title = { Text("BMI Calculator") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("Weight (kg)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
          )
          OutlinedTextField(
            value = heightInput,
            onValueChange = { heightInput = it },
            label = { Text("Height (cm)") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
          )
          Spacer(modifier = Modifier.height(16.dp))
          Text(text = "BMI: ${String.format("%.1f", calcBmi)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Emerald700)
          val status = when {
            calcBmi == 0f -> ""
            calcBmi < 18.5f -> "Underweight"
            calcBmi < 25f -> "Normal"
            calcBmi < 30f -> "Overweight"
            else -> "Obese"
          }
          Text(text = status, fontSize = 16.sp, color = Slate600)
        }
      },
      confirmButton = {
        TextButton(onClick = { showBmiDialog = false }) {
          Text("Close")
        }
      }
    )
  }

  if (showStepsOptionDialog) {
    AlertDialog(
      onDismissRequest = { showStepsOptionDialog = false },
      title = { Text("Log Steps") },
      text = { Text("How would you like to update your step count today?") },
      confirmButton = {
        TextButton(onClick = {
            showStepsOptionDialog = false
            android.widget.Toast.makeText(navController.context, "Syncing steps via Health Connect...", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.syncWithHealthConnect(navController.context)
        }) {
          Text("Sync Device Steps")
        }
      },
      dismissButton = {
        TextButton(onClick = { 
            showStepsOptionDialog = false
            showStepsDialog = true 
        }) {
          Text("Enter Manually")
        }
      }
    )
  }

  if (showStepsDialog) {
    var stepsInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showStepsDialog = false },
      title = { Text("Log Steps Manually") },
      text = {
        OutlinedTextField(
          value = stepsInput,
          onValueChange = { stepsInput = it },
          label = { Text("Steps walked") },
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val s = stepsInput.toIntOrNull()
          if (s != null) {
            viewModel.addSteps(s)
          }
          showStepsDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showStepsDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  if (showWaterDialog) {
    var waterInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showWaterDialog = false },
      title = { Text("Log Water") },
      text = {
        OutlinedTextField(
          value = waterInput,
          onValueChange = { waterInput = it },
          label = { Text("Water (Liters)") },
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val w = waterInput.toFloatOrNull()
          if (w != null) {
            viewModel.addWater(w)
          }
          showWaterDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showWaterDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
}
@Composable
fun BottomNavBar(navController: NavHostController, currentRoute: String?) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color.White)
      .border(1.dp, Slate100)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceAround,
    verticalAlignment = Alignment.CenterVertically
  ) {
    NavItem(
      icon = Icons.Default.Home, 
      label = "Home", 
      isSelected = currentRoute == "dashboard",
      onClick = {
        navController.navigate("dashboard") { launchSingleTop = true }
      }
    )
    NavItem(
      icon = Icons.Default.ChatBubbleOutline, 
      label = "Chat", 
      isSelected = currentRoute == "chat",
      onClick = {
        navController.navigate("chat") { launchSingleTop = true }
      }
    )
    NavItem(
      icon = Icons.Default.RestaurantMenu, 
      label = "Diet Plan", 
      isSelected = currentRoute == "dietplan",
      onClick = {
        navController.navigate("dietplan") { launchSingleTop = true }
      }
    )
    NavItem(
      icon = Icons.Default.MenuBook, 
      label = "Protocol", 
      isSelected = currentRoute == "lifestyle",
      onClick = {
        navController.navigate("lifestyle") { launchSingleTop = true }
      }
    )
  }
}

@Composable
fun NavItem(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
  val color = if (isSelected) Emerald600 else Slate400
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.clickable { onClick() }
  ) {
    Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
    Spacer(modifier = Modifier.height(4.dp))
    Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
  }
}

