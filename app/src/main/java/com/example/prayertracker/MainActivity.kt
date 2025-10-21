package com.example.prayertracker

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.ModalDrawerSheet
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import android.net.Uri
import android.media.RingtoneManager
import android.media.Ringtone
import android.database.Cursor
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make status and nav bars dark for contrast
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color(0x00000000).toArgb()
        window.navigationBarColor = Color(0x00000000).toArgb()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        NotificationReceiver.ensureChannel(this)
        
        // Cancel any outdated alarms when app starts
        cancelOutdatedAlarms(this)
        
        setContent {
            var isDark by remember { mutableStateOf(getIsDarkTheme(this)) }
            PrayerTheme(darkTheme = isDark) {
                App(isDark = isDark) { v -> isDark = v; setIsDarkTheme(this, v) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun App(isDark: Boolean, onToggleTheme: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    val sp = remember { ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE) }
    val todayKey = remember { dayKey(Date()) }
    var state by remember { mutableStateOf(loadState(sp)) }
    var useAlarm by remember { mutableStateOf(getUseAlarm(ctx)) }
    
    // Force recomposition key to trigger UI updates
    var refreshTrigger by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var backStack by remember { mutableStateOf(listOf("home")) }
    val currentScreen = backStack.last()

    val notifPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* result handled by system UI; not tracked */ }
    
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { setCustomAlarmSound(ctx, it.toString()) }
    }

    fun persist() { 
        saveState(sp, state)
        refreshTrigger++ // Force UI refresh
    }
    
    fun navigateTo(screen: String) {
        if (screen != currentScreen) {
            backStack = backStack + screen
        }
        scope.launch { drawerState.close() }
    }
    
    fun navigateBack(): Boolean {
        return if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
            true
        } else {
            false // Let the system handle the back press (exit app)
        }
    }

    // Handle back button for navigation
    BackHandler(enabled = currentScreen != "home") {
        navigateBack()
    }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                currentScreen = currentScreen,
                onNavigate = ::navigateTo
            )
        }
    ) {
        Scaffold(
            topBar = { 
                TopAppBar(
                    title = { 
                        Text(
                            when (currentScreen) {
                                "home" -> "Prayer Tracker"
                                "prayer_times" -> "Prayer Times"
                                "settings" -> "Settings"
                                else -> "Prayer Tracker"
                            },
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        if (currentScreen == "home") {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = { navigateBack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { onToggleTheme(!isDark) }) {
                            Text(if (isDark) "☀️" else "🌙", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                ) 
            },
            bottomBar = {
                if (currentScreen != "home") {
                    BottomAppBar {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            IconButton(onClick = { navigateTo("home") }) {
                                Icon(Icons.Default.Home, contentDescription = "Home")
                            }
                        }
                    }
                }
            }
        ) { inner ->
            when (currentScreen) {
                "home" -> HomeScreen(
                    modifier = Modifier.padding(inner),
                    state = state,
                    todayKey = todayKey,
                    refreshTrigger = refreshTrigger,
                    ctx = ctx,
                    onMarkPrayer = { name, completed ->
                        val now = Date()
                        state.day(todayKey).prayers[name] = PrayerRecord(now.time, completed)
                        
                        // If any required prayer is marked as "No", reset the streak
                        if (name in REQUIRED && !completed) {
                            setStreakResetTime(ctx, now.time)
                        }
                        
                        persist()
                    }
                )
                "prayer_times" -> PrayerTimesScreen(
                    modifier = Modifier.padding(inner),
                    ctx = ctx,
                    useAlarm = useAlarm
                )
                "settings" -> AppSettingsScreen(
                    modifier = Modifier.padding(inner),
                    useAlarm = useAlarm,
                    onAlarmChange = { v -> useAlarm = v; setUseAlarm(ctx, v) },
                    notifPermLauncher = notifPermLauncher,
                    audioPickerLauncher = audioPickerLauncher,
                    ctx = ctx
                )
            }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    currentScreen: String,
    onNavigate: (String) -> Unit
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Menu",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
            
            HorizontalDivider()
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                label = { Text("Prayer Times") },
                selected = currentScreen == "prayer_times",
                onClick = { onNavigate("prayer_times") }
            )
            
            NavigationDrawerItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("App Settings") },
                selected = currentScreen == "settings",
                onClick = { onNavigate("settings") }
            )
        }
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: AppState,
    todayKey: String,
    refreshTrigger: Int,
    ctx: Context,
    onMarkPrayer: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Use derivedStateOf to ensure recalculation when state changes
                    val streak by remember(refreshTrigger) { derivedStateOf { calculateCurrentStreak(state, ctx) } }
                    val todayCount by remember(refreshTrigger) { derivedStateOf { getTodayCompletedCount(state, todayKey) } }
                    val todayTotal = 5 // Only count required prayers for daily goal
                    
                    Text(
                        "Current Prayer Streak",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "$streak prayers",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        if (todayCount == todayTotal) 
                            "🎆 All prayers completed today! ($todayCount/$todayTotal)" 
                        else 
                            "Today: $todayCount/$todayTotal prayers completed",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Today's Prayers",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val req = listOf("fajr","dhuhr","asr","maghrib","isha")
                    val opt = listOf("tahajjud")
                    val prayerNames = mapOf(
                        "fajr" to "Fajr",
                        "dhuhr" to "Dhuhr", 
                        "asr" to "Asr",
                        "maghrib" to "Maghrib",
                        "isha" to "Isha",
                        "tahajjud" to "Tahajjud"
                    )
                    
                    (req + opt).forEach { name ->
                        // Use derivedStateOf to ensure UI updates when prayer status changes
                        val completed by remember(refreshTrigger) { derivedStateOf { 
                            state.day(todayKey).prayers[name]?.completed ?: false 
                        } }
                        val rec = state.day(todayKey).prayers[name]
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (completed) 
                                    MaterialTheme.colorScheme.primaryContainer
                                else 
                                    MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        prayerNames[name] ?: name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        if (completed) "✓ Completed" else "Not completed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (completed) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { 
                                            onMarkPrayer(name, true)
                                        },
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = if (completed) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Text("✓ Yes")
                                    }
                                    OutlinedButton(
                                        onClick = { 
                                            onMarkPrayer(name, false)
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (!completed && rec != null) 
                                                MaterialTheme.colorScheme.errorContainer 
                                            else 
                                                Color.Transparent
                                        )
                                    ) {
                                        Text("✗ No")
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

@Composable
fun PrayerTimesScreen(
    modifier: Modifier = Modifier,
    ctx: Context,
    useAlarm: Boolean
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Set Prayer Times",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Enter your local prayer times in 24-hour format (HH:mm)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val names = listOf("fajr","dhuhr","asr","maghrib","isha","tahajjud")
                    val labels = mapOf(
                        "fajr" to "Fajr",
                        "dhuhr" to "Dhuhr",
                        "asr" to "Asr",
                        "maghrib" to "Maghrib",
                        "isha" to "Isha",
                        "tahajjud" to "Tahajjud (optional)"
                    )
                    var adhanDefaults by remember { mutableStateOf(getAdhanDefaults(ctx)) }
                    
                    names.forEach { n ->
                        TimeInputField(
                            label = labels[n] ?: n,
                            initialTime = adhanDefaults[n] ?: "",
                            onTimeChange = { newTime ->
                                adhanDefaults = adhanDefaults.toMutableMap().apply { put(n, newTime) }
                            }
                        )
                    }
                    
                    Button(
                        onClick = {
                            val cleaned = normalizeAndSaveAdhanDefaults(ctx, adhanDefaults)
                            scheduleNotificationsForToday(ctx, cleaned, useAlarm)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Schedule Alerts")
                    }
                    
                    Text(
                        "Alerts will fire 5 minutes before each prayer time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AppSettingsScreen(
    modifier: Modifier = Modifier,
    useAlarm: Boolean,
    onAlarmChange: (Boolean) -> Unit,
    notifPermLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    audioPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    ctx: Context
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "App Settings",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Alarm Sound",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Play loud alarm instead of notification",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useAlarm,
                            onCheckedChange = onAlarmChange
                        )
                    }
                    
                    if (useAlarm) {
                        AlarmSoundSelector(
                            ctx = ctx,
                            audioPickerLauncher = audioPickerLauncher
                        )
                    }
                    
                    if (Build.VERSION.SDK_INT >= 33) {
                        Button(
                            onClick = {
                                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Notifications")
                        }
                    }
                }
            }
        }
    }
}

