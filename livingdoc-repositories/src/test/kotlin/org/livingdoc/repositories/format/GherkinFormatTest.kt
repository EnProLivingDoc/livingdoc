package org.livingdoc.repositories.format

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.livingdoc.repositories.DocumentFormat
import org.livingdoc.repositories.model.scenario.Scenario
import kotlin.random.Random
import kotlin.random.nextInt

internal class GherkinFormatTest {
    private val cut: DocumentFormat = GherkinFormat()

    @ParameterizedTest
    @ValueSource(strings = ["txt", "html", "md"])
    @MethodSource("generateRandomStrings")
    fun `cannot handle other files`(format: String) {
        assertThat(cut.canHandle(format)).isFalse()
    }

    @Test
    fun `can handle feature files`() {
        assertThat(cut.canHandle("feature")).isTrue()
    }

    @Test
    fun `can parse empty stream`() {
        val document = cut.parse("".byteInputStream())
        assertThat(document.elements).isEmpty()
    }

    @Test
    fun `can parse simple scenario`() {
        val document = cut.parse(simpleGherkin())

        assertThat(document.elements).hasOnlyOneElementSatisfying { element ->
            assertThat(element.description).satisfies { description ->
                assertThat(description.name).isEqualTo("Test Scenario")
                assertThat(description.isManual).isFalse()
            }

            assertThat(element).isInstanceOfSatisfying(Scenario::class.java) { scenario ->
                assertThat(scenario.steps).hasOnlyOneElementSatisfying { step ->
                    assertThat(step.value).isEqualTo("I test the Gherkin parser")
                }
            }
        }
    }

    @Test
    fun `can parse multiple scenarios`() {
        val document = cut.parse(multipleScenarioGherkin())

        assertThat(document.elements).hasSize(2)
        assertThat(document.elements[0]).satisfies { testData ->
            assertThat(testData.description).satisfies { description ->
                assertThat(description.name).isEqualTo("Test Scenario 1")
                assertThat(description.isManual).isFalse()
            }

            assertThat(testData).isInstanceOfSatisfying(Scenario::class.java) { scenario ->
                assertThat(scenario.steps).hasOnlyOneElementSatisfying { step ->
                    assertThat(step.value).isEqualTo("I test the Gherkin parser")
                }
            }
        }
        assertThat(document.elements[1]).satisfies { testData ->
            assertThat(testData.description).satisfies { description ->
                assertThat(description.name).isEqualTo("Test Scenario 2")
                assertThat(description.isManual).isFalse()
            }

            assertThat(testData).isInstanceOfSatisfying(Scenario::class.java) { scenario ->
                assertThat(scenario.steps).hasOnlyOneElementSatisfying { step ->
                    assertThat(step.value).isEqualTo("I test the Gherkin parser again")
                }
            }
        }
    }

    @Test
    fun `can parse multiple steps in a scenario`() {
        val document = cut.parse(multipleStepScenarioGherkin())

        assertThat(document.elements).hasOnlyOneElementSatisfying { testData ->
            assertThat(testData.description).satisfies { description ->
                assertThat(description.name).isEqualTo("Test Scenario 1")
                assertThat(description.isManual).isFalse()
            }

            assertThat(testData).isInstanceOfSatisfying(Scenario::class.java) { scenario ->
                assertThat(scenario.steps).satisfies { steps ->
                    assertThat(steps).hasSize(4)
                    assertThat(steps[0]).satisfies { step ->
                        assertThat(step.value).isEqualTo("a working Gherkin parser")
                    }
                    assertThat(steps[1]).satisfies { step ->
                        assertThat(step.value).isEqualTo("some Gherkin text")
                    }
                    assertThat(steps[2]).satisfies { step ->
                        assertThat(step.value).isEqualTo("I test the Gherkin parser")
                    }
                    assertThat(steps[3]).satisfies { step ->
                        assertThat(step.value).isEqualTo("I get a valid Document containing the expected information")
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun generateRandomStrings(): List<String> {
            return (0..999).map {
                generateRandomString()
            }
        }

        private fun generateRandomString(): String {
            val length = Random.nextInt(3..10)

            var result: String
            do {
                result = Random.nextBytes(length).toString(Charsets.UTF_8)
            } while (result == "feature")

            return result
        }
    }
}