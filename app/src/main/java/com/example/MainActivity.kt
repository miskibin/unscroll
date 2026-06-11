package com.example

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BudgetSnapshot
import com.example.detection.Platforms
import com.example.ui.theme.UnscrollTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnscrollTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshServiceState()
    }
}

@Composable
private fun DashboardScreen(
    viewModel: MainViewModel,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    val serviceEnabled by viewModel.serviceEnabledInSystem.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(serviceEnabled = serviceEnabled, guardEnabled = settings.enabled)

            if (!serviceEnabled) {
                SetupCard(onOpenAccessibilitySettings)
            } else {
                BudgetHero(budget = budget, now = now)
            }

            MasterSwitchCard(
                enabled = settings.enabled,
                onToggle = viewModel::setEnabled,
            )

            RuleCard(
                allowanceSeconds = settings.allowanceSeconds,
                windowMinutes = settings.windowMinutes,
                onAllowanceChange = viewModel::setAllowanceSeconds,
                onWindowChange = viewModel::setWindowMinutes,
            )

            AppsCard(
                enabledPlatforms = settings.enabledPlatforms,
                onToggle = viewModel::setPlatformEnabled,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Header(serviceEnabled: Boolean, guardEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        val (label, color) = when {
            !serviceEnabled -> stringResource(R.string.status_service_off) to MaterialTheme.colorScheme.error
            !guardEnabled -> stringResource(R.string.status_paused) to MaterialTheme.colorScheme.onSurfaceVariant
            else -> stringResource(R.string.status_active) to MaterialTheme.colorScheme.primary
        }
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(color, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SetupCard(onOpenAccessibilitySettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.setup_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.setup_body,
                    stringResource(R.string.accessibility_service_label),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.setup_button))
            }
        }
    }
}

@Composable
private fun BudgetHero(budget: BudgetSnapshot, now: Long) {
    // The service publishes a snapshot; extrapolate the reset countdown
    // locally so the dashboard ticks even while the service is idle.
    val elapsedSinceUpdate = (now - budget.updatedAt).coerceAtLeast(0)
    val resetInMs = (budget.resetInMs - elapsedSinceUpdate).coerceAtLeast(0)
    val exhausted = budget.exhausted && resetInMs > 0
    val remainingMs = if (resetInMs == 0L && budget.usedMs > 0) {
        budget.allowanceMs
    } else {
        (budget.allowanceMs - budget.usedMs).coerceAtLeast(0)
    }
    val progress = if (budget.allowanceMs > 0) {
        remainingMs.toFloat() / budget.allowanceMs.toFloat()
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(180.dp),
                    strokeWidth = 10.dp,
                    color = if (exhausted) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatMmSs(remainingMs),
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.budget_remaining),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = when {
                    exhausted -> stringResource(R.string.budget_locked, formatMmSs(resetInMs))
                    resetInMs > 0 -> stringResource(R.string.budget_refills_in, formatMmSs(resetInMs))
                    else -> stringResource(R.string.budget_full)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (exhausted) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MasterSwitchCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.master_switch_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.master_switch_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun RuleCard(
    allowanceSeconds: Int,
    windowMinutes: Int,
    onAllowanceChange: (Int) -> Unit,
    onWindowChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.rules_title),
                style = MaterialTheme.typography.titleMedium,
            )
            StepperRow(
                label = stringResource(R.string.rules_allowance),
                value = formatAllowance(allowanceSeconds),
                onDecrement = { onAllowanceChange(allowanceSeconds - 30) },
                onIncrement = { onAllowanceChange(allowanceSeconds + 30) },
            )
            StepperRow(
                label = stringResource(R.string.rules_window),
                value = stringResource(R.string.minutes_short, windowMinutes),
                onDecrement = { onWindowChange(windowMinutes - 5) },
                onIncrement = { onWindowChange(windowMinutes + 5) },
            )
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onDecrement,
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.size(40.dp),
            ) { Text("−", style = MaterialTheme.typography.titleMedium) }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(88.dp),
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onIncrement,
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier.size(40.dp),
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun AppsCard(
    enabledPlatforms: Set<String>,
    onToggle: (String, Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.apps_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            Platforms.all.forEach { platform ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = platform.label,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = platform.key in enabledPlatforms,
                        onCheckedChange = { onToggle(platform.key, it) },
                    )
                }
            }
        }
    }
}

private fun formatMmSs(ms: Long): String {
    val totalSeconds = (ms + 999) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

private fun formatAllowance(seconds: Int): String =
    if (seconds % 60 == 0) "${seconds / 60} min" else "${seconds / 60}:%02d".format(seconds % 60)
