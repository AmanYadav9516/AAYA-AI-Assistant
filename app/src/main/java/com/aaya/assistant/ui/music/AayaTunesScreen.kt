package com.aaya.assistant.ui.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aaya.assistant.data.model.SongItem
import com.aaya.assistant.data.remote.AayaMusicRepository
import com.aaya.assistant.engine.audio.AayaMusicPlayer
import com.aaya.assistant.ui.theme.*

@Composable
fun AayaTunesScreen(
    onBack: (() -> Unit)? = null,
    onOpenAudioJam: () -> Unit = {}
) {
    val context = LocalContext.current
    val musicPlayer = remember { AayaMusicPlayer.getInstance(context) }
    val musicRepo = remember { AayaMusicRepository() }

    val songs by musicRepo.getSongsStream().collectAsState(initial = AayaMusicRepository.DEFAULT_SONGS)
    val currentSong by musicPlayer.currentSong.collectAsState()
    val isPlaying by musicPlayer.isPlaying.collectAsState()
    val progressMs by musicPlayer.progressMs.collectAsState()
    val durationMs by musicPlayer.durationMs.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    var showMixerDialog by remember { mutableStateOf(false) }
    var showFullPlayerSheet by remember { mutableStateOf(false) }

    val categories = listOf("All", "Lo-Fi & Chill", "Focus & Study", "Devotional / Bhakti", "Party & Bass Boost", "Hindi Melodies", "English Pop")

    val filteredSongs = remember(songs, selectedCategory) {
        if (selectedCategory == "All") songs else songs.filter { it.category == selectedCategory }
    }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            musicPlayer.setPlaylist(songs)
        }
    }

    if (showMixerDialog) {
        AudioMixerDialog(onDismiss = { showMixerDialog = false })
    }

    if (onBack != null) {
        BackHandler { onBack() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column {
                        Text(
                            text = "🎵 AAYA Tunes",
                            style = MaterialTheme.typography.headlineLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Cloud streamed • Studio EQ • Audio Jam",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Studio EQ Button
                    IconButton(
                        onClick = { showMixerDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RadiantPurple.copy(alpha = 0.2f))
                            .border(1.dp, RadiantPurple, CircleShape)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Studio Mixer", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }

                    // Audio Jam Button
                    Button(
                        onClick = onOpenAudioJam,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Podcasts, contentDescription = null, tint = DeepIndigoBg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Jam 📡", color = DeepIndigoBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Categories horizontal scroll
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else GlassSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NeonCyan else CardBorder.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Songs List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(filteredSongs) { song ->
                    val isCurrent = currentSong?.id == song.id

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isCurrent) GlassSurface.copy(alpha = 0.95f) else GlassSurface.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isCurrent) NeonCyan else CardBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                if (isCurrent) {
                                    musicPlayer.togglePlayPause()
                                } else {
                                    musicPlayer.playSong(song)
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover art
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.coverUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = song.coverUrl,
                                        contentDescription = song.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonCyan)
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isCurrent) NeonCyan else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${song.artist} • ${song.duration}",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = RadiantPurple.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = song.category,
                                        fontSize = 10.sp,
                                        color = RadiantPurple,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Play/Pause icon button
                            IconButton(
                                onClick = {
                                    if (isCurrent) {
                                        musicPlayer.togglePlayPause()
                                    } else {
                                        musicPlayer.playSong(song)
                                    }
                                }
                            ) {
                                Icon(
                                    if (isCurrent && isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "Play",
                                    tint = if (isCurrent) NeonCyan else Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Bottom Mini-Player Bar
        currentSong?.let { song ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceDark.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable { showFullPlayerSheet = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.coverUrl.isNotBlank()) {
                            AsyncImage(
                                model = song.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeonCyan)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(song.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                    }

                    IconButton(onClick = { musicPlayer.playPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = TextPrimary)
                    }

                    IconButton(onClick = { musicPlayer.togglePlayPause() }) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(onClick = { musicPlayer.playNext() }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary)
                    }
                }
            }
        }

        // Full Screen Vinyl Player Dialog
        if (showFullPlayerSheet && currentSong != null) {
            val song = currentSong!!

            val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotate")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = if (isPlaying) 8000 else Int.MAX_VALUE, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Dialog(onDismissRequest = { showFullPlayerSheet = false }) {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, CardBorder, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showFullPlayerSheet = false }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse", tint = TextPrimary)
                            }
                            Text("NOW PLAYING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                            IconButton(onClick = { showMixerDialog = true }) {
                                Icon(Icons.Default.Tune, contentDescription = "Mixer", tint = NeonCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Spinning Vinyl Disc
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F111A))
                                .border(4.dp, Color(0xFF22283A), CircleShape)
                                .border(8.dp, Color(0xFF131622), CircleShape)
                                .rotate(if (isPlaying) rotationAngle else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, NeonCyan, CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(RadiantPurple),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(song.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(song.artist, fontSize = 13.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress Slider
                        Slider(
                            value = progressMs.toFloat(),
                            onValueChange = { musicPlayer.seekTo(it.toInt()) },
                            valueRange = 0f..durationMs.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(progressMs), fontSize = 11.sp, color = TextSecondary)
                            Text(formatTime(durationMs), fontSize = 11.sp, color = TextSecondary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Big Controls Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { musicPlayer.playPrevious() }) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = TextPrimary, modifier = Modifier.size(32.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                                    .clickable { musicPlayer.togglePlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = DeepIndigoBg,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            IconButton(onClick = { musicPlayer.playNext() }) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextPrimary, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
