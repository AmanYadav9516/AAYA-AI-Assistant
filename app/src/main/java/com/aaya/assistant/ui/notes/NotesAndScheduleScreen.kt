package com.aaya.assistant.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaya.assistant.data.model.AuditLogItem
import com.aaya.assistant.data.model.NoteItem
import com.aaya.assistant.data.model.ScheduledTask
import com.aaya.assistant.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesAndScheduleScreen(
    notes: List<NoteItem>,
    shoppingList: List<NoteItem>,
    scheduledTasks: List<ScheduledTask>,
    auditLogs: List<AuditLogItem>,
    onToggleShoppingItem: (NoteItem) -> Unit,
    onDeleteNote: (NoteItem) -> Unit,
    onDeleteScheduledTask: (ScheduledTask) -> Unit,
    onAddNote: (String, String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Notes", "Shopping", "Scheduled", "Audit Log")

    var showAddNoteDialog by remember { mutableStateOf(false) }
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }
    var newNoteCategory by remember { mutableStateOf("Notes") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Personal Hub",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Text(
                    text = "Smart Notes, Lists & Scheduled Actions",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            if (selectedTab == 0 || selectedTab == 1) {
                IconButton(
                    onClick = {
                        newNoteCategory = if (selectedTab == 1) "Shopping" else "Notes"
                        showAddNoteDialog = true
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = NeonCyan)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tab Selector Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = NeonCyan,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) NeonCyan else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> NotesTabContent(notes.filter { it.category != "Shopping" }, onDeleteNote)
            1 -> ShoppingTabContent(shoppingList, onToggleShoppingItem, onDeleteNote)
            2 -> ScheduledTabContent(scheduledTasks, onDeleteScheduledTask)
            3 -> AuditLogTabContent(auditLogs)
        }
    }

    if (showAddNoteDialog) {
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            containerColor = SurfaceDark,
            title = { Text(if (selectedTab == 1) "Add Shopping Item" else "New Note", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newNoteTitle,
                        onValueChange = { newNoteTitle = it },
                        label = { Text("Title / Item") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNoteContent,
                        onValueChange = { newNoteContent = it },
                        label = { Text("Content / Details") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = NeonCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newNoteContent.isNotBlank() || newNoteTitle.isNotBlank()) {
                        val content = if (newNoteContent.isBlank()) newNoteTitle else newNoteContent
                        val title = if (newNoteTitle.isBlank()) "Note" else newNoteTitle
                        onAddNote(title, content, newNoteCategory)
                        newNoteTitle = ""
                        newNoteContent = ""
                        showAddNoteDialog = false
                    }
                }) {
                    Text("Save", color = NeonCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun NotesTabContent(notes: List<NoteItem>, onDeleteNote: (NoteItem) -> Unit) {
    if (notes.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Description,
            message = "No notes yet.\nSay 'Make a note: robotics assignment is due Friday' to save one!"
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(notes) { note ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = note.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan,
                                    modifier = Modifier
                                        .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(note.timestamp)),
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            if (note.title.isNotBlank() && note.title != "Note") {
                                Text(note.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                            }
                            Text(note.content, fontSize = 13.sp, color = TextPrimary.copy(alpha = 0.9f))
                        }
                        IconButton(onClick = { onDeleteNote(note) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShoppingTabContent(
    shoppingList: List<NoteItem>,
    onToggleShoppingItem: (NoteItem) -> Unit,
    onDeleteNote: (NoteItem) -> Unit
) {
    if (shoppingList.isEmpty()) {
        EmptyState(
            icon = Icons.Default.ShoppingCart,
            message = "Your shopping list is empty.\nSay 'Add milk and eggs to my shopping list'!"
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shoppingList) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleShoppingItem(item) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { onToggleShoppingItem(item) },
                            colors = CheckboxDefaults.colors(checkedColor = NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.content,
                            fontSize = 14.sp,
                            color = if (item.isCompleted) TextSecondary else TextPrimary,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteNote(item) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduledTabContent(
    tasks: List<ScheduledTask>,
    onDeleteScheduledTask: (ScheduledTask) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Schedule,
            message = "No scheduled actions.\nSay 'At 12 PM call Mom' or 'In 30 minutes remind me to study'!"
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tasks) { task ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            val icon = when (task.taskType) {
                                "CALL_REMINDER" -> Icons.Default.Phone
                                "STUDY_REMINDER" -> Icons.Default.MenuBook
                                else -> Icons.Default.Alarm
                            }
                            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                                val dateStr = SimpleDateFormat("h:mm a, EEE MMM d", Locale.getDefault()).format(Date(task.triggerTimeEpochMs))
                                Text(dateStr, fontSize = 11.sp, color = NeonCyan)
                                if (task.targetData.isNotBlank()) {
                                    Text("Target: ${task.targetData}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                        IconButton(onClick = { onDeleteScheduledTask(task) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogTabContent(auditLogs: List<AuditLogItem>) {
    if (auditLogs.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Security,
            message = "No activity logs yet.\nEvery action performed by AAYA will be tracked here for complete privacy trust."
        )
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(auditLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (log.actionType) {
                            "ALARM" -> Icons.Default.Alarm
                            "CALL" -> Icons.Default.Phone
                            "CAMERA" -> Icons.Default.CameraAlt
                            "NOTE" -> Icons.Default.Description
                            "MODE" -> Icons.Default.Bedtime
                            "REMINDER" -> Icons.Default.Notifications
                            else -> Icons.Default.Psychology
                        }
                        Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.summary, fontSize = 12.sp, color = TextPrimary)
                            Text(
                                SimpleDateFormat("h:mm a - d MMM", Locale.getDefault()).format(Date(log.timestamp)),
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
