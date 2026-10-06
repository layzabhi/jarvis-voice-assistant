package com.example.jarvis

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.jarvis.bubble.BubbleService
import com.example.jarvis.data.AppDb
import com.example.jarvis.data.Birthday
import com.example.jarvis.data.Reminder
import com.example.jarvis.nlu.Command
import com.example.jarvis.schedule.Scheduler
import com.example.jarvis.speech.Status
import com.example.jarvis.ui.theme.AmberThinking
import com.example.jarvis.ui.theme.CardBackground
import com.example.jarvis.ui.theme.CardBorder
import com.example.jarvis.ui.theme.DarkBackground
import com.example.jarvis.ui.theme.GrayText
import com.example.jarvis.ui.theme.GreenSuccess
import com.example.jarvis.ui.theme.JarvisTheme
import com.example.jarvis.ui.theme.PurplePrimary
import com.example.jarvis.ui.theme.RedListening
import com.example.jarvis.ui.theme.TealSpeaking
import com.example.jarvis.ui.theme.WhiteText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var engine: AssistantEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JarvisTheme {
                MainScreen(
                    onGetEngine = { getOrCreateEngine() }
                )
            }
        }
    }

    private fun getOrCreateEngine(): AssistantEngine {
        return engine ?: AssistantEngine(this) {}.also { engine = it }
    }

    override fun onDestroy() {
        engine?.release()
        engine = null
        super.onDestroy()
    }
}

