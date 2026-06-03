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
import androidx.compose.foundation.border
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
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.AppDelayManager
import com.example.data.EventLogger
import com.example.data.EventOutcome
import com.example.data.Quiz
import com.example.data.QuizGenerator
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
                        onEnter = { reasonName ->
                            EventLogger.log(this, EventOutcome.ENTERED, reasonName)
                            launchTargetApp()
                        },
                        onTurnAway = { reasonName ->
                            EventLogger.log(this, EventOutcome.TURNED_AWAY, reasonName)
                            goHomeAndFinish()
                        },
                        onEarnedReward = {
                            EventLogger.log(this, EventOutcome.BONUS, null)
                            earnBonusTimeAndLaunch()
                        },
                        onCooldownEnded = {
                            goHomeAndFinish()
                        }
                    )
                }
            }
        }
    }

    /** Conscious pause finished: start a usage session sized by the user's cooldown settings. */
    private fun launchTargetApp() {
        Log.d(TAG, "Pause finished, allowing $targetPackage")
        lifecycleScope.launch {
            val repository = SettingRepository(AppDatabase.getDatabase(this@DelayActivity).settingDao())
            val sessionLimit = if (repository.isCooldownEnabled()) repository.getCooldownUsageMinutes() else 0
            AppDelayManager.grantAccess(targetPackage, sessionLimit)
            launchPackageAndFinish()
        }
    }

    /** The user earned their way out of odwyk: grant the bonus session and let them in. */
    private fun earnBonusTimeAndLaunch() {
        Log.d(TAG, "Bonus time earned for $targetPackage")
        AppDelayManager.grantBonusTime(targetPackage, BONUS_MINUTES)
        launchPackageAndFinish()
    }

    private fun launchPackageAndFinish() {
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

        /** Correct answers needed in a row to earn bonus time, and the reward. */
        const val QUIZ_STREAK_GOAL = 3
        const val BONUS_MINUTES = 5

        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_IS_COOLDOWN = "is_cooldown"
        const val EXTRA_COOLDOWN_REMAINING_MS = "cooldown_remaining_ms"
    }
}

/** Why the user is opening a blocked app. Purposeful reasons get a much lighter pause. */
enum class EntryReason { MESSAGE, SPECIFIC, BROWSING, BORED }

/**
 * Step one of the pause: a single, judgement-free question. Naming the intention is itself an
 * evidence-backed nudge (implementation intentions + self-monitoring), and giving purposeful uses
 * a fast lane avoids the reactance and false-positives that make blockers get uninstalled.
 */
