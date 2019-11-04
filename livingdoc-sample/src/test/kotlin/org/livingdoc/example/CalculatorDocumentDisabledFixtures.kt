package org.livingdoc.example

import org.junit.jupiter.api.fail
import org.livingdoc.api.disabled.Disabled
import org.livingdoc.api.documents.ExecutableDocument
import org.livingdoc.api.fixtures.decisiontables.Check
import org.livingdoc.api.fixtures.decisiontables.DecisionTableFixture
import org.livingdoc.api.fixtures.decisiontables.Input
import org.livingdoc.api.fixtures.scenarios.Binding
import org.livingdoc.api.fixtures.scenarios.ScenarioFixture
import org.livingdoc.api.fixtures.scenarios.Step

@ExecutableDocument("local://Calculator.md")
class CalculatorDocumentDisabledFixtures {

    @Disabled("Disabled ScenarioFixture")
    @ScenarioFixture
    class DisabledCalculatorScenarioFixture {

        @Step("adding {a} and {b} equals {c}")
        fun add(
            @Binding("a") a: Float,
            @Binding("b") b: Float,
            @Binding("c") c: Float
        ) {
            assert(false)
        }

        @Step("subtraction {b} form {a} equals {c}")
        fun subtract(
            @Binding("a") a: Float,
            @Binding("b") b: Float,
            @Binding("c") c: Float
        ) {
            fail("Test should be ignored")
        }

        @Step("multiplying {a} and {b} equals {c}")
        fun multiply(
            @Binding("a") a: Float,
            @Binding("b") b: Float,
            @Binding("c") c: Float
        ) {
            fail("Test should be ignored")
        }

        @Step("dividing {a} by {b} equals {c}")
        fun divide(
            @Binding("a") a: Float,
            @Binding("b") b: Float,
            @Binding("c") c: Float
        ) {
            fail("Test should be ignored")
        }

        @Step("add {a} to itself and you get {b}")
        fun selfadd(
            @Binding("a") a: Float,
            @Binding("b") b: Float
        ) {
            fail("Test should be ignored")
        }
    }

    @Disabled("Disabled DecisionTableFixture")
    @DecisionTableFixture
    class DisabledCalculatorDecisionTableFixture {

        @Input("a")
        private var valueA: Float = 0f
        @Input("b")
        private var valueB: Float = 0f

        @Check("a + b = ?")
        fun checkSum(expectedValue: Float) {
            fail("Test should be ignored")
        }

        @Check("a - b = ?")
        fun checkDiff(expectedValue: Float) {
            fail("Test should be ignored")
        }

        @Check("a * b = ?")
        fun checkMultiply(expectedValue: Float) {
            fail("Test should be ignored")
        }

        @Check("a / b = ?")
        fun checkDivide(expectedValue: Float) {
            fail("Test should be ignored")
        }
    }
}
