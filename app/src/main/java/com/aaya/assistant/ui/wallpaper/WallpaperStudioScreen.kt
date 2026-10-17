package com.aaya.assistant.ui.wallpaper

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.aaya.assistant.data.model.WallpaperItem
import com.aaya.assistant.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WallpaperStudioScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedWallpaper by remember { mutableStateOf<WallpaperItem?>(null) }
    var isApplying by remember { mutableStateOf(false) }

    val categories = listOf("All", "AMOLED Dark", "Cyberpunk", "Aesthetic Lo-Fi", "Anime 3D", "Devotional", "Nature", "Minimalist")

    val wallpapers = remember { getCuratedWallpapers() }

    val filteredWallpapers = remember(selectedCategory, wallpapers) {
        if (selectedCategory == "All") wallpapers else wallpapers.filter { it.category == selectedCategory }
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepIndigoBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Column {
                        Text(
                            text = "🖼️ Wallpaper Studio",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "4K Ultra HD • 1-Tap Direct Apply",
                            fontSize = 11.sp,
                            color = NeonCyan
                        )
                    }
                }

                // Random Shuffle Apply Button
                IconButton(
                    onClick = {
                        val randomWp = wallpapers.random()
                        selectedWallpaper = randomWp
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(RadiantPurple.copy(alpha = 0.2f))
                        .border(1.dp, RadiantPurple.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Casino, contentDescription = "Random Pick", tint = NeonCyan, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Selector Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan,
                            selectedLabelColor = DeepIndigoBg,
                            containerColor = GlassSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) NeonCyan else CardBorder,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2-Column Staggered Grid of Wallpapers
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredWallpapers, key = { it.id }) { item ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = GlassSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
                            .clickable { selectedWallpaper = item }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = item.previewUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Bottom Gradient Shade with Title
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Column {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.category,
                                        fontSize = 10.sp,
                                        color = NeonCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Preview & 1-Tap Apply Dialog
        selectedWallpaper?.let { wp ->
            Dialog(onDismissRequest = { if (!isApplying) selectedWallpaper = null }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepIndigoBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                        .border(1.5.dp, NeonCyan, RoundedCornerShape(24.dp))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = wp.imageUrl,
                            contentDescription = wp.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top Close Button
                        IconButton(
                            onClick = { selectedWallpaper = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        // Bottom Action Controls Container
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xF2070814))
                                    )
                                )
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = wp.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "1-Tap Direct Wallpaper Apply",
                                fontSize = 11.sp,
                                color = NeonCyan
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isApplying) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = NeonCyan,
                                        strokeWidth = 2.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Applying wallpaper...", fontSize = 13.sp, color = TextPrimary)
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            isApplying = true
                                            scope.launch {
                                                val ok = applyWallpaperDirectly(context, wp.imageUrl, WallpaperTarget.HOME)
                                                isApplying = false
                                                if (ok) {
                                                    Toast.makeText(context, "🎉 Home Screen Wallpaper Applied!", Toast.LENGTH_SHORT).show()
                                                    selectedWallpaper = null
                                                } else {
                                                    Toast.makeText(context, "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("Home", color = DeepIndigoBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            isApplying = true
                                            scope.launch {
                                                val ok = applyWallpaperDirectly(context, wp.imageUrl, WallpaperTarget.LOCK)
                                                isApplying = false
                                                if (ok) {
                                                    Toast.makeText(context, "🎉 Lock Screen Wallpaper Applied!", Toast.LENGTH_SHORT).show()
                                                    selectedWallpaper = null
                                                } else {
                                                    Toast.makeText(context, "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RadiantPurple),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("Lock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            isApplying = true
                                            scope.launch {
                                                val ok = applyWallpaperDirectly(context, wp.imageUrl, WallpaperTarget.BOTH)
                                                isApplying = false
                                                if (ok) {
                                                    Toast.makeText(context, "🎉 Home & Lock Screen Applied!", Toast.LENGTH_SHORT).show()
                                                    selectedWallpaper = null
                                                } else {
                                                    Toast.makeText(context, "Failed to apply wallpaper", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("Both", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class WallpaperTarget {
    HOME, LOCK, BOTH
}

private suspend fun applyWallpaperDirectly(
    context: android.content.Context,
    imageUrl: String,
    target: WallpaperTarget
): Boolean = withContext(Dispatchers.IO) {
    try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .allowHardware(false)
            .build()

        val result = loader.execute(request)
        if (result is SuccessResult) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@withContext false
            val wm = WallpaperManager.getInstance(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                when (target) {
                    WallpaperTarget.HOME -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                    WallpaperTarget.LOCK -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                    WallpaperTarget.BOTH -> {
                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    }
                }
            } else {
                wm.setBitmap(bitmap)
            }
            return@withContext true
        }
    } catch (e: Exception) {
        android.util.Log.e("WallpaperStudio", "Failed to apply wallpaper", e)
    }
    return@withContext false
}

private fun getCuratedWallpapers(): List<WallpaperItem> {
    return listOf(
        // AMOLED Dark
        WallpaperItem(
            id = "wp_1",
            title = "Cyber Neon Sphere",
            category = "AMOLED Dark",
            imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1080&q=80"
        ),
        WallpaperItem(
            id = "wp_2",
            title = "Obsidian Liquid Wave",
            category = "AMOLED Dark",
            imageUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1080&q=80"
        ),

        // Cyberpunk
        WallpaperItem(
            id = "wp_3",
            title = "Neon Tokyo Skyline",
            category = "Cyberpunk",
            imageUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1080&q=80"
        ),
        WallpaperItem(
            id = "wp_4",
            title = "Futuristic Matrix CyberGrid",
            category = "Cyberpunk",
            imageUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=1080&q=80"
        ),

        // Aesthetic Lo-Fi
        WallpaperItem(
            id = "wp_5",
            title = "Purple Sunset Highway",
            category = "Aesthetic Lo-Fi",
            imageUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1080&q=80"
        ),
        WallpaperItem(
            id = "wp_6",
            title = "Midnight Coffee Lo-Fi",
            category = "Aesthetic Lo-Fi",
            imageUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=1080&q=80"
        ),

        // Anime 3D
        WallpaperItem(
            id = "wp_7",
            title = "Cosmic Sky Anime Vista",
            category = "Anime 3D",
            imageUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=1080&q=80"
        ),

        // Devotional
        WallpaperItem(
            id = "wp_8",
            title = "Golden Divine Temple Light",
            category = "Devotional",
            imageUrl = "https://images.unsplash.com/photo-1544717305-2782549b5136?w=1080&q=80"
        ),

        // Nature
        WallpaperItem(
            id = "wp_9",
            title = "Aurora Borealis Night",
            category = "Nature",
            imageUrl = "https://images.unsplash.com/photo-1531366936337-7c912a4589a7?w=1080&q=80"
        ),
        WallpaperItem(
            id = "wp_10",
            title = "Misty Mountain Pines",
            category = "Nature",
            imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1080&q=80"
        ),

        // Minimalist
        WallpaperItem(
            id = "wp_11",
            title = "Geometric Gradient Cube",
            category = "Minimalist",
            imageUrl = "https://images.unsplash.com/photo-1618005198919-d3d4b5a92ead?w=1080&q=80"
        )
    )
}
