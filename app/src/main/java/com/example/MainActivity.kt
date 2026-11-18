package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.olympiades.firststep.FirstStepApplication
import com.olympiades.firststep.ui.navigation.FirstStepAppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FirstStepApplication).container
        setContent {
            FirstStepAppRoot(container = container)
        }
    }
}
