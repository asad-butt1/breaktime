package com.asad.breaktime.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.asad.breaktime.data.BreakStore
import com.asad.breaktime.service.BreakScheduler

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BreakTimeTheme {
                val store = remember { BreakStore.get(this) }
                var onboarded by remember { mutableStateOf(store.onboardingDone) }
                if (!onboarded) {
                    Surface(Modifier.fillMaxSize()) {
                        OnboardingScreen(onFinish = {
                            store.onboardingDone = true
                            onboarded = true
                        })
                    }
                } else {
                    MainTabs()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BreakScheduler.finishIfDue(this)
    }
}

@Composable
private fun MainTabs() {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0, onClick = { tab = 0 },
                                icon = { Icon(Icons.Filled.Timer, contentDescription = null) },
                                label = { Text("Break") },
                            )
                            NavigationBarItem(
                                selected = tab == 1, onClick = { tab = 1 },
                                icon = { Icon(Icons.Filled.Apps, contentDescription = null) },
                                label = { Text("Apps") },
                            )
                            NavigationBarItem(
                                selected = tab == 2, onClick = { tab = 2 },
                                icon = { Icon(Icons.Filled.Insights, contentDescription = null) },
                                label = { Text("Progress") },
                            )
                        }
                    }
                ) { padding ->
                    val modifier = Modifier.padding(padding)
                    when (tab) {
                        0 -> BreakScreen(modifier, onPickApps = { tab = 1 })
                        1 -> AppsScreen(modifier, onDone = { tab = 0 })
                        else -> HistoryScreen(modifier)
                    }
                }
            }
