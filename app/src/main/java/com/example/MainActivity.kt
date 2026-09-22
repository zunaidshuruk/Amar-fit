package com.example

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import com.example.presentation.auth.AuthScreen
import com.example.presentation.badges.BadgeGalleryScreen
import com.example.presentation.chat.ChatScreen
import com.example.presentation.coach.CoachScreen
import com.example.presentation.fitness.FitnessScreen
import com.example.presentation.foodlog.FoodLogScreen
import com.example.presentation.health.HealthScreen
import com.example.presentation.lifestyle.LifestyleScreen
import com.example.presentation.mealplan.DietChartScreen
import com.example.presentation.mealplan.MealPlanScreen
import com.example.presentation.metrics.GlucoseHistoricalEntryScreen
import com.example.presentation.metrics.GlucoseLogScreen
import com.example.presentation.metrics.WeightLogScreen
import com.example.presentation.notifications.NotificationHelper
import com.example.presentation.nutrition.NutritionScreen
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.recipe.RecipeScreen
import com.example.presentation.scanner.ScannerScreen
import com.example.presentation.settings.HealthGoalsScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.sleep.SleepScreen
import com.example.presentation.today.TodayScreen
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.presentation.workout.ExerciseLibraryScreen
import com.example.data.health.HealthConnectManager
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

val LocalNavController = androidx.compose.runtime.staticCompositionLocalOf<NavHostController?> { null }

sealed class TabScreen(val route: String, val label: String, val icon: ImageVector) {
    object Today : TabScreen("today", "Today", Icons.Rounded.Home)
    object Fitness : TabScreen("fitness", "Fitness", Icons.Rounded.FitnessCenter)
    object Nutrition : TabScreen("nutrition", "Nutrition", Icons.Rounded.RestaurantMenu)
    object Sleep : TabScreen("sleep", "Sleep", Icons.Rounded.Bedtime)
    object Health : TabScreen("health", "Health", Icons.Rounded.Favorite)
}

val bottomTabs = listOf(
    TabScreen.Today,
    TabScreen.Fitness,
    TabScreen.Nutrition,
    TabScreen.Sleep,
    TabScreen.Health
)

class MainActivity : ComponentActivity(), SensorEventListener {
  private val viewModel: ShasthoViewModel by viewModels()
  private var sensorManager: SensorManager? = null
  private var stepSensor: Sensor? = null
  private var isSensorRegistered = false
  private var pendingNavigationRoute by mutableStateOf<String?>(null)

  @OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pendingNavigationRoute = intent.getStringExtra("NAVIGATE_TO")
    
