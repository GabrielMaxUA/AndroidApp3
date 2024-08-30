package com.trios2024aa.treasurehunt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.trios2024aa.treasurehunt.ui.theme.TreasureHuntTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TreasureHuntTheme {
                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(title = { Text("Treasure Hunt") })
                    },
                    content = { paddingValues ->
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            TreasureList()
                        }
                    }
                )
            }
        }
    }
}
