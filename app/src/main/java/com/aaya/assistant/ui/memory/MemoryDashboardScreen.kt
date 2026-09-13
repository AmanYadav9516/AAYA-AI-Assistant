package com.aaya.assistant.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.data.model.MemoryItem
import com.aaya.assistant.data.model.RoutineModel
import com.aaya.assistant.data.model.VipContact
import com.aaya.assistant.ui.theme.*

@Composable
fun MemoryDashboardScreen(
    memories: List<MemoryItem>,
    routines: List<RoutineModel>,
    vipContacts: List<VipContact>,
    onDeleteMemory: (MemoryItem) -> Unit,
    onToggleRoutine: (RoutineModel) -> Unit,
    onDeleteVip: (VipContact) -> Unit,
    onAddMemory: (String, String, String) -> Unit,
    onAddVip: (String, String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoBg)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Title & Add Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🧠 Assistant Memory",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary
                )
                Text(
                    text = "Everything AAYA has learned. You have 100% control to edit or forget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = NeonCyan,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Learned Facts (${memories.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Routines (${routines.size})", fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("VIP Contacts (${vipContacts.size})", fontSize = 12.sp) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> MemoriesList(memories, onDeleteMemory)
                1 -> RoutinesList(routines, onToggleRoutine)
                2 -> VipContactsList(vipContacts, onDeleteVip)
            }
        }

        // Add Quick Fact / VIP Button
        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = DeepIndigoBg)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Teach AAYA a New Fact or Relation", color = DeepIndigoBg, fontWeight = FontWeight.Bold)
        }
    }

    if (showAddDialog) {
        AddMemoryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { key, value, category ->
                onAddMemory(key, value, category)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun MemoriesList(memories: List<MemoryItem>, onDelete: (MemoryItem) -> Unit) {
    if (memories.isEmpty()) {
        EmptyStateNotice("No learned habits or facts yet. Speak commands like 'My college starts at 9' or teach AAYA below!")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(memories) { memory ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = memory.key.replace("_", " ").uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = memory.value,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Category: ${memory.category}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        IconButton(onClick = { onDelete(memory) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Forget", tint = ErrorRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoutinesList(routines: List<RoutineModel>, onToggle: (RoutineModel) -> Unit) {
    if (routines.isEmpty()) {
        EmptyStateNotice("No scheduled routines configured. Add sleep or class periods to automate silent mode!")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(routines) { routine ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${routine.startTime} – ${routine.endTime} (${routine.modeType})",
                                fontSize = 13.sp,
                                color = NeonCyan
                            )
                            Text(
                                text = "Auto-DND: ${if (routine.autoSilenceNotifications) "Active" else "Off"} | VIP Ring: ${if (routine.allowVipCalls) "Allowed" else "Muted"}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = routine.isEnabled,
                            onCheckedChange = { onToggle(routine.copy(isEnabled = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VipContactsList(vips: List<VipContact>, onDelete: (VipContact) -> Unit) {
    if (vips.isEmpty()) {
        EmptyStateNotice("No VIP contacts defined yet. Add contacts like Mummy or Papa who can ring during Sleep or Class mode.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vips) { vip ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⭐ ${vip.name} (${vip.relationship})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = vip.phoneNumber,
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Can bypass Sleep Mode & Class Mode",
                                fontSize = 11.sp,
                                color = SuccessGreen
                            )
                        }
                        IconButton(onClick = { onDelete(vip) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove VIP", tint = ErrorRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateNotice(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun AddMemoryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("relationship") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teach AAYA", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("e.g. Relationship ('mother' -> 'MUMMY') or Habit ('college' -> '9 AM to 4 PM')", fontSize = 12.sp, color = TextSecondary)
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Keyword / Relation (e.g. mother)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Contact Name / Detail (e.g. MUMMY)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (key.isNotBlank() && value.isNotBlank()) onSave(key, value, category) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Remember", color = DeepIndigoBg, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceDark
    )
}
