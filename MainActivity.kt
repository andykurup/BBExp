package com.bbexperience.launcher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var vibrator: BBVibrator
    private lateinit var prefs: BBPreferences
    private lateinit var companionManager: BBCompanionManager
    private val predictionEngine = BBPredictionEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vibrator = BBVibrator(this)
        prefs = BBPreferences(this)
        companionManager = BBCompanionManager(this)

        val productivityFilter = ProductivityFilter(this)
        val allInstalledApps = productivityFilter.getAllLaunchableApps()

        // On first install, hide social media & games from the Home Screen by default,
        // while keeping them available in Settings -> Allow/Remove Apps and the Apps Drawer!
        prefs.initializeDefaultHomeFilter(allInstalledApps)

        setContent {
            var searchText by remember { mutableStateOf("") }
            var keyboardScale by remember { mutableFloatStateOf(prefs.keyboardScale) }
            var themeMode by remember { mutableStateOf(prefs.themeMode) }
            var wallpaperIdx by remember { mutableIntStateOf(prefs.wallpaperIndex) }
            var hiddenPackages by remember { mutableStateOf(prefs.getHiddenHomePackages()) }

            var activeScreen by remember { mutableStateOf("HOME") }
            var isSliderDocOpen by remember { mutableStateOf(false) }

            var showCompanionPrompt by remember {
                mutableStateOf(!prefs.hasDismissedCompanionPrompt && companionManager.getMissingCount() > 0)
            }

            val homeApps = remember(searchText, allInstalledApps, hiddenPackages) {
                val visible = allInstalledApps.filterNot { hiddenPackages.contains(it.packageName) }
                if (searchText.isBlank()) visible
                else visible.filter { it.label.contains(searchText, ignoreCase = true) }
            }

            val drawerApps = remember(searchText, allInstalledApps) {
                if (searchText.isBlank()) allInstalledApps
                else allInstalledApps.filter { it.label.contains(searchText, ignoreCase = true) }
            }

            fun launchPackage(packageName: String) {
                packageManager.getLaunchIntentForPackage(packageName)?.let {
                    startActivity(it)
                }
            }

            BBExperienceApp(
                homeApps = homeApps,
                drawerApps = drawerApps,
                allApps = allInstalledApps,
                searchQuery = searchText,
                keyboardScale = keyboardScale,
                themeMode = themeMode,
                wallpaperIndex = wallpaperIdx,
                hiddenPackages = hiddenPackages,
                activeScreen = activeScreen,
                isSliderDocOpen = isSliderDocOpen,
                showCompanionPrompt = showCompanionPrompt,
                companionSuite = companionManager.getSuiteStatus(),
                vibrator = vibrator,
                prefs = prefs,
                predictionEngine = predictionEngine,
                onUpdateSearch = { searchText = it },
                onScaleChange = { newScale ->
                    keyboardScale = newScale
                    prefs.keyboardScale = newScale
                },
                onThemeModeChange = { newMode ->
                    themeMode = newMode
                    prefs.themeMode = newMode
                },
                onWallpaperChange = { newIdx ->
                    wallpaperIdx = newIdx
                    prefs.wallpaperIndex = newIdx
                },
                onToggleHomeApp = { pkg, visible ->
                    prefs.setAppVisibleOnHome(pkg, visible)
                    hiddenPackages = prefs.getHiddenHomePackages()
                },
                onSetShortcut = { keyChar, pkg ->
                    prefs.setShortcutPackage(keyChar, pkg)
                },
                onNavigateScreen = { activeScreen = it },
                onToggleSliderDoc = { isSliderDocOpen = it },
                onDismissCompanionPrompt = {
                    prefs.hasDismissedCompanionPrompt = true
                    showCompanionPrompt = false
                },
                onInstallCompanion = { pkg ->
                    companionManager.openPlayStoreForPackage(pkg)
                },
                onAppClick = { launchPackage(it) },
                onShortcutTriggered = { keyChar ->
                    val customPkg = prefs.getShortcutPackage(keyChar)
                    if (customPkg != null) {
                        launchPackage(customPkg)
                    } else {
                        allInstalledApps.firstOrNull { it.label.startsWith(keyChar, ignoreCase = true) }
                            ?.let { launchPackage(it.packageName) }
                    }
                },
                onOpenSystemKeyboardSettings = {
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                },
                onOpenDefaultLauncherSettings = {
                    startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BBExperienceApp(
    homeApps: List<AppItem>,
    drawerApps: List<AppItem>,
    allApps: List<AppItem>,
    searchQuery: String,
    keyboardScale: Float,
    themeMode: String,
    wallpaperIndex: Int,
    hiddenPackages: Set<String>,
    activeScreen: String,
    isSliderDocOpen: Boolean,
    showCompanionPrompt: Boolean,
    companionSuite: List<CompanionAppSpec>,
    vibrator: BBVibrator,
    prefs: BBPreferences,
    predictionEngine: BBPredictionEngine,
    onUpdateSearch: (String) -> Unit,
    onScaleChange: (Float) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onWallpaperChange: (Int) -> Unit,
    onToggleHomeApp: (String, Boolean) -> Unit,
    onSetShortcut: (String, String?) -> Unit,
    onNavigateScreen: (String) -> Unit,
    onToggleSliderDoc: (Boolean) -> Unit,
    onDismissCompanionPrompt: () -> Unit,
    onInstallCompanion: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onShortcutTriggered: (String) -> Unit,
    onOpenSystemKeyboardSettings: () -> Unit,
    onOpenDefaultLauncherSettings: () -> Unit
) {
    val wallpapers = listOf(
        Brush.verticalGradient(listOf(Color(0xFF0E131B), Color(0xFF172233), Color(0xFF0A0C10))),
        Brush.verticalGradient(listOf(Color(0xFF141518), Color(0xFF23262E), Color(0xFF0B0C0E))),
        Brush.verticalGradient(listOf(Color(0xFF1B1510), Color(0xFF2D2318), Color(0xFF0E0B08)))
    )
    val selectedBg = wallpapers[wallpaperIndex % wallpapers.size]

    val timeString = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
    val dateString = remember {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
    }

    val currentWord = remember(searchQuery) { searchQuery.substringAfterLast(' ') }
    val previousWord = remember(searchQuery) {
        if (' ' in searchQuery) searchQuery.substringBeforeLast(' ').substringAfterLast(' ') else ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08090B))
            .systemBarsPadding()
    ) {
        // ADAPTIVE UPPER VIEWPORT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(selectedBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x66000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeString,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = dateString,
                            color = Color(0xFFB0B8C4),
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TopNavChip("Home", activeScreen == "HOME") { onNavigateScreen("HOME") }
                        TopNavChip("Apps", activeScreen == "DRAWER") { onNavigateScreen("DRAWER") }
                        TopNavChip("Suite", activeScreen == "COMPANIONS") { onNavigateScreen("COMPANIONS") }
                        TopNavChip("⚙", activeScreen == "SETTINGS") { onNavigateScreen("SETTINGS") }
                    }
                }

                if (searchQuery.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .background(Color(0x991C222D), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Search: $searchQuery", color = Color(0xFFD4AF37), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "Clear ✕",
                            color = Color(0xFF70B0FF),
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onUpdateSearch("") }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 6.dp)) {
                    when (activeScreen) {
                        "DRAWER" -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(drawerApps) { app ->
                                    BBAppTile(app = app, iconSize = 42) { onAppClick(app.packageName) }
                                }
                            }
                        }
                        "COMPANIONS" -> {
                            BBCompanionSuiteScreen(
                                suite = companionSuite,
                                onInstall = onInstallCompanion,
                                onLaunch = onAppClick
                            )
                        }
                        "SETTINGS" -> {
                            BBSettingsScreen(
                                allApps = allApps,
                                hiddenPackages = hiddenPackages,
                                keyboardScale = keyboardScale,
                                themeMode = themeMode,
                                wallpaperIndex = wallpaperIdx,
                                prefs = prefs,
                                onScaleChange = onScaleChange,
                                onThemeModeChange = onThemeModeChange,
                                onWallpaperChange = onWallpaperChange,
                                onToggleHomeApp = onToggleHomeApp,
                                onSetShortcut = onSetShortcut,
                                onOpenKeyboardSettings = onOpenSystemKeyboardSettings,
                                onOpenLauncherSettings = onOpenDefaultLauncherSettings
                            )
                        }
                        else -> {
                            BBPaginatedHomeView(
                                homeApps = homeApps,
                                allApps = allApps,
                                themeMode = themeMode,
                                vibrator = vibrator,
                                onAppClick = onAppClick,
                                onOpenCompanions = { onNavigateScreen("COMPANIONS") }
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(16.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    .background(Color(0xCCD4AF37))
                    .clickable {
                        vibrator.keyClick()
                        onToggleSliderDoc(!isSliderDocOpen)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSliderDocOpen) "›" else "‹",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            if (isSliderDocOpen) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    BBSliderDocPanel(
                        allApps = homeApps.ifEmpty { allApps },
                        onAppClick = {
                            onToggleSliderDoc(false)
                            onAppClick(it)
                        },
                        onClose = { onToggleSliderDoc(false) }
                    )
                }
            }

            if (showCompanionPrompt) {
                BBCompanionPromptOverlay(
                    suite = companionSuite,
                    onInstall = onInstallCompanion,
                    onDismiss = onDismissCompanionPrompt
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFF4B4F58))
        )

        BBKey2FullKeyboard(
            vibrator = vibrator,
            predictionEngine = predictionEngine,
            keyboardScale = keyboardScale,
            onScaleChange = onScaleChange,
            currentWordContext = currentWord,
            previousWordContext = previousWord,
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
                homeApps.firstOrNull()?.let { onAppClick(it.packageName) }
            },
            onSwipeLeftDeleteWord = {
                vibrator.keyClick()
                onUpdateSearch("")
            },
            onCursorMove = { vibrator.trackpadTick() },
            onApplyPrediction = { word ->
                vibrator.keyClick()
                val prefix = if (' ' in searchQuery) searchQuery.substringBeforeLast(' ') + " " else ""
                onUpdateSearch("$prefix$word ")
            },
            onShortcutTriggered = onShortcutTriggered
        )
    }
}

