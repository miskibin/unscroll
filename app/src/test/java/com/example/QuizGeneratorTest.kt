package com.example

import com.example.data.QuizGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * The bonus-time quiz must always produce a well-formed, single-answer multiple choice question.
 * If the "correct" answer were ever wrong or missing, the user could never earn their time back.
 */
class QuizGeneratorTest {

    @Test
    fun `every generated quiz is a valid 4-option single-answer question`() {
        // Many seeds to exercise all generator branches and the unique-solution logic puzzles.
        for (seed in 0 until 2000) {
            val quiz = QuizGenerator.next(Random(seed))
            assertEquals("seed $seed should have 4 options", 4, quiz.options.size)
            assertTrue(
                "seed $seed correctIndex out of range",
                quiz.correctIndex in quiz.options.indices
            )
            assertEquals(
                "seed $seed has duplicate options: ${quiz.options}",
                quiz.options.size,
                quiz.options.toSet().size
            )
            assertTrue("seed $seed has a blank prompt", quiz.prompt.isNotBlank())
        }
    }

    @Test
    fun `modular exponentiation helper is correct`() {
        assertEquals(9L, QuizGenerator.modPow(7, 100, 13))
        assertEquals(445L, QuizGenerator.modPow(4, 13, 497))
        assertEquals(0L, QuizGenerator.modPow(10, 5, 100))
        assertEquals(1L, QuizGenerator.modPow(2, 0, 7))
    }
}
