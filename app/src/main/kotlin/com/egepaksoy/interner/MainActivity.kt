package com.egepaksoy.interner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.egepaksoy.interner.ui.AppNav
import com.egepaksoy.interner.ui.theme.InternerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            InternerTheme {
                AppNav()
            }
        }
    }
}
