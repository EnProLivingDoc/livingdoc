package org.livingdoc.jvm.decisiontable

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.livingdoc.jvm.api.extension.context.FixtureContext
import org.livingdoc.jvm.api.fixture.FixtureFactory
import org.livingdoc.repositories.model.decisiontable.DecisionTable
import org.livingdoc.repositories.model.decisiontable.Field
import org.livingdoc.repositories.model.decisiontable.Header
import org.livingdoc.repositories.model.decisiontable.Row
import kotlin.reflect.KClass

internal class DecisionTableFixtureFactoryTest {

    companion object {
        lateinit var cut: FixtureFactory<DecisionTable>

        @BeforeAll
        @JvmStatic
        fun `initialize factory`() {
            cut = DecisionTableFixtureFactory()
        }
    }

    @Test
    fun `can handle decisiontables`() {
        Assertions.assertThat(cut.isCompatible(DecisionTable(listOf(), listOf())))
    }

    @Test
    fun `empty decisiontable matches all fixtures`() {
        val decisionTable = DecisionTable(listOf(), listOf())

        Assertions.assertThat(cut.match(EmptyFixture::class, decisionTable)).isTrue()
        Assertions.assertThat(cut.match(CalculatorFixture::class, decisionTable)).isTrue()
    }

    @Test
    fun `decisiontable with headers and rows does not match an empty fixture`() {
        val headerA = Header("a")
        val headerB = Header("b")
        val headerAPlusB = Header("a + b = ?")
        val decisionTable = DecisionTable(
            listOf(
                headerA,
                headerB,
                headerAPlusB
            ),
            listOf(
                Row(mapOf(headerA to Field("1"), headerB to Field("2"), headerAPlusB to Field("3"))),
                Row(mapOf(headerA to Field("3"), headerB to Field("0"), headerAPlusB to Field("3")))
            )
        )

        Assertions.assertThat(cut.match(EmptyFixture::class, decisionTable)).isFalse()
    }

    @Test
    fun `decisiontable with headers and rows matches a fixture with correct check definitions`() {
        val headerA = Header("a")
        val headerB = Header("b")
        val headerAPlusB = Header("a + b = ?")
        val decisionTable = DecisionTable(
            listOf(
                headerA,
                headerB,
                headerAPlusB
            ),
            listOf(
                Row(mapOf(headerA to Field("1"), headerB to Field("2"), headerAPlusB to Field("3"))),
                Row(mapOf(headerA to Field("3"), headerB to Field("0"), headerAPlusB to Field("3")))
            )
        )

        Assertions.assertThat(cut.match(CalculatorFixture::class, decisionTable)).isTrue()
    }

    @Test
    fun `decisiontable with headers and rows does not match fixture with incorrect check definitions`() {
        val headerA = Header("a")
        val headerB = Header("b")
        val headerAPlusB = Header("a / b = ?")
        val decisionTable = DecisionTable(
            listOf(
                headerA,
                headerB,
                headerAPlusB
            ),
            listOf(
                Row(mapOf(headerA to Field("1"), headerB to Field("2"), headerAPlusB to Field("0.5"))),
                Row(mapOf(headerA to Field("3"), headerB to Field("1"), headerAPlusB to Field("3")))
            )
        )

        Assertions.assertThat(cut.match(CalculatorFixture::class, decisionTable)).isFalse()
    }

    @Test
    fun `can create correct fixture from context`() {
        val fixture = mockk<KClass<*>>()
        val context = mockk<FixtureContext>()
        every { context.fixtureClass } returns fixture

        Assertions.assertThat(cut.getFixture(context)).isInstanceOfSatisfying(DecisionTableFixture::class.java) {
            Assertions.assertThat(it.fixtureClass).isEqualTo(fixture)
        }
    }
}