data class PermissionState(
    val hasAudio: Boolean = false,
    val hasNotification: Boolean = false,
    val hasContacts: Boolean = false,
    val hasCall: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasExactAlarm: Boolean = false,
    val isBatteryOptimizedIgnored: Boolean = false
) {
    val essentialsGranted: Boolean
        get() = hasAudio && hasOverlay
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(onGetEngine: () -> AssistantEngine) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDb.get(context) }
    val scheduler = remember { Scheduler(context) }

    var permissions by remember { mutableStateOf(checkAllPermissions(context)) }
    var engineStatus by remember { mutableStateOf(Status.Idle) }
    var textInput by remember { mutableStateOf("") }
    var lastParsedCommand by remember { mutableStateOf<String?>(null) }
    var lastSpokenResponse by remember { mutableStateOf<String?>(null) }

    // Observe Room data
    val reminders by db.reminders().activeFlow().collectAsState(initial = emptyList())
    val birthdays by db.birthdays().allFlow().collectAsState(initial = emptyList())

    // Update permission status on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == LifecycleEvent.ON_RESUME) {
                permissions = checkAllPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher for runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissions = checkAllPermissions(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    when (engineStatus) {
                                        Status.Idle -> PurplePrimary
                                        Status.Listening -> RedListening
                                        Status.Thinking -> AmberThinking
                                        Status.Speaking -> TealSpeaking
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Jarvis Assistant",
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header / Intro
            item {
                Text(
                    text = "Local-First Android Voice Assistant",
                    fontSize = 14.sp,
                    color = GrayText
                )
            }

            // SECTION 1: Service Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Floating Bubble Control",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (permissions.essentialsGranted)
                                "Tap below to launch or dismiss the persistent floating overlay bubble."
                            else
                                "Grant Microphone and Draw Over Apps permissions below to enable the bubble.",
                            fontSize = 13.sp,
                            color = GrayText
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    val intent = Intent(context, BubbleService::class.java)
                                    ContextCompat.startForegroundService(context, intent)
                                },
                                enabled = permissions.essentialsGranted,
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Bubble")
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(context, BubbleService::class.java).apply {
                                        action = BubbleService.ACTION_STOP
                                    }
                                    context.startService(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Bubble")
                            }
                        }
                    }
                }
            }

            // SECTION 2: Setup Checklist
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Setup & Permissions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhiteText
                            )
                            Button(
                                onClick = {
                                    val list = mutableListOf(
                                        Manifest.permission.RECORD_AUDIO,
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.CALL_PHONE
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    permissionLauncher.launch(list.toTypedArray())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                            ) {
                                Text("Grant All", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        PermissionRow(
                            title = "Microphone (RECORD_AUDIO)",
                            subtitle = "Required for offline & online speech recognition",
                            isGranted = permissions.hasAudio,
                            onClick = {
                                permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            }
                        )

                        PermissionRow(
                            title = "Draw Over Other Apps (Overlay)",
                            subtitle = "Required for the floating bubble on screen",
                            isGranted = permissions.hasOverlay,
                            onClick = {
                                requestOverlay(context)
                            }
                        )

                        PermissionRow(
                            title = "Notifications (POST_NOTIFICATIONS)",
                            subtitle = "Shows foreground service & alarm notifications",
                            isGranted = permissions.hasNotification,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                }
                            }
                        )

                        PermissionRow(
                            title = "Contacts & Calls (READ_CONTACTS, CALL_PHONE)",
                            subtitle = "Allows voice dialing by fuzzy name matching",
                            isGranted = permissions.hasContacts && permissions.hasCall,
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CONTACTS,
                                        Manifest.permission.CALL_PHONE
                                    )
                                )
                            }
                        )

                        PermissionRow(
                            title = "Exact Alarms (SCHEDULE_EXACT_ALARM)",
                            subtitle = "Ensures reminders trigger precisely on time",
                            isGranted = permissions.hasExactAlarm,
                            onClick = {
                                requestExactAlarm(context)
                            }
                        )

                        PermissionRow(
                            title = "Battery Optimization Exclusion",
                            subtitle = "Keeps background service alive on aggressive phones",
                            isGranted = permissions.isBatteryOptimizedIgnored,
                            onClick = {
                                requestIgnoreBatteryOptimization(context)
                            }
                        )
                    }
                }
            }

            // SECTION 3: Text-Box & Voice Command Tester
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Command Tester",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Test Jarvis commands via text or voice directly:",
                            fontSize = 13.sp,
                            color = GrayText
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("e.g. remind me to buy milk tomorrow at 10 am") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = WhiteText,
                                unfocusedTextColor = WhiteText
                            ),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val engine = onGetEngine()
                                            engine.toggleListening()
                                            engineStatus = engine.status
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Mic,
                                            contentDescription = "Voice Input",
                                            tint = if (engineStatus == Status.Listening) RedListening else PurplePrimary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (textInput.isNotBlank()) {
                                                val engine = onGetEngine()
                                                val q = textInput
                                                engine.executeText(q) { cmd, reply ->
                                                    lastParsedCommand = cmd.toString()
                                                    lastSpokenResponse = reply
                                                    engineStatus = Status.Idle
                                                }
                                                textInput = ""
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = "Run Command",
                                            tint = PurplePrimary
                                        )
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Prompt Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val samples = listOf(
                                "set alarm for 6:30 am",
                                "set a timer for 5 minutes",
                                "remind me to workout tomorrow at 7 am",
                                "add birthday of Rahul on 15 march",
                                "play Bohemian Rhapsody on spotify",
                                "call Mom",
                                "search for Android documentation"
                            )
                            samples.forEach { sample ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF1E293B),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.clickable {
                                        textInput = sample
                                    }
                                ) {
                                    Text(
                                        text = sample,
                                        fontSize = 11.sp,
                                        color = GrayText,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = lastParsedCommand != null || lastSpokenResponse != null) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                if (lastParsedCommand != null) {
                                    Text(
                                        text = "Parsed: $lastParsedCommand",
                                        fontSize = 12.sp,
                                        color = AmberThinking
                                    )
                                }
                                if (lastSpokenResponse != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Jarvis Reply: \"$lastSpokenResponse\"",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TealSpeaking
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 4: Active Reminders
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = PurplePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Active Reminders (${reminders.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhiteText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (reminders.isEmpty()) {
                            Text("No pending reminders.", fontSize = 13.sp, color = GrayText)
                        } else {
                            val formatter = DateTimeFormatter.ofPattern("h:mm a, d MMM yyyy")
                            reminders.forEach { r ->
                                val dt = Instant.ofEpochMilli(r.triggerAt)
                                    .atZone(ZoneId.systemDefault())
                                    .format(formatter)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(r.text, fontSize = 14.sp, color = WhiteText, fontWeight = FontWeight.Medium)
                                        Text(dt, fontSize = 12.sp, color = GrayText)
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                scheduler.cancel(r.id, "reminder")
                                                db.reminders().delete(r.id)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 5: Birthdays
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Cake, contentDescription = null, tint = TealSpeaking)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Saved Birthdays (${birthdays.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WhiteText
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (birthdays.isEmpty()) {
                            Text("No saved birthdays.", fontSize = 13.sp, color = GrayText)
                        } else {
                            birthdays.forEach { b ->
                                val mName = try {
                                    Month.of(b.month).name.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase() }
                                } catch (_: Exception) {
                                    "${b.month}"
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(b.name, fontSize = 14.sp, color = WhiteText, fontWeight = FontWeight.Medium)
                                        Text("$mName ${b.day}", fontSize = 12.sp, color = GrayText)
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                scheduler.cancel(b.id, "birthday")
                                                scheduler.cancel(b.id, "birthday_eve")
                                                db.birthdays().delete(b.id)
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 6: Reliability on OEM Devices (Section 16 in guide)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OEM Background Reliability Tips",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = WhiteText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Xiaomi, Redmi, Realme, Oppo, Vivo and Samsung aggressively kill background services. To ensure the bubble and reminders never stop:\n" +
                                    "• Enable 'Autostart' for Jarvis in app info.\n" +
                                    "• Set Battery usage to 'No restrictions' / 'Don't optimize'.\n" +
                                    "• Lock Jarvis in your recents app carousel.\n" +
                                    "• Turn off 'Pause app activity if unused'.",
                            fontSize = 12.sp,
                            color = GrayText,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { if (!isGranted) onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isGranted) GreenSuccess.copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isGranted) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isGranted) GreenSuccess else Color(0xFFEF4444),
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = WhiteText, fontWeight = FontWeight.Medium)
            Text(subtitle, fontSize = 11.sp, color = GrayText)
        }
        if (!isGranted) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PurplePrimary.copy(alpha = 0.15f),
                modifier = Modifier.clickable { onClick() }
            ) {
                Text(
                    text = "Fix",
                    fontSize = 12.sp,
                    color = PurplePrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

fun checkAllPermissions(ctx: Context): PermissionState {
    val hasAudio = ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    val hasContacts = ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    val hasCall = ctx.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(ctx)
    } else true

    val am = ctx.getSystemService(AlarmManager::class.java)
    val hasExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        am?.canScheduleExactAlarms() ?: false
    } else true

    val pm = ctx.getSystemService(PowerManager::class.java)
    val isBatteryOptimizedIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false
    } else true

    return PermissionState(
        hasAudio = hasAudio,
        hasNotification = hasNotification,
        hasContacts = hasContacts,
        hasCall = hasCall,
        hasOverlay = hasOverlay,
        hasExactAlarm = hasExactAlarm,
        isBatteryOptimizedIgnored = isBatteryOptimizedIgnored
    )
}

fun requestOverlay(ctx: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + ctx.packageName)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ctx.startActivity(intent)
    }
}

fun requestExactAlarm(ctx: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:" + ctx.packageName)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        ctx.startActivity(intent)
    }
}

fun requestIgnoreBatteryOptimization(ctx: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:" + ctx.packageName)
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        try {
            ctx.startActivity(intent)
        } catch (_: Exception) {}
    }
}