/* -------------------- Data and storage -------------------- */
data class PrayerRecord(val doneAt: Long, val completed: Boolean)
data class DayData(
    val adhanTimes: MutableMap<String, String> = mutableMapOf(),
    val prayers: MutableMap<String, PrayerRecord> = mutableMapOf(),
    val quran: MutableMap<String, Int> = mutableMapOf()
)
data class AppState(val days: MutableMap<String, DayData> = mutableMapOf())

fun SharedPreferences.getJson(): JSONObject =
    JSONObject(getString("state_json", "{\"days\":{}}") ?: "{\"days\":{}}")

fun loadState(sp: SharedPreferences): AppState {
    val obj = sp.getJson()
    val daysObj = obj.optJSONObject("days") ?: JSONObject()
    val days = mutableMapOf<String, DayData>()
    for (k in daysObj.keys()) {
        val d = daysObj.getJSONObject(k)
        val adhanObj = d.optJSONObject("adhanTimes") ?: JSONObject()
        val prayersObj = d.optJSONObject("prayers") ?: JSONObject()
        val quranObj = d.optJSONObject("quran") ?: JSONObject()
        val adhan = mutableMapOf<String, String>()
        adhanObj.keys().forEach { name -> adhan[name] = adhanObj.optString(name, "") }
        val prs = mutableMapOf<String, PrayerRecord>()
        prayersObj.keys().forEach { name ->
            val p = prayersObj.getJSONObject(name)
            prs[name] = PrayerRecord(p.optLong("doneAt"), p.optBoolean("completed"))
        }
        val q = mutableMapOf<String, Int>()
        quranObj.keys().forEach { key -> q[key] = quranObj.optInt(key) }
        days[k] = DayData(adhan, prs, q)
    }
    return AppState(days)
}