@Composable
private fun IntentionScreen(
    appDisplayName: String,
    modifier: Modifier = Modifier,
    onPick: (EntryReason) -> Unit
) {
    Column(
        modifier = modifier
            .background(Color(0xFF000000))
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ZANIM WEJDZIESZ",
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Po co otwierasz $appDisplayName?",
            color = Color.White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )
        Spacer(modifier = Modifier.height(36.dp))

        listOf(
            EntryReason.MESSAGE to "Napisać do kogoś",
            EntryReason.SPECIFIC to "Sprawdzić coś konkretnego",
            EntryReason.BROWSING to "Tylko przeglądam",
            EntryReason.BORED to "Z nudów / odruchowo"
        ).forEach { (entryReason, label) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .clickable { onPick(entryReason) }
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Sama ta chwila zastanowienia już działa.",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DelayScreen(
    targetPackage: String,
    isCooldownMode: Boolean,
    initialCooldownMs: Long,
    modifier: Modifier = Modifier,
    onEnter: (reasonName: String?) -> Unit,
    onTurnAway: (reasonName: String?) -> Unit,
    onEarnedReward: () -> Unit = {},
    onCooldownEnded: () -> Unit = {}
) {
    val context = LocalContext.current
    var totalSeconds by remember { mutableStateOf(5) }
    var secondsLeft by remember { mutableStateOf(5) }
    var cooldownRemainingSec by remember { mutableStateOf((initialCooldownMs / 1000).toInt().coerceAtLeast(1)) }
    var isLoaded by remember { mutableStateOf(false) }
    var earningTime by remember { mutableStateOf(false) }
    var intentionPlan by remember { mutableStateOf("") }
    // Why the user is opening the app — chosen on the first step of the pause (null until then).
    var reason by remember { mutableStateOf<EntryReason?>(null) }

    // Read the user's settings (delay length + their own if-then plan)
    LaunchedEffect(Unit) {
        val db = AppDatabase.getDatabase(context)
        val repository = SettingRepository(db.settingDao())
        totalSeconds = repository.getDelaySeconds()
        secondsLeft = repository.getDelaySeconds()
        intentionPlan = repository.getIntentionPlan()
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

    // A purposeful reason gets a short, respectful beat; aimless ones get the full pause.
    val effectiveSeconds = when (reason) {
        EntryReason.MESSAGE, EntryReason.SPECIFIC -> minOf(totalSeconds, 2)
        else -> totalSeconds
    }

    // Timer: cooldown counts down to the end of the focus block; the pause counts the breath.
    LaunchedEffect(isCooldownMode, reason, effectiveSeconds, isLoaded) {
        if (!isLoaded) return@LaunchedEffect
        if (isCooldownMode) {
            while (cooldownRemainingSec > 0) {
                delay(1000L)
                cooldownRemainingSec--
            }
            onCooldownEnded() // Close the focus-block screen once it naturally ends
        } else {
            if (reason == null) return@LaunchedEffect // still on the intention step
            secondsLeft = effectiveSeconds
            progressAnim.snapTo(1f)
            launch {
                progressAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = effectiveSeconds * 1000,
                        easing = LinearEasing
                    )
                )
            }

            for (i in effectiveSeconds downTo 1) {
                secondsLeft = i
                delay(1000L)
            }
            secondsLeft = 0
            onEnter(reason?.name?.lowercase())
        }
    }

    // Determine target app display name
    val appDisplayName = when {
        targetPackage.contains("instagram") -> "Instagram"
        targetPackage.contains("tiktok") || targetPackage.contains("musically") -> "TikTok"
        else -> "Aplikacji"
    }

    // During a focus block the user can earn a few minutes by solving a streak of hard questions.
    if (isCooldownMode && earningTime) {
        QuizScreen(
            modifier = modifier,
            onSolved = onEarnedReward,
            onGiveUp = { earningTime = false }
        )
        return
    }

    // Intention-first: ask why before adding any friction (implementation-intention + self-monitoring).
    if (!isCooldownMode && reason == null) {
        IntentionScreen(
            appDisplayName = appDisplayName,
            modifier = modifier,
            onPick = { reason = it }
        )
        return
    }

    // What to reflect on while the breath runs — the user's own plan, or a calm, honest nudge.
    val cardLabel = when {
        isCooldownMode -> "TWÓJ BLOK SKUPIENIA"
        intentionPlan.isNotBlank() -> "TWÓJ PLAN"
        else -> "NA SPOKOJNIE"
    }
    val cardBody = when {
        isCooldownMode ->
            "Ten czas wybrałeś dla siebie. Świat poczeka — wrócisz do tego za chwilę."
        intentionPlan.isNotBlank() ->
            intentionPlan
        reason == EntryReason.BORED ->
            "Nuda to nie problem do rozwiązania. Daj jej chwilę — często pomysł przychodzi sam."
        reason == EntryReason.BROWSING ->
            "Przeglądasz bez celu i to jest OK. Pytanie tylko: czy teraz naprawdę tego chcesz?"
        else ->
            "Wejdź, zrób swoje i wyjdź. Trzymam kciuki."
    }

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
                text = if (isCooldownMode) "BLOK SKUPIENIA" else "ŚWIADOMY ODDECH",
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = if (isCooldownMode) "Robisz sobie przerwę od $appDisplayName" else "Za chwilę otworzysz $appDisplayName",
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
                        text = "do końca przerwy",
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

        // Calm reflection card: the user's own plan, or an honest, non-shaming nudge.
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
                Text(
                    text = cardLabel,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = cardBody,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        // Action bar: during odwyk, offer to earn bonus time; otherwise just bail out.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isCooldownMode) {
                Button(
                    onClick = { earningTime = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Muszę wejść — ${DelayActivity.QUIZ_STREAK_GOAL} zagadki (+${DelayActivity.BONUS_MINUTES} min)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                OutlinedButton(
                    onClick = { onTurnAway(null) },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Zostawiam to na teraz",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )
                }
            } else {
                Button(
                    onClick = { onTurnAway(reason?.name?.lowercase()) },
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
                        text = "Odpuszczam — odzyskuję chwilę",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

/**
 * The "earn bonus time" flow shown during odwyk: answer [DelayActivity.QUIZ_STREAK_GOAL] hard
 * questions in a row (a wrong answer resets the streak) to unlock bonus scrolling minutes.
 */
@Composable
private fun QuizScreen(
    modifier: Modifier = Modifier,
    onSolved: () -> Unit,
    onGiveUp: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var streak by remember { mutableStateOf(0) }
    var quiz by remember { mutableStateOf(QuizGenerator.next()) }
    var selectedIndex by remember { mutableStateOf(-1) }
    var locked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(Color(0xFF000000))
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header + streak progress
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(
                text = "ZARÓB CZAS • ${streak}/${DelayActivity.QUIZ_STREAK_GOAL}",
                color = Color.White.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(DelayActivity.QUIZ_STREAK_GOAL) { i ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(100))
                            .background(if (i < streak) Color.White else Color.White.copy(alpha = 0.15f))
                    )
                }
            }
        }

        // Question
        Text(
            text = quiz.prompt,
            color = Color.White,
            fontSize = 19.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Answer options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            quiz.options.forEachIndexed { index, option ->
                val isCorrect = index == quiz.correctIndex
                val borderColor = when {
                    !locked -> Color.White.copy(alpha = 0.15f)
                    isCorrect -> Color(0xFF10B981)
                    index == selectedIndex -> Color(0xFFEF4444)
                    else -> Color.White.copy(alpha = 0.1f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable(enabled = !locked) {
                            selectedIndex = index
                            locked = true
                            val correct = index == quiz.correctIndex
                            scope.launch {
                                delay(650L)
                                if (correct) {
                                    streak += 1
                                    if (streak >= DelayActivity.QUIZ_STREAK_GOAL) {
                                        onSolved()
                                        return@launch
                                    }
                                } else {
                                    streak = 0
                                }
                                quiz = QuizGenerator.next()
                                selectedIndex = -1
                                locked = false
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = option,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        OutlinedButton(
            onClick = onGiveUp,
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = "Poddaję się",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.5.sp
            )
        }
    }
}
