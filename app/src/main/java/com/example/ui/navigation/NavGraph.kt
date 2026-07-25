package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppSettings
import com.example.ui.RemindCareViewModel
import com.example.ui.screens.*

@Composable
fun RemindCareNavGraph(viewModel: RemindCareViewModel, settings: AppSettings) {
    val navController = rememberNavController()
    
    val startDestination = when (settings.role) {
        "Patient" -> "patient_home"
        "Caregiver" -> "caregiver_dashboard"
        else -> "welcome"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("welcome") {
            WelcomeScreen(
                onRoleSelected = { role ->
                    if (role == "Patient") {
                        navController.navigate("patient_setup")
                    } else {
                        navController.navigate("caregiver_setup")
                    }
                }
            )
        }
        
        composable("patient_setup") {
            PatientSetupScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate("pairing_patient_setup")
                }
            )
        }

        composable("pairing_patient_setup") {
            PatientPairingScreen(
                viewModel = viewModel,
                settings = settings,
                onBack = { 
                    viewModel.setRole("Patient")
                    navController.navigate("patient_home") { popUpTo(0) { inclusive = true } }
                },
                isSetupFlow = true
            )
        }
        
        composable("caregiver_setup") {
            CaregiverSetupScreen(
                viewModel = viewModel,
                onComplete = {
                    navController.navigate("pairing_caregiver_setup")
                }
            )
        }
        
        composable("pairing_caregiver_setup") {
            CaregiverPairingScreen(
                viewModel = viewModel,
                settings = settings,
                onBack = { 
                    viewModel.setRole("Caregiver")
                    navController.navigate("caregiver_dashboard") { popUpTo(0) { inclusive = true } }
                },
                isSetupFlow = true
            )
        }

        composable("patient_home") {
            PatientHomeScreen(
                viewModel = viewModel,
                settings = settings,
                onNavigateToPairing = { navController.navigate("pairing_patient") },
                onNavigateToSettings = { navController.navigate("settings_patient") }
            )
        }

        composable("caregiver_dashboard") {
            CaregiverDashboardScreen(
                viewModel = viewModel,
                settings = settings,
                onAddReminder = { navController.navigate("create_reminder") },
                onNavigateToPairing = { navController.navigate("pairing_caregiver") },
                onNavigateToSettings = { navController.navigate("settings_caregiver") }
            )
        }
        
        composable("create_reminder") {
            CreateReminderScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("pairing_patient") {
            PatientPairingScreen(
                viewModel = viewModel,
                settings = settings,
                onBack = { navController.popBackStack() },
                isSetupFlow = false
            )
        }
        
        composable("pairing_caregiver") {
            CaregiverPairingScreen(
                viewModel = viewModel,
                settings = settings,
                onBack = { navController.popBackStack() },
                isSetupFlow = false
            )
        }
        
        composable("settings_patient") {
            PatientSettingsScreen(
                viewModel = viewModel,
                settings = settings,
                onBack = { navController.popBackStack() },
                onSwitchRole = {
                    viewModel.setRole("")
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                }
            )
        }
        
        composable("settings_caregiver") {
            CaregiverSettingsScreen(
                viewModel = viewModel,
                settings = settings,
                onBack = { navController.popBackStack() },
                onSwitchRole = {
                    viewModel.setRole("")
                    navController.navigate("welcome") { popUpTo(0) { inclusive = true } }
                }
            )
        }
    }
}
