package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.MomoViewModel

@Composable
fun SettingsScreen(
    viewModel: MomoViewModel,
    modifier: Modifier = Modifier
) {
    val currentProvider by viewModel.aiProvider.collectAsStateWithLifecycle()
    val currentApiKey by viewModel.aiApiKey.collectAsStateWithLifecycle()
    val currentModelName by viewModel.aiModelName.collectAsStateWithLifecycle()
    val currentCustomEndpoint by viewModel.aiCustomEndpoint.collectAsStateWithLifecycle()

    val currentHfUrl by viewModel.hfSpaceUrl.collectAsStateWithLifecycle()
    val currentHfToken by viewModel.hfSpaceToken.collectAsStateWithLifecycle()
    val hfStatus by viewModel.hfHealthStatus.collectAsStateWithLifecycle()

    var provider by remember { mutableStateOf(currentProvider) }
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var modelName by remember { mutableStateOf(currentModelName) }
    var customEndpoint by remember { mutableStateOf(currentCustomEndpoint) }
    var hideKey by remember { mutableStateOf(true) }

    var hfUrl by remember { mutableStateOf(currentHfUrl) }
    var hfToken by remember { mutableStateOf(currentHfToken) }
    var hideToken by remember { mutableStateOf(true) }

    var saveFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentProvider, currentApiKey, currentModelName, currentCustomEndpoint) {
        provider = currentProvider
        apiKey = currentApiKey
        modelName = currentModelName
        customEndpoint = currentCustomEndpoint
    }

    LaunchedEffect(currentHfUrl, currentHfToken) {
        hfUrl = currentHfUrl
        hfToken = currentHfToken
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Settings & Integrations",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Configure AI model providers, Hugging Face Space, and Google Drive.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Save status feedback
        if (saveFeedback != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = saveFeedback ?: "",
                    color = Color(0xFF15803D),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Section 1: AI Provider Configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Reasoning Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "Select provider for natural language task planning & tool selection:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Autonomous Local", "Gemini", "OpenAI Compatible").forEach { p ->
                        FilterChip(
                            selected = provider == p,
                            onClick = { provider = p },
                            label = { Text(p, fontSize = 11.sp) }
                        )
                    }
                }

                if (provider != "Autonomous Local") {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("API Key") },
                        visualTransformation = if (hideKey) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            Button(
                                onClick = { hideKey = !hideKey },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                            ) {
                                Text(if (hideKey) "Show" else "Hide", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_api_key_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Name (e.g. gemini-1.5-flash, gpt-4o-mini)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (provider == "OpenAI Compatible") {
                        OutlinedTextField(
                            value = customEndpoint,
                            onValueChange = { customEndpoint = it },
                            label = { Text("Base URL (e.g. https://openrouter.ai/api/v1)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFF6FF))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Autonomous Local mode works with zero configuration, using Momo's built-in intelligent rule planner. Add a Gemini API key anytime for deep reasoning.",
                            fontSize = 12.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }

                Button(
                    onClick = {
                        viewModel.saveAiSettings(provider, apiKey, modelName, customEndpoint)
                        saveFeedback = "AI Settings saved successfully!"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_ai_settings_btn")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save AI Settings")
                }
            }
        }

        // Section 2: Hugging Face Browser Space Configuration
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hugging Face Browser Space",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "Controls remote Playwright Chromium Docker instance:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = hfUrl,
                    onValueChange = { hfUrl = it },
                    label = { Text("HF Space URL (e.g. https://username-momo-space.hf.space)") },
                    placeholder = { Text("https://my-momo-browser.hf.space") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hf_space_url_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = hfToken,
                    onValueChange = { hfToken = it },
                    label = { Text("Bearer Auth Token (HF_API_TOKEN)") },
                    visualTransformation = if (hideToken) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        Button(
                            onClick = { hideToken = !hideToken },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                        ) {
                            Text(if (hideToken) "Show" else "Hide", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveHfSettings(hfUrl, hfToken)
                            saveFeedback = "Hugging Face Space configured & pinged!"
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save & Connect")
                    }

                    Button(
                        onClick = { viewModel.checkHfHealth() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Health", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }

        // Section 3: Safety & Permission Defaults
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Safety & Permission System",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Text(
                    text = "Owner Safeguards Active:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "✓ Owner Confirmation required before payments or financial transactions\n✓ Approval required before sending emails or messages\n✓ Approval required before deleting persistent files\n✓ Never logs passwords or sensitive credentials into memory\n✓ Tasks checkpointed in Room database at each step",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