fun saveState(sp: SharedPreferences, state: AppState) {
    val root = JSONObject()
    val daysObj = JSONObject()
    state.days.forEach { (k, d) ->
        val obj = JSONObject()
        val adhan = JSONObject()
        d.adhanTimes.forEach { (n, v) -> adhan.put(n, v) }
        val prs = JSONObject()
        d.prayers.forEach { (n, r) ->
            prs.put(n, JSONObject().put("doneAt", r.doneAt).put("completed", r.completed))
        }
        val q = JSONObject()
        d.quran.forEach { (n, v) -> q.put(n, v) }
        obj.put("adhanTimes", adhan).put("prayers", prs).put("quran", q)
        daysObj.put(k, obj)
    }
    root.put("days", daysObj)
    sp.edit { putString("state_json", root.toString()) }
}

fun AppState.day(key: String): DayData = days.getOrPut(key) { DayData() }

// Make AppState observable by making it a mutable state holder
@Stable
class ObservableAppState(initialState: AppState = AppState()) {
    private val _state = mutableStateOf(initialState)
    val state: AppState get() = _state.value
    
    fun updateState(newState: AppState) {
        _state.value = newState
    }
    
    fun day(key: String): DayData = state.day(key)
}

/* -------------------- Time helpers -------------------- */
private val REQUIRED = listOf("fajr","dhuhr","asr","maghrib","isha")
fun dayKey(d: Date): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d)
fun parseTimeToDate(dayKey: String, hhmm: String?): Date? {
    if (hhmm.isNullOrBlank()) return null
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.parse("$dayKey $hhmm")
}
fun nextPrayerName(name: String): String? {
    val idx = REQUIRED.indexOf(name)
    return if (idx in 0 until REQUIRED.lastIndex) REQUIRED[idx + 1] else null
}
// Removed inWindow logic since we're now tracking simple completion status
fun formatStamp(d: Date): String =
    SimpleDateFormat("hh:mm a, MMM d", Locale.getDefault()).format(d)

