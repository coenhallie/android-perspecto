package com.example.perspecto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.perspecto.di.SupabaseModule
import com.example.perspecto.ui.auth.AuthScreen
import com.example.perspecto.ui.auth.BiometricPromptManager
import com.example.perspecto.ui.home.MainScreen
import com.example.perspecto.ui.theme.PerspectoTheme
import com.example.perspecto.ui.video.VideoPlayerScreen
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : AppCompatActivity() {
    private val promptManager by lazy { BiometricPromptManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PerspectoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    val biometricResult by promptManager.promptResults.collectAsState(initial = null)
                    
                    val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    val isBiometricEnabled = sharedPrefs.getBoolean("biometric_enabled", false)

                    LaunchedEffect(Unit) {
                        val session = SupabaseModule.client.auth.currentSessionOrNull()
                        if (session != null) {
                            if (isBiometricEnabled) {
                                promptManager.showBiometricPrompt(
                                    title = "Login",
                                    description = "Authenticate to continue"
                                )
                            } else {
                                startDestination = "main"
                            }
                        } else {
                            startDestination = "auth"
                        }
                    }

                    LaunchedEffect(biometricResult) {
                        if (biometricResult is BiometricPromptManager.BiometricResult.AuthenticationSuccess) {
                            startDestination = "main"
                        } else if (biometricResult is BiometricPromptManager.BiometricResult.AuthenticationError || 
                                   biometricResult is BiometricPromptManager.BiometricResult.AuthenticationFailed) {
                             // If biometric fails, maybe we should just stay on a "locked" screen or go to auth?
                             // For now, let's go to auth if it's a hard error, or just show toast.
                             // If it was an initial check, we might want to fallback to login.
                             if (startDestination == null) {
                                 startDestination = "auth"
                             }
                        }
                    }

                    if (startDestination != null) {
                        NavHost(navController = navController, startDestination = startDestination!!) {
                            composable("auth") {
                                AuthScreen(
                                    onAuthSuccess = {
                                        navController.navigate("main") {
                                            popUpTo("auth") { inclusive = true }
                                        }
                                    },
                                    promptManager = promptManager
                                )
                            }
                            composable(
                                "main",
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) }
                            ) {
                                MainScreen(
                                    onVideoClick = { videoId ->
                                        navController.navigate("video_player/$videoId")
                                    },
                                    onAnnotationClick = { videoId, annotationId ->
                                        navController.navigate("video_player/$videoId?annotationId=$annotationId")
                                    },
                                    onProfileClick = {
                                        navController.navigate("profile")
                                    }
                                )
                            }
                            composable(
                                route = "video_player/{videoId}?annotationId={annotationId}",
                                arguments = listOf(
                                    navArgument("videoId") { type = NavType.StringType },
                                    navArgument("annotationId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                ),
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                            ) { backStackEntry ->
                                val videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                                val annotationId = backStackEntry.arguments?.getString("annotationId")
                                VideoPlayerScreen(
                                    videoId = videoId,
                                    annotationId = annotationId,
                                    onBackClick = { navController.popBackStack() }
                                )
                            }
                            composable(
                                route = "profile",
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
                            ) {
                                com.example.perspecto.ui.profile.ProfileScreen(
                                    onBackClick = { navController.popBackStack() },
                                    onSignOut = {
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}