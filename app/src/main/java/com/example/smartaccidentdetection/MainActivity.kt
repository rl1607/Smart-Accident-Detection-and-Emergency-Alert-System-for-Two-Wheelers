package com.example.smartaccidentdetection

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.maps.MapsInitializer
import com.example.smartaccidentdetection.ui.navigation.AppNavigation
import com.example.smartaccidentdetection.ui.theme.SmartAccidentDetectionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            MapsInitializer.initialize(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enableEdgeToEdge()
        setContent {
            SmartAccidentDetectionTheme {
                AppNavigation()
            }
        }
    }
}