// Store streak reset information
fun getStreakResetTime(ctx: Context): Long {
    return ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).getLong("streak_reset_time", 0L)
}

fun setStreakResetTime(ctx: Context, time: Long) {
    ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).edit().putLong("streak_reset_time", time).apply()
}

fun calculateCurrentStreak(state: AppState, ctx: Context): Int {
    val streakResetTime = getStreakResetTime(ctx)
    var streakCount = 0
    
    // If no reset time is set, count all completed prayers from the beginning
    if (streakResetTime == 0L) {
        // Simple count of all completed prayers
        state.days.values.forEach { dayData ->
            dayData.prayers.forEach { (name, record) ->
                if (name in REQUIRED && record.completed) {
                    streakCount++
                }
            }
        }
        return streakCount
    }
    
    // Get all prayer records sorted by completion time
    val allPrayerRecords = mutableListOf<Pair<String, PrayerRecord>>()
    
    state.days.values.forEach { dayData ->
        dayData.prayers.forEach { (name, record) ->
            if (name in REQUIRED) {
                allPrayerRecords.add(name to record)
            }
        }
    }
    
    // Sort by completion time
    val sortedRecords = allPrayerRecords.sortedBy { it.second.doneAt }
    
    // Count prayers completed after the reset time
    for ((prayerName, record) in sortedRecords) {
        if (record.doneAt >= streakResetTime) {
            if (record.completed) {
                streakCount++
            } else {
                // If we find a "No" after reset time, the streak ends here
                break
            }
        }
    }
    
    return streakCount
}

fun getTodayCompletedCount(state: AppState, todayKey: String): Int {
    val dayData = state.days[todayKey] ?: return 0
    return REQUIRED.count { prayerName ->
        dayData.prayers[prayerName]?.completed == true
    }
}

// Adhan defaults in SharedPreferences
fun getAdhanDefaults(ctx: Context): Map<String,String> {
    val sp = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE)
    val m = mutableMapOf<String,String>()
    listOf("fajr","dhuhr","asr","maghrib","isha","tahajjud").forEach { n ->
        val v = sp.getString("adhan_default_$n", null)
        if (!v.isNullOrBlank()) m[n] = v
    }
    return m
}
fun setAdhanDefaults(ctx: Context, map: Map<String,String>) {
    val sp = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE)
    sp.edit().apply {
        listOf("fajr","dhuhr","asr","maghrib","isha","tahajjud").forEach { n ->
            val v = map[n]
            if (v.isNullOrBlank()) remove("adhan_default_$n") else putString("adhan_default_$n", v)
        }
    }.apply()
}

fun normalizeAndSaveAdhanDefaults(ctx: Context, map: Map<String,String>): Map<String,String> {
    val cleaned = mutableMapOf<String,String>()
    val re = Regex("^\\s*(\\d{1,2})[:](\\d{2})\\s*$")
    listOf("fajr","dhuhr","asr","maghrib","isha","tahajjud").forEach { n ->
        val raw = map[n]
        val m = raw?.let { re.matchEntire(it) }
        if (m != null) {
            val h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            if (h in 0..23 && min in 0..59) cleaned[n] = "%02d:%02d".format(h, min)
        }
    }
    setAdhanDefaults(ctx, cleaned)
    val preview = cleaned.entries.joinToString { "${it.key}=${it.value}" }
    Toast.makeText(ctx, if (cleaned.isEmpty()) "No valid times saved" else "Saved: $preview", Toast.LENGTH_SHORT).show()
    return cleaned
}