@Composable
fun TopNavChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) Color(0xFFD4AF37) else Color(0x33FFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BBPaginatedHomeView(
    homeApps: List<AppItem>,
    allApps: List<AppItem>,
    themeMode: String,
    vibrator: BBVibrator,
    onAppClick: (String) -> Unit,
    onOpenCompanions: () -> Unit
) {
    val appsPerPage = if (themeMode == "BB10") 12 else 16
    val appPages = remember(homeApps, appsPerPage) {
        if (homeApps.isEmpty()) listOf(emptyList()) else homeApps.chunked(appsPerPage)
    }

    val totalPages = appPages.size + 1
    val pagerState = rememberPagerState(initialPage = 1) { totalPages }
    val scope = rememberCoroutineScope()
    val dockApps = remember(homeApps) { homeApps.take(4) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { pageIdx ->
            if (pageIdx == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x8812151C), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("BLACKBERRY HUB", color = Color(0xFFD9383A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Install BB Apps ›", color = Color(0xFF70B0FF), fontSize = 11.sp, modifier = Modifier.clickable { onOpenCompanions() })
                    }
                    HubActionRow("✉  Unified Inbox (BB Hub)", "com.blackberry.hub", allApps, onAppClick, onOpenCompanions)
                    HubActionRow("📅  Enterprise Calendar", "com.blackberry.calendar", allApps, onAppClick, onOpenCompanions)
                    HubActionRow("📝  BlackBerry Notes", "com.blackberry.notes", allApps, onAppClick, onOpenCompanions)
                }
            } else {
                val pageItems = appPages.getOrElse(pageIdx - 1) { emptyList() }
                val rowCount = if (themeMode == "BB10") 3 else 4
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val rows = pageItems.chunked(4)
                    for (r in 0 until rowCount) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val rowItems = rows.getOrElse(r) { emptyList() }
                            for (c in 0 until 4) {
                                val app = rowItems.getOrNull(c)
                                if (app != null) {
                                    BBAppTile(
                                        app = app,
                                        iconSize = if (themeMode == "BB10") 44 else 40,
                                        isCardStyle = themeMode == "BB10",
                                        onClick = { onAppClick(app.packageName) }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(68.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        BBScrubbableDots(
            totalPages = totalPages,
            currentPage = pagerState.currentPage,
            vibrator = vibrator,
            onSelectPage = { target -> scope.launch { pagerState.scrollToPage(target) } }
        )

        if (dockApps.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x77000000), RoundedCornerShape(10.dp))
                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                    .padding(vertical = 5.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                dockApps.forEach { app ->
                    BBAppTile(app = app, iconSize = 36, showLabel = false) { onAppClick(app.packageName) }
                }
            }
        }
    }
}

@Composable
fun HubActionRow(
    label: String,
    targetPkg: String,
    allApps: List<AppItem>,
    onAppClick: (String) -> Unit,
    onInstallRedirect: () -> Unit
) {
    val installed = allApps.any { it.packageName == targetPkg }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x991E232D), RoundedCornerShape(6.dp))
            .clickable {
                if (installed) onAppClick(targetPkg) else onInstallRedirect()
            }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(
            text = if (installed) "OPEN" else "GET APP",
            color = if (installed) Color(0xFF4ADE80) else Color(0xFFD4AF37),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun BBScrubbableDots(
    totalPages: Int,
    currentPage: Int,
    vibrator: BBVibrator,
    onSelectPage: (Int) -> Unit
) {
    var rowWidthPx by remember { mutableIntStateOf(1) }
    var lastPage by remember { mutableIntStateOf(currentPage) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .onSizeChanged { rowWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(totalPages) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val fraction = (change.position.x / rowWidthPx.toFloat()).coerceIn(0f, 0.999f)
                    val target = (fraction * totalPages).toInt()
                    if (target != lastPage) {
                        lastPage = target
                        vibrator.trackpadTick()
                        onSelectPage(target)
                    }
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalPages) {
            val selected = i == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .clickable {
                        vibrator.keyClick()
                        onSelectPage(i)
                    }
                    .size(width = if (selected) 14.dp else 6.dp, height = 6.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            selected && i == 0 -> Color(0xFFD9383A)
                            selected -> Color(0xFFD4AF37)
                            else -> Color(0x66FFFFFF)
                        }
                    )
            )
        }
    }
}

@Composable
fun BBAppTile(
    app: AppItem,
    iconSize: Int = 40,
    showLabel: Boolean = true,
    isCardStyle: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCardStyle) Color(0x551E232D) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 3.dp)
    ) {
        val bitmap = remember(app.icon) { app.icon.toBitmap(96, 96).asImageBitmap() }
        Image(
            bitmap = bitmap,
            contentDescription = app.label,
            modifier = Modifier
                .size(iconSize.dp)
                .clip(RoundedCornerShape(9.dp))
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(3.dp))
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

@Composable
fun BBSliderDocPanel(
    allApps: List<AppItem>,
    onAppClick: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(210.dp)
            .fillMaxHeight()
            .background(Color(0xEE11141B))
            .border(1.dp, Color(0xFFD4AF37))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PRODUCTIVITY DOC", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("✕", color = Color.White, fontSize = 14.sp, modifier = Modifier.clickable { onClose() })
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PINNED QUICK TOOLS", color = Color(0xFF8C929E), fontSize = 10.sp)
            allApps.take(5).forEach { app ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1C212B), RoundedCornerShape(6.dp))
                        .clickable { onAppClick(app.packageName) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bmp = remember(app.icon) { app.icon.toBitmap(64, 64).asImageBitmap() }
                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(app.label, color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        Text(
            text = "Tip: Press Speed Key (:::) + Letter for instant launch.",
            color = Color(0xFF8C929E),
            fontSize = 10.sp
        )
    }
}

@Composable
fun BBCompanionSuiteScreen(
    suite: List<CompanionAppSpec>,
    onInstall: (String) -> Unit,
    onLaunch: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99101318), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "BLACKBERRY COMPANION SUITE",
                color = Color(0xFFD4AF37),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        items(suite) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1E26), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(item.subtitle, color = Color(0xFF9CA3AF), fontSize = 10.sp)
                }
                Button(
                    onClick = {
                        if (item.isInstalled) onLaunch(item.packageName) else onInstall(item.packageName)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (item.isInstalled) Color(0xFF1E3A2F) else Color(0xFFD4AF37)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(
                        text = if (item.isInstalled) "Installed" else "Install",
                        color = if (item.isInstalled) Color(0xFF4ADE80) else Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun BBCompanionPromptOverlay(
    suite: List<CompanionAppSpec>,
    onInstall: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD080A0E))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161A22), RoundedCornerShape(10.dp))
                .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(10.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INSTALL REQUIRED BB APPS", color = Color(0xFFD4AF37), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Skip ✕", color = Color(0xFF9CA3AF), fontSize = 11.sp, modifier = Modifier.clickable { onDismiss() })
            }

            suite.filter { !it.isInstalled }.take(3).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF202530), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.subtitle, color = Color(0xFF9CA3AF), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        text = "INSTALL",
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFD4AF37))
                            .clickable { onInstall(item.packageName) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BBSettingsScreen(
    allApps: List<AppItem>,
    hiddenPackages: Set<String>,
    keyboardScale: Float,
    themeMode: String,
    wallpaperIndex: Int,
    prefs: BBPreferences,
    onScaleChange: (Float) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onWallpaperChange: (Int) -> Unit,
    onToggleHomeApp: (String, Boolean) -> Unit,
    onSetShortcut: (String, String?) -> Unit,
    onOpenKeyboardSettings: () -> Unit,
    onOpenLauncherSettings: () -> Unit
) {
    var selectedShortcutKey by remember { mutableStateOf("E") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC101318), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenLauncherSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Set Default Home", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onOpenKeyboardSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Enable KEY2 IME", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1E26), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text("HOME STYLE & WALLPAPER", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TopNavChip("BB OS7 Grid", themeMode == "BBOS7") { onThemeModeChange("BBOS7") }
                    TopNavChip("BB10 Cards", themeMode == "BB10") { onThemeModeChange("BB10") }
                    TopNavChip("Cobalt", wallpaperIndex == 0) { onWallpaperChange(0) }
                    TopNavChip("Carbon", wallpaperIndex == 1) { onWallpaperChange(1) }
                    TopNavChip("Bronze", wallpaperIndex == 2) { onWallpaperChange(2) }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1E26), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "KEYBOARD SIZE: ${(keyboardScale * 100).toInt()}%",
                    color = Color(0xFFD4AF37),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Slider(
                    value = keyboardScale,
                    onValueChange = onScaleChange,
                    valueRange = 0.75f..1.35f,
                    modifier = Modifier.height(28.dp)
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1E26), RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Text("SPEED KEY (:::) SHORTCUTS", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("E", "C", "N", "M", "W", "B").forEach { k ->
                        TopNavChip("Key $k", selectedShortcutKey == k) { selectedShortcutKey = k }
                    }
                }
                val currentAssigned = prefs.getShortcutPackage(selectedShortcutKey) ?: "Auto (First '$selectedShortcutKey' App)"
                Text("Current [$selectedShortcutKey]: $currentAssigned", color = Color(0xFF9CA3AF), fontSize = 10.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allApps.take(3).forEach { app ->
                        TopNavChip(app.label.take(8), prefs.getShortcutPackage(selectedShortcutKey) == app.packageName) {
                            onSetShortcut(selectedShortcutKey, app.packageName)
                        }
                    }
                    TopNavChip("Reset", false) { onSetShortcut(selectedShortcutKey, null) }
                }
            }
        }

        item {
            Text("ALLOW / REMOVE APPS ON HOME SCREEN (ALL APPS)", color = Color(0xFFD4AF37), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        items(allApps) { app ->
            val isVisible = !hiddenPackages.contains(app.packageName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1E26), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, color = Color.White, fontSize = 12.sp)
                    if (app.isSocialOrGame) {
                        Text("Social / Media (Hidden by default)", color = Color(0xFF9CA3AF), fontSize = 9.sp)
                    }
                }
                Switch(
                    checked = isVisible,
                    onCheckedChange = { onToggleHomeApp(app.packageName, it) },
                    modifier = Modifier.height(26.dp)
                )
            }
        }
    }
}
