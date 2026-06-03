package com.example.data

import kotlin.random.Random

/**
 * A single multiple-choice challenge. [options] always contains exactly the right answer at
 * [correctIndex]; everything is computed, never hard-coded for the math/logic types, so the
 * "price" for bonus scrolling time can't be memorised.
 */
data class Quiz(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
)

/**
 * Offline generator of deliberately non-trivial questions (modular exponentiation, number
 * theory, integer sequences, knights-and-knaves logic, bit twiddling) plus a small bank of
 * hard logic/knowledge questions. No network, instant, free.
 */
object QuizGenerator {

    fun next(random: Random = Random.Default): Quiz =
        when (random.nextInt(6)) {
            0 -> powerMod(random)
            1 -> divisorCount(random)
            2 -> sequence(random)
            3 -> knightsAndKnaves(random)
            4 -> bitTwiddle(random)
            else -> fromBank(random)
        }

    // --- Generators -----------------------------------------------------------

    /** b^e mod m — needs Fermat or fast exponentiation to do in your head. */
    private fun powerMod(random: Random): Quiz {
        val base = random.nextInt(2, 10)
        val mod = listOf(7, 11, 13, 17, 19, 23).random(random)
        val exp = random.nextInt(40, 200)
        val answer = modPow(base.toLong(), exp.toLong(), mod.toLong()).toInt()
        return numericQuiz(
            prompt = "Ile wynosi $base^$exp mod $mod?",
            answer = answer,
            random = random,
            distractor = { random.nextInt(0, mod) },
        )
    }

    /** Number of positive divisors of n = ∏ pᵢ^aᵢ  →  ∏ (aᵢ + 1). */
    private fun divisorCount(random: Random): Quiz {
        val primes = listOf(2, 3, 5, 7, 11)
        val chosen = primes.shuffled(random).take(random.nextInt(2, 4))
        var n = 1L
        var answer = 1
        for (p in chosen) {
            val a = random.nextInt(1, 4)
            repeat(a) { n *= p }
            answer *= (a + 1)
        }
        return numericQuiz(
            prompt = "Ile dodatnich dzielników ma liczba $n?",
            answer = answer,
            random = random,
            distractor = { (answer + random.nextInt(-4, 5)).coerceAtLeast(1) },
        )
    }

    /** Quadratic integer sequence aₙ = An² + Bn + C; predict the 6th term from the first 5. */
    private fun sequence(random: Random): Quiz {
        val a = random.nextInt(1, 4)
        val b = random.nextInt(-4, 6)
        val c = random.nextInt(-3, 6)
        val term = { n: Int -> a * n * n + b * n + c }
        val shown = (1..5).joinToString(", ") { term(it).toString() }
        val answer = term(6)
        return numericQuiz(
            prompt = "Podaj następny wyraz ciągu: $shown, ?",
            answer = answer,
            random = random,
            distractor = { answer + listOf(-a, a, 2 * a, b, -b, a + b).random(random) },
        )
    }

    /** Knights (truth) and knaves (lie): generate random statements, keep only puzzles with a
     *  unique consistent assignment, ask how many tell the truth. */
    private fun knightsAndKnaves(random: Random): Quiz {
        val names = listOf("A", "B", "C")
        repeat(40) {
            // statement[i] = (target j, claimTruthful) — person i claims j is truthful/a liar.
            val statements = (0..2).map { i ->
                val j = ((i + 1 + random.nextInt(2)) % 3)
                j to random.nextBoolean()
            }
            val consistent = (0..7).filter { mask ->
                val truthful = BooleanArray(3) { (mask shr it) and 1 == 1 }
                (0..2).all { i ->
                    val (j, claimTruthful) = statements[i]
                    val claim = truthful[j] == claimTruthful
                    truthful[i] == claim
                }
            }
            if (consistent.size == 1) {
                val mask = consistent.single()
                val answer = Integer.bitCount(mask)
                val lines = statements.mapIndexed { i, (j, claimTruthful) ->
                    val verb = if (claimTruthful) "mówi prawdę" else "kłamie"
                    "${names[i]} mówi: «${names[j]} $verb»"
                }
                return Quiz(
                    prompt = lines.joinToString(". ") + ". Ile osób mówi prawdę?",
                    options = (0..3).map { it.toString() },
                    correctIndex = answer,
                )
            }
        }
        return fromBank(random)
    }

