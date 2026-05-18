package com.tinybrowse.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tinybrowse.ui.browser.BrowserScreen
import com.tinybrowse.ui.theme.TinyBrowseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TinyBrowseTheme {
                val viewModel: MainViewModel = viewModel()
                BrowserScreen(viewModel = viewModel)
            }
        }
    }
}
