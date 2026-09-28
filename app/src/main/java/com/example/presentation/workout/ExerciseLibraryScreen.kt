package com.example.presentation.workout

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.data.local.LibraryExercise
import com.example.data.model.WorkoutExercise
import com.example.data.model.WorkoutPlan
import com.example.data.remote.RetrofitClient
import com.example.presentation.viewmodel.ShasthoViewModel

private const val BASE_IMAGE_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"

@Composable
fun ExerciseLibraryScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var allExercises by remember { mutableStateOf<List<LibraryExercise>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedEquipment by remember { mutableStateOf<String?>(null) }
    var selectedMuscle by remember { mutableStateOf<String?>(null) }

    var selectedExerciseIds by remember { mutableStateOf(setOf<String>()) }
    var viewingExercise by remember { mutableStateOf<LibraryExercise?>(null) }
    var showWorkoutTitleDialog by remember { mutableStateOf(false) }
    var workoutTitleInput by remember { mutableStateOf("Custom Workout") }

    LaunchedEffect(Unit) {
        allExercises = viewModel.getExerciseLibrary(context)
        isLoading = false
    }

    val equipmentOptions = remember(allExercises) {
        allExercises.mapNotNull { it.equipment?.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    val muscleOptions = remember(allExercises) {
        allExercises.flatMap { it.primaryMuscles }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    val filteredExercises = remember(allExercises, searchQuery, selectedEquipment, selectedMuscle) {
        val q = searchQuery.trim().lowercase()
        allExercises.filter { exercise ->
            val matchesSearch = q.isEmpty() || exercise.name.lowercase().contains(q)
            val matchesEquipment = selectedEquipment == null || exercise.equipment.equals(selectedEquipment, ignoreCase = true)
            val matchesMuscle = selectedMuscle == null || exercise.primaryMuscles.any { it.equals(selectedMuscle, ignoreCase = true) }
            matchesSearch && matchesEquipment && matchesMuscle
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exercise Library",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Search field reusing CoachScreen pattern
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search exercises...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Equipment Filter Chip Row
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text(
                        text = "Equipment",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedEquipment == null,
                                onClick = { selectedEquipment = null },
                                label = { Text("All") }
                            )
                        }
                        items(equipmentOptions) { equipment ->
                            FilterChip(
                                selected = selectedEquipment.equals(equipment, ignoreCase = true),
                                onClick = {
                                    selectedEquipment = if (selectedEquipment.equals(equipment, ignoreCase = true)) null else equipment
                                },
                                label = { Text(equipment.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }

                // Muscle Filter Chip Row
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Text(
                        text = "Primary Muscle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedMuscle == null,
                                onClick = { selectedMuscle = null },
                                label = { Text("All") }
                            )
                        }
                        items(muscleOptions) { muscle ->
                            FilterChip(
                                selected = selectedMuscle.equals(muscle, ignoreCase = true),
                                onClick = {
                                    selectedMuscle = if (selectedMuscle.equals(muscle, ignoreCase = true)) null else muscle
                                },
                                label = { Text(muscle.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (selectedExerciseIds.isNotEmpty()) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedExerciseIds.size} selected",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(
                            onClick = {
                                workoutTitleInput = "Custom Workout"
                                showWorkoutTitleDialog = true
                            }
                        ) {
                            Text("Add to Workout")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredExercises.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No exercises found matching filters",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(filteredExercises, key = { it.id }) { exercise ->
                    val isSelected = selectedExerciseIds.contains(exercise.id)
                    val firstImage = exercise.images.firstOrNull()
                    val imageUrl = if (firstImage != null) BASE_IMAGE_URL + firstImage else null

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewingExercise = exercise },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedExerciseIds = if (checked) {
                                        selectedExerciseIds + exercise.id
                                    } else {
                                        selectedExerciseIds - exercise.id
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Thumbnail with graceful fallback
                            SubcomposeAsyncImage(
                                model = imageUrl,
                                contentDescription = exercise.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    }
                                },
                                error = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = exercise.category.replaceFirstChar { it.uppercase() },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )

                                    val primaryMuscle = exercise.primaryMuscles.firstOrNull()
                                    if (!primaryMuscle.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = primaryMuscle.replaceFirstChar { it.uppercase() },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    viewingExercise?.let { exercise ->
        AlertDialog(
            onDismissRequest = { viewingExercise = null },
            title = {
                Text(
                    text = exercise.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Images if present
                    if (exercise.images.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(exercise.images) { imgPath ->
                                SubcomposeAsyncImage(
                                    model = BASE_IMAGE_URL + imgPath,
                                    contentDescription = exercise.name,
                                    modifier = Modifier
                                        .size(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        }
                                    },
                                    error = {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Metadata details
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (exercise.level.isNotBlank()) {
                            Text(
                                text = "Level: ${exercise.level.replaceFirstChar { it.uppercase() }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (!exercise.equipment.isNullOrBlank()) {
                            Text(
                                text = "Equipment: ${exercise.equipment.replaceFirstChar { it.uppercase() }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (exercise.primaryMuscles.isNotEmpty()) {
                            Text(
                                text = "Primary Muscles: ${exercise.primaryMuscles.joinToString { it.replaceFirstChar { c -> c.uppercase() } }}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (exercise.secondaryMuscles.isNotEmpty()) {
                            Text(
                                text = "Secondary Muscles: ${exercise.secondaryMuscles.joinToString { it.replaceFirstChar { c -> c.uppercase() } }}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Instructions
                    if (exercise.instructions.isNotEmpty()) {
                        Text(
                            text = "Instructions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        exercise.instructions.forEachIndexed { index, step ->
                            Text(
                                text = "${index + 1}. $step",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingExercise = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Workout Title Dialog
    if (showWorkoutTitleDialog) {
        AlertDialog(
            onDismissRequest = { showWorkoutTitleDialog = false },
            title = { Text("Workout Title") },
            text = {
                OutlinedTextField(
                    value = workoutTitleInput,
                    onValueChange = { workoutTitleInput = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = workoutTitleInput.trim().ifBlank { "Custom Workout" }
                        val selectedObjs = allExercises.filter { selectedExerciseIds.contains(it.id) }
                        val mainExercises = selectedObjs.map { ex ->
                            WorkoutExercise(
                                name = ex.name,
                                sets = 3,
                                reps = "10",
                                durationSeconds = 0,
                                restSeconds = 30,
                                youtubeSearchQuery = ex.name,
                                metValue = 3.5
                            )
                        }
                        val plan = WorkoutPlan(
                            title = title,
                            warmup = emptyList(),
                            mainExercises = mainExercises,
                            cooldown = emptyList()
                        )
                        val adapter = RetrofitClient.moshi.adapter(WorkoutPlan::class.java)
                        val json = adapter.toJson(plan)
                        viewModel.saveWorkout(title = title, content = "", structuredJson = json)

                        selectedExerciseIds = emptySet()
                        showWorkoutTitleDialog = false
                        Toast.makeText(context, "Workout saved to Saved Workouts!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorkoutTitleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
