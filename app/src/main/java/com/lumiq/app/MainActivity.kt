/*
 * Copyright (c) 2026 zxtni
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lumiq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lumiq.app.ui.theme.LUMIQTheme

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumiq.app.viewmodel.EditorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LUMIQTheme {
                val navController = rememberNavController()
                // Shared ViewModel to hold state across navigation (Simple MVP solution)
                // In robust app, pass ID or URI string as Argument.
                // We'll pass URI string as encoded argument.
                
                NavHost(navController = navController, startDestination = "splash") {
                    
                    composable("splash") {
                        SplashScreen(
                            onNavigateToHome = {
                                navController.navigate("home") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }
                    
                    composable("home") {
                        HomeScreen(
                            onImageSelected = { uri ->
                                val encodedUri = Uri.encode(uri.toString())
                                navController.navigate("editor/$encodedUri")
                            }
                        )
                    }
                    
                    composable("editor/{imageUri}") { backStackEntry ->
                        val imageUriString = backStackEntry.arguments?.getString("imageUri")
                        val uri = Uri.parse(Uri.decode(imageUriString))
                        
                        // We need to load this URI into the EditorViewModel
                        // Since EditorScreen instantiates its own VM by default, we need to pass it or init it.
                        // Ideally EditorScreen accepts the Uri.
                        
                        EditorScreen(
                            initialUri = uri,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
