package com.example.firebaseauth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.firebaseauth.ui.theme.LoginScreen
import com.example.firebaseauth.ui.theme.SignUpScreen
import com.example.firebaseauth.ui.theme.FireBaseAuthTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "signup") {
                composable("signup") {
                    SignUpScreen(
                        signUp = { email, password -> signUp(email, password, navController) },
                        navController = navController
                    )
                }
                composable("login") {
                    LoginScreen(
                        signIn = { email, password -> signIn(email, password) },
                        navController = navController
                    )
                }
            }
        }
    }

    private fun signUp(email: String, password: String, navController: NavController) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("User created: ${auth.currentUser}")
                    navController.navigate("login")
                } else {
                    println("User couldn't be created: ${task.exception?.message}")
                }
            }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    println("User Logged In: ${auth.currentUser?.email}")
                } else {
                    println("User couldn't be logged in: ${task.exception?.message}")
                }
            }
    }
}

