package com.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppDatabase
import com.example.data.AppDelayManager
import com.example.data.SettingRepository
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DelayActivity : ComponentActivity() {

    private var targetPackage: String = ""
    private var isCooldownMode: Boolean = false
    private var cooldownRemainingMs: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""
        isCooldownMode = intent.getBooleanExtra(EXTRA_IS_COOLDOWN, false)
        cooldownRemainingMs = intent.getLongExtra(EXTRA_COOLDOWN_REMAINING_MS, 0L)

        if (targetPackage.isEmpty()) {
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    DelayScreen(
                        targetPackage = targetPackage,
                        isCooldownMode = isCooldownMode,
                        initialCooldownMs = cooldownRemainingMs,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onDelayFinished = {
                            launchTargetApp()
                        },
                        onCancel = {
                            goHomeAndFinish()
                        }
                    )
                }
            }
        }
    }

    private fun launchTargetApp() {
        Log.d(TAG, "Pause finished, allowing $targetPackage")
        AppDelayManager.grantAccess(targetPackage)

        try {
            val intent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                Log.e(TAG, "No launch intent for $targetPackage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching $targetPackage", e)
        } finally {
            finish()
        }
    }

    private fun goHomeAndFinish() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    @Deprecated("Deprecated in Java", ReplaceWith("goHomeAndFinish()"))
    override fun onBackPressed() {
        goHomeAndFinish()
    }

    companion object {
        private const val TAG = "DelayActivity"

        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_IS_COOLDOWN = "is_cooldown"
        const val EXTRA_COOLDOWN_REMAINING_MS = "cooldown_remaining_ms"
    }
}

@Composable
fun DelayScreen(
    targetPackage: String,
    isCooldownMode: Boolean,
    initialCooldownMs: Long,
    modifier: Modifier = Modifier,
    onDelayFinished: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var totalSeconds by remember { mutableStateOf(5) }
    var secondsLeft by remember { mutableStateOf(5) }
    var cooldownRemainingSec by remember { mutableStateOf((initialCooldownMs / 1000).toInt().coerceAtLeast(1)) }
    var isLoaded by remember { mutableStateOf(false) }

    // Read general conscious delay seconds
    LaunchedEffect(Unit) {
        val db = AppDatabase.getDatabase(context)
        val repository = SettingRepository(db.settingDao())
        val delaySetting = repository.getDelaySeconds()
        totalSeconds = delaySetting
        secondsLeft = delaySetting
        isLoaded = true
    }

    if (!isLoaded) {
        Box(
            modifier = modifier.background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
        }
        return
    }

    val progressAnim = remember { Animatable(1f) }

    // Sync timer mechanics
    LaunchedEffect(isCooldownMode, totalSeconds) {
        if (isCooldownMode) {
            // Cooldown ticker loop
            while (cooldownRemainingSec > 0) {
                delay(1000L)
                cooldownRemainingSec--
            }
            onCancel() // Close lock screen once punishment expires
        } else {
            // General countdown
            progressAnim.snapTo(1f)
            launch {
                progressAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = totalSeconds * 1000,
                        easing = LinearEasing
                    )
                )
            }

            for (i in totalSeconds downTo 1) {
                secondsLeft = i
                delay(1000L)
            }
            secondsLeft = 0
            onDelayFinished()
        }
    }

    // Determine target app display name
    val appDisplayName = when {
        targetPackage.contains("instagram") -> "Instagram"
        targetPackage.contains("tiktok") || targetPackage.contains("musically") -> "TikTok"
        else -> "Aplikacji"
    }

    // Dopamine / Detox education facts with premium typography weight
    val facts = listOf(
        Pair(
            "🧠 DOPAMINOWA PĘTLA",
            "Uruchomienie social mediów powoduje nagły skok dopaminy o ponad 150%. To sztucznie przeciąża mózg i niszczy naturalną motywację."
        ),
        Pair(
            "⏱️ KRADZIEŻ CZASU",
            "Przeciętna sesja trwa aż 28 minut, choć planujemy wejść tylko na sekundę. Tracisz bezpowrotnie ponad 200 godzin rocznie!"
        ),
        Pair(
            "💔 USZKODZENIE SKUPIENIA",
            "Krótkie wideo niszczą zdolność do głębokiej koncentracji. Uszczerbek uwagi utrzymuje się nawet do 2 godzin po odłożeniu telefonu."
        ),
        Pair(
            "🎰 UKŁAD UZALEŻNIEŃ",
            "Mechanizm nieskończonego przewijania (feed) działa dokładnie jak jednoręki bandyta. Został celowo zaprojektowany, by wywołać przymus."
        ),
        Pair(
            "🧘 RESET IMPULSU",
            "Pauza, którą właśnie robisz, pozwala korze przedczołowej przejąć kontrolę. 5-10 sekund wystarcza, by opanować automatyczny nawyk."
        ),
        Pair(
            "🌳 AUTONOMICZNA NUDA",
            "Ciągłe bodźce blokują naturalne procesy myślowe. Nuda jest kluczowa dla kreatywności i regeneracji układu nerwowego."
        )
    )

    var currentFactIndex by remember { mutableStateOf((facts.indices).random()) }
    val currentFact = facts[currentFactIndex]

    // Pure Clean Black & White Meditative Grid Layout
    Column(
        modifier = modifier
            .background(Color(0xFF000000))
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App / Context Label Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = if (isCooldownMode) "TRYB COOLDOWN • ZABLOKOWANE" else "ŚWIADOMY ODDECH",
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = if (isCooldownMode) "Twoje 5 minut na $appDisplayName dobiegło końca" else "Wchodzisz na aplikację $appDisplayName",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )
        }

        // Giant Monochrome Numeric Counter Area
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(220.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            if (!isCooldownMode) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = 360f * progressAnim.value,
                        useCenter = false,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isCooldownMode) {
                    val minutes = cooldownRemainingSec / 60
                    val seconds = cooldownRemainingSec % 60
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        color = Color.White,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.W100,
                        letterSpacing = (-1).sp
                    )
                    Text(
                        text = "odwyk trwa",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                } else {
                    Text(
                        text = "$secondsLeft",
                        color = Color.White,
                        fontSize = 84.sp,
                        fontWeight = FontWeight.W100,
                        letterSpacing = (-2).sp
                    )
                    Text(
                        text = "sekund",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        // High-readability educational dopamine facts (Deep Zen black focus card, larger readable typography)
        Surface(
            color = Color(0xFF0F0F12),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = SolidColor(Color.White.copy(alpha = 0.12f))
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentFact.first,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = "LOSUJ 🔄",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                var newIndex = currentFactIndex
                                while (newIndex == currentFactIndex && facts.size > 1) {
                                    newIndex = (facts.indices).random()
                                }
                                currentFactIndex = newIndex
                            }
                            .padding(4.dp)
                    )
                }

                // Fact Body with enhanced large typography
                Text(
                    text = currentFact.second,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        // Action Trigger Cancel Bar
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel Icon",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Rezygnuję (Uratuj swój czas)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