    NotificationHelper.createNotificationChannel(this)
    sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    enableEdgeToEdge()
    setContent {
      val userProfile by viewModel.userProfile.collectAsState()
      val isDark = true
      MyApplicationTheme(darkTheme = true) {
        val permissionState = rememberMultiplePermissionsState(
            permissions = listOf(
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.ACTIVITY_RECOGNITION
            )
        )
        LaunchedEffect(Unit) {
            permissionState.launchMultiplePermissionRequest()
        }
        
        LaunchedEffect(permissionState.allPermissionsGranted, userProfile?.remindersEnabled, userProfile?.notificationsEnabled) {
             if (permissionState.allPermissionsGranted) {
                 if (userProfile?.remindersEnabled != false) {
                     com.example.presentation.notifications.ReminderManager.scheduleMealReminders(this@MainActivity)
                 } else {
                     com.example.presentation.notifications.ReminderManager.cancelMealReminders(this@MainActivity)
                 }
                 if (userProfile?.notificationsEnabled != false) {
                     viewModel.checkNutritionalDeficiencies(this@MainActivity)
                 }
             }
        }
        
        val navController = rememberNavController()
        var sessionChecked by remember { mutableStateOf(false) }
        var initialRoute by remember { mutableStateOf("splash") }

        LaunchedEffect(Unit) {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                viewModel.logout {}
                initialRoute = "auth"
                sessionChecked = true
            } else {
                viewModel.syncDataOnLogin { hasValidProfile ->
                    initialRoute = if (hasValidProfile) TabScreen.Today.route else "onboarding"
                    sessionChecked = true
                }
            }
        }

        if (!sessionChecked) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = if (isDark) MaterialTheme.colorScheme.primary else Emerald600)
            }
        } else {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            val isMainTab = bottomTabs.any { it.route == currentRoute }

            LaunchedEffect(pendingNavigationRoute, sessionChecked) {
                val route = pendingNavigationRoute
                if (sessionChecked && route != null) {
                    navController.navigate(route)
                    pendingNavigationRoute = null
                }
            }

            Scaffold(
              modifier = Modifier.fillMaxSize(),
              containerColor = MaterialTheme.colorScheme.background,
              topBar = {
                  if (isMainTab) {
                      TopAppBar(
                          title = { 
                              val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                              val greeting = when (hour) {
                                  in 0..11 -> "Good Morning"
                                  12 -> "Good Noon"
                                  in 13..16 -> "Good Afternoon"
                                  in 17..20 -> "Good Evening"
                                  else -> "Good Night"
                              }
                              val name = (userProfile?.name ?: "Guest").trim().split(" ").firstOrNull { it.isNotBlank() } ?: "Guest"
                              Column {
                                  Text(text = "HEALTH & DIET", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (isDark) MaterialTheme.colorScheme.primary else Emerald700, letterSpacing = 1.sp)
                                  Text(
                                      text = "$greeting, $name",
                                      fontSize = 18.sp,
                                      fontWeight = FontWeight.Bold,
                                      color = MaterialTheme.colorScheme.onBackground,
                                      maxLines = 1,
                                      overflow = TextOverflow.Ellipsis
                                  )
                              }
                          },
                          actions = {
                              val context = androidx.compose.ui.platform.LocalContext.current
                              var isHealthConnectAvailable by remember { mutableStateOf(HealthConnectManager.isAvailable(context)) }
                              
                              if (isHealthConnectAvailable) {
                                  val coroutineScope = rememberCoroutineScope()
                                  IconButton(
                                      onClick = {
                                          coroutineScope.launch {
                                              if (HealthConnectManager.hasAnyPermissions(context)) {
                                                  android.widget.Toast.makeText(context, "Syncing with Health Connect...", android.widget.Toast.LENGTH_SHORT).show()
                                                  viewModel.syncWithHealthConnect(context)
                                              } else {
                                                  android.widget.Toast.makeText(context, "Please connect Health Connect in Profile & Settings first", android.widget.Toast.LENGTH_LONG).show()
                                              }
                                          }
                                      },
                                      modifier = Modifier
                                          .padding(end = 8.dp)
                                          .size(36.dp)
                                          .clip(CircleShape)
                                          .background(MaterialTheme.colorScheme.surface)
                                          .border(1.dp, if (isDark) MaterialTheme.colorScheme.outline else Slate200, CircleShape)
                                  ) {
                                      Icon(Icons.Default.Sync, contentDescription = "Sync Health Connect", tint = if (isDark) MaterialTheme.colorScheme.primary else Emerald600, modifier = Modifier.size(20.dp))
                                  }
                              }

                              Box(
                                  modifier = Modifier
                                      .padding(end = 16.dp)
                                      .size(36.dp)
                                      .clip(CircleShape)
                                      .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Emerald200)
                                      .border(2.dp, if (isDark) MaterialTheme.colorScheme.outline else Color.White, CircleShape)
                                      .shadow(2.dp, CircleShape)
                                      .clickable { navController.navigate("settings") },
                                  contentAlignment = Alignment.Center
                              ) {
                                  if (!userProfile?.profilePictureUri.isNullOrEmpty()) {
                                      var decodedBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                                      if (!userProfile!!.profilePictureUri!!.startsWith("http") && !userProfile!!.profilePictureUri!!.startsWith("content://")) {
                                          try {
                                              val decodedBytes = android.util.Base64.decode(userProfile!!.profilePictureUri, android.util.Base64.DEFAULT)
                                              decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.let { it.asImageBitmap() }
                                          } catch (e: Exception) { }
                                      }
                                      
                                      if (decodedBitmap != null) {
                                          androidx.compose.foundation.Image(
                                              bitmap = decodedBitmap,
                                              contentDescription = "Profile Picture",
                                              modifier = Modifier.fillMaxSize(),
                                              contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                          )
                                      } else {
                                          AsyncImage(
                                              model = userProfile?.profilePictureUri,
                                              contentDescription = "Profile Picture",
                                              modifier = Modifier.fillMaxSize(),
                                              contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                          )
                                      }
                                  } else {
                                      val initial = userProfile?.name?.firstOrNull()?.toString() ?: "G"
                                      Text(
                                          text = initial,
                                          fontWeight = FontWeight.Bold,
                                          color = if (isDark) MaterialTheme.colorScheme.primary else Emerald800,
                                          fontSize = 14.sp
                                      )
                                  }
                              }
                          },
                          colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                      )
                  }
              },
              bottomBar = {
                if (isMainTab) {
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(MaterialTheme.colorScheme.background)
                      .padding(horizontal = 16.dp, vertical = 12.dp)
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), clip = false)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      bottomTabs.forEach { tab ->
                          val isSelected = currentRoute == tab.route
                          val color = if (isSelected) (if (isDark) MaterialTheme.colorScheme.primary else Emerald600) else Slate400
                          val pillColor = if (isSelected) (if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Emerald50) else Color.Transparent
                          Column(
                              horizontalAlignment = Alignment.CenterHorizontally,
                              verticalArrangement = Arrangement.spacedBy(2.dp),
                              modifier = Modifier
                                  .weight(1f)
                                  .clip(RoundedCornerShape(16.dp))
                                  .clickable {
                                      navController.navigate(tab.route) {
                                          popUpTo(navController.graph.findStartDestination().id) {
                                              saveState = true
                                          }
                                          launchSingleTop = true
                                          restoreState = true
                                      }
                                  }
                                  .background(pillColor)
                                  .padding(vertical = 8.dp)
                          ) {
                              Icon(imageVector = tab.icon, contentDescription = tab.label, tint = color, modifier = Modifier.size(20.dp))
                              Text(
                                  text = tab.label,
                                  fontSize = 10.sp,
                                  fontWeight = FontWeight.Bold,
                                  color = color,
                                  maxLines = 1,
                                  softWrap = false,
                                  overflow = TextOverflow.Clip
                              )
                          }
                      }
                    }
                  }
                }
              }
            ) { innerPadding ->
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
                  kotlinx.coroutines.delay(15_000)
                  viewModel.syncWithHealthConnect(context)
                }
              }

              CompositionLocalProvider(LocalNavController provides navController) {
                NavHost(
                  navController = navController,
                  startDestination = initialRoute,
                  modifier = Modifier.padding(innerPadding)
                ) {
                  composable("auth") {
                    AuthScreen(
                      onNavigateToOnboarding = {
                        navController.navigate("onboarding") { popUpTo("auth") { inclusive = true } }
                      },
                      onNavigateToDashboard = {
                        navController.navigate(TabScreen.Today.route) { popUpTo("auth") { inclusive = true } }
                      }
                    )
                  }
                  composable("onboarding") {
                    OnboardingScreen(viewModel = viewModel, onComplete = {
                      navController.navigate(TabScreen.Today.route) { popUpTo("onboarding") { inclusive = true } }
                    })
                  }
                  
                  // MAIN TABS
                  composable(TabScreen.Today.route) { TodayScreen(viewModel, navController) }
                  composable(TabScreen.Fitness.route) { FitnessScreen(viewModel) }
                  composable(TabScreen.Nutrition.route) { NutritionScreen(viewModel, navController) }
                  composable(TabScreen.Sleep.route) { SleepScreen(viewModel, navController) }
                  composable(TabScreen.Health.route) { HealthScreen(viewModel, navController) }
                  
                  // SUB-DESTINATIONS
                  composable(
                      route = "chat?openSavedChats={openSavedChats}",
                      arguments = listOf(androidx.navigation.navArgument("openSavedChats") { type = androidx.navigation.NavType.BoolType; defaultValue = false })
                  ) { backStackEntry ->
                      val openSavedChats = backStackEntry.arguments?.getBoolean("openSavedChats") ?: false
                      ChatScreen(viewModel = viewModel, initialTab = if (openSavedChats) 1 else 0, onNavigateBack = { navController.popBackStack() })
                  }
                  composable("coach") { CoachScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("dietplan") { DietChartScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("lifestyle") { LifestyleScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("mealplan") { MealPlanScreen(onNavigateBack = { navController.popBackStack() }) }
                  composable("glucoselog") { GlucoseLogScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToHistoricalEntry = { navController.navigate("glucose_historical_entry") }) }
                  composable("glucose_historical_entry") { GlucoseHistoricalEntryScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("weightlog") { WeightLogScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("badges") { BadgeGalleryScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("recipe") { RecipeScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("foodlog") { FoodLogScreen(viewModel = viewModel, onNavigateToScanner = { navController.navigate("scanner") }, onNavigateBack = { navController.popBackStack() }) }
                  composable("settings") { SettingsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onLogout = { navController.navigate("auth") { popUpTo(0) { inclusive = true } } }, onNavigateToHealthGoals = { navController.navigate("health_goals") }) }
                  composable("health_goals") { HealthGoalsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("scanner") { ScannerScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("medical_records") { com.example.presentation.health.MedicalRecordsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("exercise_library") { ExerciseLibraryScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("mindfulness_timer") { com.example.presentation.mindfulness.MindfulnessTimerScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable("edit_focus") { com.example.presentation.today.EditFocusScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                  composable(
                      route = "metric_detail/{metricKey}",
                      arguments = listOf(androidx.navigation.navArgument("metricKey") { type = androidx.navigation.NavType.StringType })
                  ) { backStackEntry ->
                      val key = backStackEntry.arguments?.getString("metricKey") ?: ""
                      com.example.presentation.health.MetricDetailScreen(viewModel = viewModel, metricKey = key, onNavigateBack = { navController.popBackStack() })
                  }
                }
              }
            }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
      super.onNewIntent(intent)
      setIntent(intent)
      pendingNavigationRoute = intent.getStringExtra("NAVIGATE_TO")
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
