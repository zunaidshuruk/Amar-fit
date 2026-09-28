package com.example.presentation.fitness

import androidx.compose.runtime.Composable
import com.example.presentation.viewmodel.ShasthoViewModel
import com.example.presentation.workout.WorkoutScreen

@Composable
fun FitnessScreen(viewModel: ShasthoViewModel) {
    WorkoutScreen(viewModel = viewModel)
}