/* -------------------- Notifications scheduling -------------------- */
fun scheduleNotificationsForToday(ctx: Context, times: Map<String,String>, useAlarm: Boolean) {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val now = System.currentTimeMillis()
    val key = dayKey(Date())
    var scheduled = 0
    
    // First, cancel any existing alarms to avoid duplicates
    cancelAllPrayerAlarms(ctx)
    
    val toSchedule = REQUIRED + listOf("tahajjud") // include tahajjud as a prayer reminder
    toSchedule.forEach { name ->
        val hhmm = times[name] ?: return@forEach
        val atToday = parseTimeToDate(key, hhmm) ?: return@forEach
        // For required prayers: reminder 5 minutes before Adhan
        // For tahajjud: reminder at the exact time user set (no 5-minute offset)
        var fireAt = if (name == "tahajjud") atToday.time else atToday.time - 5*60*1000
        
        // Skip if the prayer time has already passed (don't schedule for tomorrow automatically)
        if (fireAt <= now) {
            // Only schedule for tomorrow if it's still early in the day (before 6 AM)
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = now
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            
            if (currentHour < 6) {
                fireAt += 24*60*60*1000 // schedule for tomorrow
            } else {
                return@forEach // skip this prayer, it's too late for today
            }
        }

        if (useAlarm) {
            val title = if (name == "tahajjud") "TAHAJJUD time" else "${name.uppercase()} in 5 minutes"
            val body = if (name == "tahajjud") "Time for Tahajjud" else "Adhan at ${formatStamp(Date(fireAt + 5*60*1000))}"
            scheduleAlarm(ctx, fireAt, title, body)
            scheduled++
        } else {
            val title = if (name == "tahajjud") "Tahajjud reminder" else "${name.uppercase()} in 5 minutes"
            val body = if (name == "tahajjud") "Time for Tahajjud" else "Adhan at ${formatStamp(Date(fireAt + 5*60*1000))}"
            val intent = Intent(ctx, NotificationReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("body", body)
                putExtra("scheduled_time", fireAt) // Add scheduled time for validation
            }
            val pi = PendingIntent.getBroadcast(
                ctx,
                (name + fireAt.toString()).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= 31) {
                    val canExact = ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                    if (!canExact) {
                        val showIntent = PendingIntent.getActivity(
                            ctx,
                            (name + "show" + fireAt.toString()).hashCode(),
                            Intent(ctx, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        val info = AlarmManager.AlarmClockInfo(fireAt, showIntent)
                        am.setAlarmClock(info, pi)
                    } else if (Build.VERSION.SDK_INT >= 23) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
                    } else {
                        am.setExact(AlarmManager.RTC_WAKEUP, fireAt, pi)
                    }
                } else if (Build.VERSION.SDK_INT >= 23) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, fireAt, pi)
                }
                scheduled++
            } catch (_: SecurityException) { }
        }
    }
    Toast.makeText(ctx, "Scheduled $scheduled ${if (useAlarm) "alarms" else "alerts"} (next 24h)", Toast.LENGTH_SHORT).show()
}

/* -------------------- Small utils -------------------- */
fun getIsDarkTheme(ctx: Context): Boolean = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).getBoolean("is_dark", true)
fun setIsDarkTheme(ctx: Context, v: Boolean) = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).edit().putBoolean("is_dark", v).apply()

fun getUseAlarm(ctx: Context): Boolean = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).getBoolean("use_alarm", false)
fun setUseAlarm(ctx: Context, v: Boolean) = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).edit().putBoolean("use_alarm", v).apply()

