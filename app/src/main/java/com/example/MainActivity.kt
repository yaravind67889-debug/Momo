package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ApprovalDialog
import com.example.ui.navigation.MomoScreen
import com.example.ui.screens.BrowserScreen
import com.example.ui.screens.ChatAgentScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.MemorySnippetsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MomoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MomoApp()
            }
        }
    }
}

@Composable
fun MomoApp(
    viewModel: MomoViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(MomoScreen.CHAT) }
    var showMemorySnippets by remember { mutableStateOf(false) }
    val activeTasks by viewModel.activeTasks.collectAsStateWithLifecycle()
    val pendingApprovalTask by viewModel.pendingApprovalTask.collectAsStateWithLifecycle()

    // BackHandler to return to CHAT from any sub-screen
    BackHandler(enabled = !showMemorySnippets && currentScreen != MomoScreen.CHAT) {
        currentScreen = MomoScreen.CHAT
    }

    if (showMemorySnippets) {
        MemorySnippetsScreen(
            viewModel = viewModel,
            onNavigateBack = { showMemorySnippets = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    MomoScreen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        val activeCount = activeTasks.size
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                if (screen == MomoScreen.TASKS && activeCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$activeCount") } }) {
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            val screenModifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

            when (currentScreen) {
                MomoScreen.CHAT -> ChatAgentScreen(
                    viewModel = viewModel,
                    onNavigateToBrowser = { currentScreen = MomoScreen.BROWSER },
                    onOpenMemorySnippets = { showMemorySnippets = true },
                    modifier = screenModifier
                )
                MomoScreen.TASKS -> TasksScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MomoScreen.BROWSER -> BrowserScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MomoScreen.MEMORY -> MemoryScreen(
                    viewModel = viewModel,
                    onOpenSnippets = { showMemorySnippets = true },
                    modifier = screenModifier
                )
                MomoScreen.FILES -> FilesScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
                MomoScreen.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = screenModifier
                )
            }
        }
    }

    // Modal Approval Dialog for sensitive actions
    pendingApprovalTask?.let { task ->
        ApprovalDialog(
            task = task,
            onApprove = { viewModel.approveAction(task.id) },
            onReject = { viewModel.rejectAction(task.id) }
        )
    }
}
