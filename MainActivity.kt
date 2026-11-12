package com.bbexperience.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

class MainActivity : ComponentActivity() {

    private lateinit var vibrator: BBVibrator
    private val predictionEngine = BBPredictionEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vibrator = BBVibrator(this)
        val productivityFilter = ProductivityFilter(this)
        val apps = productivityFilter.getFilteredApps()

        setContent {
            var searchText by remember { mutableStateOf("") }

            val filteredApps = remember(searchText, apps) {
                if (searchText.isBlank()) apps
                else apps.filter { it.label.contains(searchText, ignoreCase = true) }
            }

            BBHomeScreen(
                apps = filteredApps,
                searchQuery = searchText,
                vibrator = vibrator,
                predictionEngine = predictionEngine,
                onUpdateSearch = { searchText = it },
                onAppClick = { packageName ->
                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    }
                },
                onShortcutTriggered = { keyChar ->
                    val target = apps.firstOrNull { it.label.startsWith(keyChar, ignoreCase = true) }
                    if (target != null) {
                        packageManager.getLaunchIntentForPackage(target.packageName)?.let {
                            startActivity(it)
                        }
                    }
                },
                onOpenKeyboardSettings = {
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                }
            )
        }
    }
}

@Composable
fun BBHomeScreen(
    apps: List<AppItem>,
    searchQuery: String,
    vibrator: BBVibrator,
    predictionEngine: BBPredictionEngine,
    onUpdateSearch: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onShortcutTriggered: (String) -> Unit,
    onOpenKeyboardSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E11))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f)
                .background(Color(0xFF14161A))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BB EXPERIENCE",
                        color = Color(0xFFC5A059),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "Enable Keyboard",
                        color = Color(0xFF4CA3FF),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clickable { onOpenKeyboardSettings() }
                            .padding(4.dp)
                    )
                }

                if (searchQuery.isNotEmpty()) {
                    Text(
                        text = "Type-to-Search: $searchQuery",
                        color = Color(0xFFD4AF37),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (apps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching productivity apps", color = Color.Gray, fontSize = 12.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(apps) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onAppClick(app.packageName) }
                                    .padding(4.dp)
                            ) {
                                val bitmap = remember(app.icon) { app.icon.toBitmap(96, 96).asImageBitmap() }
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = app.label,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFF383A40))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF08090A)),
            contentAlignment = Alignment.BottomCenter
        ) {
            BBKey2FullKeyboard(
                vibrator = vibrator,
                predictionEngine = predictionEngine,
                getCurrentWord = { searchQuery.substringAfterLast(' ') },
                onCommitText = { char ->
                    vibrator.keyClick()
                    onUpdateSearch(searchQuery + char)
                },
                onDelete = {
                    vibrator.keyClick()
                    if (searchQuery.isNotEmpty()) {
                        onUpdateSearch(searchQuery.dropLast(1))
                    }
                },
                onSpace = {
                    vibrator.keyClick()
                    onUpdateSearch("$searchQuery ")
                },
                onEnter = {
                    vibrator.keyClick()
                    apps.firstOrNull()?.let { onAppClick(it.packageName) }
                },
                onSwipeLeftDeleteWord = {
                    vibrator.keyClick()
                    onUpdateSearch("")
                },
                onCursorMove = { vibrator.trackpadTick() },
                onApplyPrediction = { word ->
                    vibrator.keyClick()
                    val prefix = if (' ' in searchQuery) searchQuery.substringBeforeLast(' ') + " " else ""
                    onUpdateSearch("$prefix$word")
                },
                onShortcutTriggered = onShortcutTriggered
            )
        }
    }
}