// Alarm sound preference functions
fun getSelectedAlarmSound(ctx: Context): String {
    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString()
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.toString() 
        ?: "default"
    return ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).getString("alarm_sound", defaultUri) ?: defaultUri
}
fun setSelectedAlarmSound(ctx: Context, soundType: String) = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).edit().putString("alarm_sound", soundType).apply()
fun getCustomAlarmSound(ctx: Context): String? = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).getString("custom_alarm_uri", null)
fun setCustomAlarmSound(ctx: Context, uri: String) = ctx.getSharedPreferences("pt_v1", Context.MODE_PRIVATE).edit().putString("custom_alarm_uri", uri).apply()

data class SystemSound(val uri: String, val title: String)

fun getAllSystemAlarmSounds(ctx: Context): List<SystemSound> {
    val sounds = mutableListOf<SystemSound>()
    
    try {
        // Get all alarm sounds first
        val alarmManager = RingtoneManager(ctx)
        alarmManager.setType(RingtoneManager.TYPE_ALARM)
        val alarmCursor = alarmManager.cursor
        
        if (alarmCursor != null) {
            while (alarmCursor.moveToNext()) {
                val title = alarmCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = alarmManager.getRingtoneUri(alarmCursor.position)
                if (uri != null) {
                    sounds.add(SystemSound(uri.toString(), "[Alarm] $title"))
                }
            }
            alarmCursor.close()
        }
        
        // Get all ringtone sounds as additional options
        val ringtoneManager = RingtoneManager(ctx)
        ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE)
        val ringtoneCursor = ringtoneManager.cursor
        
        if (ringtoneCursor != null) {
            while (ringtoneCursor.moveToNext()) {
                val title = ringtoneCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = ringtoneManager.getRingtoneUri(ringtoneCursor.position)
                if (uri != null) {
                    sounds.add(SystemSound(uri.toString(), "[Ringtone] $title"))
                }
            }
            ringtoneCursor.close()
        }
        
        // Get notification sounds as backup
        val notificationManager = RingtoneManager(ctx)
        notificationManager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val notificationCursor = notificationManager.cursor
        
        if (notificationCursor != null) {
            while (notificationCursor.moveToNext()) {
                val title = notificationCursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = notificationManager.getRingtoneUri(notificationCursor.position)
                if (uri != null) {
                    sounds.add(SystemSound(uri.toString(), "[Notification] $title"))
                }
            }
            notificationCursor.close()
        }
        
        // Add default system sounds if no sounds found
        if (sounds.isEmpty()) {
            val defaultAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val defaultNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            
            defaultAlarm?.let { sounds.add(SystemSound(it.toString(), "Default System Alarm")) }
            defaultNotification?.let { sounds.add(SystemSound(it.toString(), "Default Notification")) }
            defaultRingtone?.let { sounds.add(SystemSound(it.toString(), "Default Ringtone")) }
        }
        
    } catch (e: Exception) {
        // Fallback to default sounds if there's any error
        val defaultAlarm = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val defaultNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        defaultAlarm?.let { sounds.add(SystemSound(it.toString(), "Default System Alarm")) }
        defaultNotification?.let { sounds.add(SystemSound(it.toString(), "Default Notification")) }
    }
    
    return sounds
}

fun scheduleAlarm(ctx: Context, triggerAtMillis: Long, title: String, body: String) {
    val req = (title + "@" + triggerAtMillis).hashCode()
    val intent = Intent(ctx, NotificationReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("body", body)
        putExtra("alarm", true)
        putExtra("scheduled_time", triggerAtMillis) // Add scheduled time for validation
    }
    val pi = PendingIntent.getBroadcast(ctx, req, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (Build.VERSION.SDK_INT >= 31 && !ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
        val showIntent = PendingIntent.getActivity(
            ctx, req + 1, Intent(ctx, AlarmActivity::class.java).apply { putExtra("title", title) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), pi)
    } else if (Build.VERSION.SDK_INT >= 23) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
    } else {
        am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
    }
}