    private fun bitTwiddle(random: Random): Quiz {
        val x = random.nextInt(1, 256)
        return when (random.nextInt(2)) {
            0 -> numericQuiz(
                prompt = "Ile bitów równych 1 ma binarny zapis liczby $x?",
                answer = Integer.bitCount(x),
                random = random,
                distractor = { random.nextInt(0, 9) },
            )
            else -> {
                val y = random.nextInt(1, 256)
                numericQuiz(
                    prompt = "Ile wynosi $x XOR $y? (operacja bitowa)",
                    answer = x xor y,
                    random = random,
                    distractor = { (x xor y) + random.nextInt(-30, 31) },
                )
            }
        }
    }

    private fun fromBank(random: Random): Quiz = BANK.random(random)

    // --- Helpers --------------------------------------------------------------

    /** Build a 4-option quiz from a correct number plus distinct distractors. */
    private fun numericQuiz(
        prompt: String,
        answer: Int,
        random: Random,
        distractor: () -> Int,
    ): Quiz {
        val options = linkedSetOf(answer)
        var guard = 0
        while (options.size < 4 && guard++ < 200) {
            val d = distractor()
            if (d != answer) options.add(d)
        }
        // Pad deterministically in the unlikely event the distractor source ran dry.
        var pad = answer + 1
        while (options.size < 4) options.add(pad++)
        val shuffled = options.toList().shuffled(random).map { it.toString() }
        return Quiz(prompt, shuffled, shuffled.indexOf(answer.toString()))
    }

    /** Fast modular exponentiation, base^exp mod m. */
    internal fun modPow(base: Long, exp: Long, mod: Long): Long {
        var result = 1L
        var b = base % mod
        var e = exp
        while (e > 0) {
            if (e and 1L == 1L) result = result * b % mod
            b = b * b % mod
            e = e shr 1
        }
        return result
    }

    /** Hard logic/knowledge questions for variety. correctIndex points at the right option. */
    private val BANK: List<Quiz> = listOf(
        Quiz(
            "Pociąg jedzie 60 km z prędkością 60 km/h, potem 60 km z prędkością 30 km/h. " +
                "Jaka jest średnia prędkość na całej trasie?",
            listOf("45 km/h", "40 km/h", "50 km/h", "36 km/h"),
            1,
        ),
        Quiz(
            "Ile wynosi złożoność czasowa wyszukiwania binarnego w posortowanej tablicy n elementów?",
            listOf("O(n)", "O(log n)", "O(n log n)", "O(1)"),
            1,
        ),
        Quiz(
            "Rzucasz dwiema uczciwymi kostkami. Jakie jest prawdopodobieństwo sumy równej 7?",
            listOf("1/6", "1/8", "1/12", "1/9"),
            0,
        ),
        Quiz(
            "Która liczba jest pierwsza?",
            listOf("91", "87", "97", "51"),
            2,
        ),
        Quiz(
            "Ile różnych sposobów ustawienia 4 osób w rzędzie?",
            listOf("16", "12", "24", "256"),
            2,
        ),
        Quiz(
            "Jeśli wszystkie Bloopy są Razzami, a niektóre Razzy są Lazzami, to które zdanie jest na pewno prawdziwe?",
            listOf(
                "Niektóre Bloopy są Lazzami",
                "Wszystkie Lazzy są Bloopami",
                "Żaden Bloop nie jest Lazzem",
                "Żadne z powyższych nie musi być prawdą",
            ),
            3,
        ),
        Quiz(
            "Ile bajtów ma adres IPv6?",
            listOf("4", "8", "16", "32"),
            2,
        ),
        Quiz(
            "Pochodna funkcji f(x) = x·ln(x) to:",
            listOf("ln(x)", "ln(x) + 1", "1/x", "x/ln(x)"),
            1,
        ),
        Quiz(
            "Ile wynosi 0.1 + 0.2 w arytmetyce zmiennoprzecinkowej IEEE 754 (double)?",
            listOf("Dokładnie 0.3", "Nieco więcej niż 0.3", "Nieco mniej niż 0.3", "0.30000000001 dokładnie"),
            1,
        ),
        Quiz(
            "Masz 8 kul, jedna jest cięższa. Ile minimalnie ważeń na wadze szalkowej, by ją znaleźć?",
            listOf("2", "3", "4", "1"),
            0,
        ),
    )
}
