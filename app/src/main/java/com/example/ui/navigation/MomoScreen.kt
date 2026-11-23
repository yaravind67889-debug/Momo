package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.ui.graphics.vector.ImageVector

enum class MomoScreen(
    val title: String,
    val icon: ImageVector
) {
    CHAT("Momo", Icons.Filled.AutoAwesome),
    TASKS("Tasks", Icons.Filled.TaskAlt),
    BROWSER("Browser", Icons.Filled.Language),
    MEMORY("Memory", Icons.Filled.Psychology),
    FILES("Files", Icons.Filled.Folder),
    SETTINGS("Settings", Icons.Filled.Settings)
}
