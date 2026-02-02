package com.example.speeddial

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.speeddial.ui.ContactViewModel
import com.example.speeddial.ui.screens.ContactListScreen
import com.example.speeddial.ui.screens.EditContactScreen

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Handle permission denied
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request call permission if not granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: ContactViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = "contactList"
                    ) {
                        composable("contactList") {
                            ContactListScreen(
                                viewModel = viewModel,
                                onAddContact = {
                                    navController.navigate("editContact")
                                },
                                onEditContact = { contactId ->
                                    navController.navigate("editContact/$contactId")
                                }
                            )
                        }
                        composable(
                            route = "editContact/{contactId}",
                            arguments = listOf(
                                navArgument("contactId") {
                                    type = NavType.LongType
                                }
                            )
                        ) { backStackEntry ->
                            EditContactScreen(
                                viewModel = viewModel,
                                contactId = backStackEntry.arguments?.getLong("contactId"),
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("editContact") {
                            EditContactScreen(
                                viewModel = viewModel,
                                contactId = null,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}