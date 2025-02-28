package com.example.whychat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.example.whychat.ui.HomeScreen
import com.example.whychat.ui.InputScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "inputScreen") {
                composable("inputScreen") {
                    InputScreen(navController)
                }
                composable("homeScreen/{senderId}/{chatGroupId}") { backStackEntry ->
                    val senderId = backStackEntry.arguments?.getString("senderId") ?: "Anonymous"
                    val chatGroupId = backStackEntry.arguments?.getString("chatGroupId") ?: "chat1"
                    HomeScreen(senderId, chatGroupId)
                }
            }
        }
    }


}