fun sendImmediateNotification(ctx: Context, title: String, body: String) {
    NotificationReceiver.ensureChannel(ctx)
    val intent = Intent(ctx, NotificationReceiver::class.java).apply {
        putExtra("title", title); putExtra("body", body)
    }
    val pi = PendingIntent.getBroadcast(ctx, 55555, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val now = System.currentTimeMillis() + 100 // tiny delay to go through receiver consistently
    if (Build.VERSION.SDK_INT >= 23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, now, pi) else am.setExact(AlarmManager.RTC_WAKEUP, now, pi)
}

// Function to cancel all prayer-related alarms
fun cancelAllPrayerAlarms(ctx: Context) {
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val prayerNames = REQUIRED + listOf("tahajjud")
    
    prayerNames.forEach { name ->
        // Cancel both alarm and notification versions
        val notificationIntent = Intent(ctx, NotificationReceiver::class.java)
        val notificationPi = PendingIntent.getBroadcast(
            ctx,
            (name + "notification").hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(notificationPi)
        
        // Cancel alarm version
        val alarmPi = PendingIntent.getBroadcast(
            ctx,
            (name + "alarm").hashCode(),
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(alarmPi)
    }
}

// Function to cancel outdated alarms when app starts
fun cancelOutdatedAlarms(ctx: Context) {
    val now = System.currentTimeMillis()
    val todayKey = dayKey(Date())
    val times = getAdhanDefaults(ctx)
    
    // Cancel all existing prayer alarms first
    cancelAllPrayerAlarms(ctx)
    
    // Check if we should reschedule for tomorrow
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = now
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    
    // Only reschedule if it's late at night (after 10 PM) or early morning (before 6 AM)
    if (currentHour >= 22 || currentHour < 6) {
        val useAlarm = getUseAlarm(ctx)
        scheduleNotificationsForToday(ctx, times, useAlarm)
    }
}

@Composable
fun TimeInputField(
    label: String,
    initialTime: String,
    onTimeChange: (String) -> Unit
) {
    // Parse initial time into hours and minutes, removing leading zeros for display
    val timeParts = remember(initialTime) {
        if (initialTime.contains(":")) {
            val parts = initialTime.split(":")
            if (parts.size == 2) {
                // Remove leading zeros for cleaner display
                val hour = parts[0].toIntOrNull()?.toString() ?: ""
                val minute = parts[1].toIntOrNull()?.toString() ?: ""
                Pair(hour, minute)
            } else {
                Pair("", "")
            }
        } else {
            Pair("", "")
        }
    }
    
    var hourValue by remember(initialTime) { mutableStateOf(TextFieldValue(timeParts.first, TextRange(timeParts.first.length))) }
    var minuteValue by remember(initialTime) { mutableStateOf(TextFieldValue(timeParts.second, TextRange(timeParts.second.length))) }
    
    val minuteFocusRequester = remember { FocusRequester() }
    
    // Update parent when either field changes
    LaunchedEffect(hourValue.text, minuteValue.text) {
        if (hourValue.text.isNotEmpty() && minuteValue.text.isNotEmpty()) {
            // Only pad with zeros when creating the final time string for saving
            val hour = hourValue.text.padStart(2, '0')
            val minute = minuteValue.text.padStart(2, '0')
            onTimeChange("$hour:$minute")
        } else if (hourValue.text.isEmpty() && minuteValue.text.isEmpty()) {
            onTimeChange("")
        }
    }
    
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour input
            OutlinedTextField(
                value = hourValue,
                onValueChange = { newValue ->
                    val inputText = newValue.text.filter { it.isDigit() }.take(2)
                    
                    // Validate hour input (0-23)
                    val hourInt = inputText.toIntOrNull()
                    if (inputText.isEmpty() || (hourInt != null && hourInt in 0..23)) {
                        hourValue = newValue.copy(text = inputText)
                    }
                },
                label = { Text("Hour") },
                placeholder = { Text("HH") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                ":",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            // Minute input
            OutlinedTextField(
                value = minuteValue,
                onValueChange = { newValue ->
                    val inputText = newValue.text.filter { it.isDigit() }.take(2)
                    
                    // Validate minute input (0-59)
                    val minuteInt = inputText.toIntOrNull()
                    if (inputText.isEmpty() || (minuteInt != null && minuteInt in 0..59)) {
                        minuteValue = newValue.copy(text = inputText)
                    }
                },
                label = { Text("Minute") },
                placeholder = { Text("MM") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(minuteFocusRequester)
            )
        }
    }
}

@Composable
fun AlarmSoundSelector(
    ctx: Context,
    audioPickerLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    var selectedSound by remember { mutableStateOf(getSelectedAlarmSound(ctx)) }
    var showSoundOptions by remember { mutableStateOf(false) }
    var currentlyPlayingRingtone by remember { mutableStateOf<Ringtone?>(null) }
    
    // Get all available system sounds
    val systemSounds = remember { getAllSystemAlarmSounds(ctx) }
    val customSoundUri = remember { getCustomAlarmSound(ctx) }
    
    // Function to play/stop sound preview
    fun toggleSoundPreview(uri: String) {
        try {
            currentlyPlayingRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
            
            val ringtone = RingtoneManager.getRingtone(ctx, Uri.parse(uri))
            if (ringtone != null) {
                ringtone.play()
                currentlyPlayingRingtone = ringtone
                // Stop after 3 seconds
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(3000)
                    if (ringtone.isPlaying) {
                        ringtone.stop()
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    // Stop any playing sound when composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            currentlyPlayingRingtone?.let {
                if (it.isPlaying) {
                    it.stop()
                }
            }
        }
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Alarm Sound",
            style = MaterialTheme.typography.titleMedium
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSoundOptions = !showSoundOptions },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    when (selectedSound) {
                        "custom" -> if (customSoundUri != null) "Custom Sound Selected" else "Select Custom Sound"
                        else -> systemSounds.find { it.uri == selectedSound }?.title ?: "Default System Alarm"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    if (showSoundOptions) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
        }
        
        if (showSoundOptions) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(8.dp)
                        .heightIn(max = 300.dp) // Limit height for scrolling
                ) {
                    // System sounds
                    items(systemSounds) { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .selectable(
                                        selected = selectedSound == sound.uri,
                                        onClick = {
                                            selectedSound = sound.uri
                                            setSelectedAlarmSound(ctx, sound.uri)
                                            showSoundOptions = false
                                        }
                                    )
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSound == sound.uri,
                                    onClick = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sound.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            // Preview button
                            IconButton(
                                onClick = { toggleSoundPreview(sound.uri) }
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Preview sound",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // Custom sound option
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedSound == "custom",
                                    onClick = {
                                        selectedSound = "custom"
                                        setSelectedAlarmSound(ctx, "custom")
                                        audioPickerLauncher.launch("audio/*")
                                    }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedSound == "custom",
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Custom Sound from Device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        if (selectedSound == "custom" && customSoundUri != null) {
            Text(
                "Custom sound loaded",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/* -------------------- Theming -------------------- */
@Composable
fun PrayerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val light = lightColorScheme(
        primary = Color(0xFF3355CC),
        onPrimary = Color.White,
        background = Color(0xFFF6F6FA),
        onBackground = Color(0xFF111318),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF111318)
    )
    val dark = darkColorScheme(
        primary = Color(0xFF5CC8FF),
        onPrimary = Color(0xFF0A0F1A),
        background = Color(0xFF0B1220),
        onBackground = Color(0xFFE6EEFC),
        surface = Color(0xFF111A2E),
        onSurface = Color(0xFFE6EEFC)
    )
    MaterialTheme(colorScheme = if (darkTheme) dark else light, content = content)
}
