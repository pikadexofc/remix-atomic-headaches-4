package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AtomicHeadachesApp
import com.example.ui.NoteViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(dynamicColor = false) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = com.example.ui.SlateBg
        ) {
          val noteViewModel: NoteViewModel = viewModel()
          AtomicHeadachesApp(viewModel = noteViewModel)
        }
      }
    }
  }
}
