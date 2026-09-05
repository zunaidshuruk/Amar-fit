import sys

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

target = """        val navController = rememberNavController()
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
          ) {"""

replacement = """        val navController = rememberNavController()
        var sessionChecked by remember { mutableStateOf(false) }
        var initialRoute by remember { mutableStateOf("splash") }

        LaunchedEffect(Unit) {
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                // Clear any cached profile to prevent leakage
                viewModel.logout {}
                initialRoute = "auth"
                sessionChecked = true
            } else {
                viewModel.syncDataOnLogin { hasValidProfile ->
                    initialRoute = if (hasValidProfile) "dashboard" else "onboarding"
                    sessionChecked = true
                }
            }
        }

        if (!sessionChecked) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Emerald600)
            }
        } else {
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
                startDestination = initialRoute,
                modifier = Modifier.padding(innerPadding)
              ) {"""

# Replace without exact whitespace matching using regex
import re
# Normalize both to ignore trailing spaces on blank lines
def norm(s):
    return re.sub(r'[ \t]+\n', '\n', s)

content = norm(content)
target = norm(target)
replacement = norm(replacement)

if target in content:
    content = content.replace(target, replacement)
    
    # Also we need to add the closing brace for the `} else {` block
    # We can inject it right before `override fun onResume()`
    content = content.replace("  override fun onResume() {", "      }\n    }\n  }\n\n  override fun onResume() {")
    
    with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
        f.write(content)
    print("Patched successfully")
else:
    print("Target not found")
