package io.github.evaogbe.diswantin.task.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class FindOccurrencesTest(private val query: String, private val expected: List<IntRange>) {
    companion object {
        @JvmStatic
        @get:Parameters
        val data = listOf(
            arrayOf("B", listOf((0..0))),
            arrayOf("Bru", listOf((0..2))),
            arrayOf("Brush ", listOf((0..4))),
            arrayOf("Brush t", listOf((0..4), (6..6), (9..9))),
            arrayOf("Brush teeth", listOf((0..4), (6..10))),
        )
    }

    @Test
    fun `returns inclusive indices of words in query`() {
        assertThat("Brush teeth".findOccurrences(query)).isEqualTo(expected)
    }
}
