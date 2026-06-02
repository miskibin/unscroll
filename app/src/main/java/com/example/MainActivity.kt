package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val service = "${context.packageName}/${DelayAccessibilityService::class.java.canonicalName}"
    return try {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        enabledServices?.contains(service) == true
    } catch (e: Exception) {
        false
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val delaySeconds by viewModel.delaySeconds.collectAsState()
    val blockedPackagesStr by viewModel.blockedPackages.collectAsState()
    val cooldownEnabled by viewModel.cooldownEnabled.collectAsState()
    val cooldownUsageMinutes by viewModel.cooldownUsageMinutes.collectAsState()
    val cooldownPeriodMinutes by viewModel.cooldownPeriodMinutes.collectAsState()

    var isServiceEnabled by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isServiceEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Minimalist Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ZEN DELAY",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.W200,
                        letterSpacing = 8.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "weź wdech • odzyskaj skupienie",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Divider
            item {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            }

            // Status Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "MONITOROWANIE SYSTEMU",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = if (isServiceEnabled) "Czuwanie włączone" else "Wymagana autoryzacja",
                            color = if (isServiceEnabled) Color(0xFF030712).run { Color(0xFF10B981) } else Color(0xFFFBBF24),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                    if (!isServiceEnabled) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Safe fallback
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "WŁĄCZ",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // Custom non-intrusive step layout if service is disabled
            if (!isServiceEnabled) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Instrukcja aktywacji usługi:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                        listOf(
                            "1. Kliknij przycisk „WŁĄCZ” powyżej.",
                            "2. Przejdź do zakładki „Zainstalowane aplikacje”.",
                            "3. Wybierz „ZenDelay” i zezwól na działanie usługi."
                        ).forEach { step ->
                            Text(
                                text = step,
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Divider
            item {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            }

            // Delay Selector Option Area
            item {
                DelaySelectorSection(
                    currentDelay = delaySeconds,
                    onDelayChanged = { viewModel.updateDelaySeconds(it) }
                )
            }

            // Divider
            item {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            }

            // Custom Blocked Packages Area
            item {
                BlockedPackagesSection(
                    packagesStr = blockedPackagesStr,
                    onPackagesChanged = { viewModel.updateBlockedPackages(it) }
                )
            }

            // Divider
            item {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            }

            // Cooldown session settings
            item {
                CooldownConfigSection(
                    cooldownEnabled = cooldownEnabled,
                    cooldownUsageMinutes = cooldownUsageMinutes,
                    cooldownPeriodMinutes = cooldownPeriodMinutes,
                    onCooldownEnabledChanged = { viewModel.updateCooldownEnabled(it) },
                    onCooldownUsageMinutesChanged = { viewModel.updateCooldownUsageMinutes(it) },
                    onCooldownPeriodMinutesChanged = { viewModel.updateCooldownPeriodMinutes(it) }
                )
            }

            // Divider
            item {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), thickness = 1.dp)
            }

            // Test Area Trigger Row
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "STREFA TESTOWA",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .clickable {
                                val testIntent = Intent(context, DelayActivity::class.java).apply {
                                    putExtra("target_package", "com.instagram.android")
                                }
                                context.startActivity(testIntent)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Test Delay Target",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Przetestuj widok pauzy",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DelaySelectorSection(
    currentDelay: Int,
    onDelayChanged: (Int) -> Unit
) {
    val options = listOf(3, 5, 10, 15, 30)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "CZAS ŚWIADOMEJ PAUZY",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = currentDelay == option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(6.dp)
                        )
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .clickable { onDelayChanged(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${option}s",
                        color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Light
                    )
                }
            }
        }
    }
}

@Composable
fun BlockedPackagesSection(
    packagesStr: String,
    onPackagesChanged: (String) -> Unit
) {
    val currentList = remember(packagesStr) {
        packagesStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    var customPackageInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "BLOKOWANE APLIKACJE",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )

        val instagramPackage = "com.instagram.android"
        val tiktokPackage = "com.zhiliaoapp.musically"
        val isInstagramBlocked = instagramPackage in currentList
        val isTiktokBlocked = tiktokPackage in currentList || "com.ss.android.ugc.aweme" in currentList

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Instagram" to (instagramPackage to isInstagramBlocked),
                "TikTok" to (tiktokPackage to isTiktokBlocked)
            ).forEach { (label, data) ->
                val pkg = data.first
                val isBlocked = data.second
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            1.dp,
                            if (isBlocked) Color.White else Color.White.copy(alpha = 0.10f),
                            RoundedCornerShape(6.dp)
                        )
                        .background(if (isBlocked) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable {
                            val newList = currentList.toMutableList()
                            if (label == "TikTok") {
                                val tiktoks = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill", "com.ss.android.ugc.aweme")
                                if (isBlocked) newList.removeAll(tiktoks) else newList.addAll(tiktoks)
                            } else {
                                if (isBlocked) newList.remove(pkg) else newList.add(pkg)
                            }
                            onPackagesChanged(newList.joinToString(","))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(RoundedCornerShape(100))
                                .background(if (isBlocked) Color.White else Color.White.copy(alpha = 0.25f))
                        )
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customPackageInput,
                onValueChange = { customPackageInput = it.lowercase().trim() },
                placeholder = { Text("Wpisz pakiet, np. com.youtube...", fontSize = 12.sp, color = Color.White.copy(alpha = 0.25f)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.4f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
            )

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .clickable {
                        if (customPackageInput.isNotEmpty() && customPackageInput !in currentList) {
                            val newList = currentList.toMutableList().apply { add(customPackageInput) }
                            onPackagesChanged(newList.joinToString(","))
                            customPackageInput = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add custom package name",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (currentList.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currentList.forEach { pkg ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pkg,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete path tag",
                            tint = Color.White.copy(alpha = 0.35f),
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    val newList = currentList.toMutableList().apply { remove(pkg) }
                                    onPackagesChanged(newList.joinToString(","))
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", color = Color.White, modifier = modifier)
}

@Composable
fun CooldownConfigSection(
    cooldownEnabled: Boolean,
    cooldownUsageMinutes: Int,
    cooldownPeriodMinutes: Int,
    onCooldownEnabledChanged: (Boolean) -> Unit,
    onCooldownUsageMinutesChanged: (Int) -> Unit,
    onCooldownPeriodMinutesChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "OGRANICZENIE SESJI (COOLDOWN)",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Blokuj aplikację po wykorzystaniu limitu",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Light
                )
            }

            Switch(
                checked = cooldownEnabled,
                onCheckedChange = onCooldownEnabledChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color.White,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.3f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.05f),
                    uncheckedBorderColor = Color.White.copy(alpha = 0.1f)
                )
            )
        }

        AnimatedVisibility(
            visible = cooldownEnabled,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Maksymalny czas sesji:",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 3, 5, 10).forEach { option ->
                            val isSelected = cooldownUsageMinutes == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable { onCooldownUsageMinutesChanged(option) },
                                contentAlignment = Alignment.Center
                              ) {
                                  Text(
                                      text = "${option} min",
                                      color = Color.White,
                                      fontSize = 11.sp,
                                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                  )
                              }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Czas trwania blokady (cooldown):",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(5, 10, 15, 30).forEach { option ->
                            val isSelected = cooldownPeriodMinutes == option
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color.White else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                      )
                                      .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                                      .clickable { onCooldownPeriodMinutesChanged(option) },
                                  contentAlignment = Alignment.Center
                              ) {
                                  Text(
                                      text = "${option} min",
                                      color = Color.White,
                                      fontSize = 11.sp,
                                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                  )
                              }
                        }
                    }
                }
            }
        }
    }
}
