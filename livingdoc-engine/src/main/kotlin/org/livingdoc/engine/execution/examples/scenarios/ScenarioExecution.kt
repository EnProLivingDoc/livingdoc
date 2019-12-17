package org.livingdoc.engine.execution.examples.scenarios

import org.livingdoc.api.disabled.Disabled
import org.livingdoc.api.exception.ExampleSyntax
import org.livingdoc.api.fixtures.scenarios.Binding
import org.livingdoc.engine.execution.Status
import org.livingdoc.engine.execution.examples.NoExpectedExceptionThrownException
import org.livingdoc.engine.execution.examples.executeWithBeforeAndAfter
import org.livingdoc.engine.execution.examples.scenarios.model.ScenarioResult
import org.livingdoc.engine.execution.examples.scenarios.model.StepResult
import org.livingdoc.engine.fixtures.FixtureMethodInvoker
import org.livingdoc.engine.fixtures.FixtureMethodInvoker.ExpectedException
import org.livingdoc.engine.fixtures.FixtureMethodInvoker.FixtureMethodInvocationException
import org.livingdoc.repositories.model.scenario.Scenario
import java.lang.reflect.Method
import java.lang.reflect.Parameter

internal class ScenarioExecution(
    private val fixtureClass: Class<*>,
    private val scenario: Scenario,
    document: Any?
) {

    private val fixtureModel = ScenarioFixtureModel(fixtureClass)
    private val scenarioResultBuilder = ScenarioResult.Builder().withScenario(scenario)
    private val methodInvoker = FixtureMethodInvoker(document)

    /**
     * Executes the configured [Scenario].
     *
     * Does not throw any kind of exception.
     * Exceptional state of the execution is packaged inside the [ScenarioResult] in
     * the form of different status objects.
     */
    fun execute(): ScenarioResult {
        if (fixtureClass.isAnnotationPresent(Disabled::class.java)) {
            return scenarioResultBuilder
                .withStatus(Status.Disabled(fixtureClass.getAnnotation(Disabled::class.java).value))
                .build()
        }

        try {
            assertFixtureIsDefinedCorrectly()
            executeScenario()
            scenarioResultBuilder.withStatus(Status.Executed)
        } catch (e: Exception) {
            scenarioResultBuilder.withStatus(Status.Exception(e))
                .withUnassignedSkipped()
        } catch (e: AssertionError) {
            scenarioResultBuilder.withStatus(Status.Exception(e))
                .withUnassignedSkipped()
        }
        return scenarioResultBuilder.build()
    }

    private fun assertFixtureIsDefinedCorrectly() {
        val errors = ScenarioFixtureChecker.check(fixtureModel)
        if (errors.isNotEmpty()) {
            throw MalformedScenarioFixtureException(fixtureClass, errors)
        }
    }

    private fun executeScenario() {
        val fixture = createFixtureInstance()
        executeWithBeforeAndAfter(
            before = { invokeBeforeMethods(fixture) },
            body = { executeSteps(fixture) },
            after = { invokeAfterMethods(fixture) }
        )
    }

    private fun executeSteps(fixture: Any) {
        var previousStatus: Status = Status.Executed
        for (step in scenario.steps) {
            val stepResultBuilder = StepResult.Builder().withValue(step.value)

            if (previousStatus == Status.Executed) {
                executeStep(fixture, step.value, stepResultBuilder)
            } else {
                stepResultBuilder.withStatus(Status.Skipped)
            }

            stepResultBuilder.build().also {
                scenarioResultBuilder.withStep(it)
                previousStatus = it.status
            }
        }
    }

    private fun executeStep(fixture: Any, stepValue: String, stepResultBuilder: StepResult.Builder) {
        val result = fixtureModel.getMatchingStepTemplate(stepValue)
        val method = fixtureModel.getStepMethod(result.template)
        val parameterList = method.parameters
            .map { parameter ->
                result.variables.getOrElse(
                    getParameterName(parameter),
                    { error("Missing parameter value: ${getParameterName(parameter)}") })
            }
            .toTypedArray()
        stepResultBuilder.withStatus(
            invokeExpectingException(method, fixture, parameterList)
        )
    }

    private fun getParameterName(parameter: Parameter): String {
        return parameter.getAnnotationsByType(Binding::class.java).firstOrNull()?.value
            ?: parameter.name
    }

    private fun invokeExpectingException(
        method: Method,
        fixture: Any,
        parameterList: Array<String>
    ): Status {
        return try {
            methodInvoker.invoke(method, fixture, parameterList)
            if (parameterList.contains(ExampleSyntax.EXCEPTION)) {
                return Status.Failed(NoExpectedExceptionThrownException())
            }
            Status.Executed
        } catch (e: AssertionError) {
            this.handleAssertionError(parameterList, e)
        } catch (e: ExpectedException) {
            Status.Executed
        } catch (e: Exception) {
            Status.Exception(e)
        }
    }

    /**
     * Creates a new instance of the fixture class passed to this execution
     */
    private fun createFixtureInstance(): Any {
        return fixtureClass.getDeclaredConstructor().newInstance()
    }

    private fun invokeBeforeMethods(fixture: Any) {
        fixtureModel.beforeMethods.forEach { methodInvoker.invoke(it, fixture) }
    }

    private fun invokeAfterMethods(fixture: Any) {
        val exceptions = mutableListOf<Throwable>()
        for (afterMethod in fixtureModel.afterMethods) {
            try {
                methodInvoker.invoke(afterMethod, fixture)
            } catch (e: AssertionError) {
                exceptions.add(e)
            } catch (e: FixtureMethodInvocationException) {
                exceptions.add(e.cause!!)
            }
        }
        if (exceptions.isNotEmpty()) throw AfterMethodExecutionException(exceptions)
    }

    private fun handleAssertionError(parameterList: Array<String>, e: AssertionError): Status {
        if (parameterList.contains(ExampleSyntax.EXCEPTION)) {
            return Status.Failed(NoExpectedExceptionThrownException())
        }
        return Status.Failed(e)
    }

    internal class MalformedScenarioFixtureException(fixtureClass: Class<*>, errors: List<String>) : RuntimeException(
        "The fixture class <$fixtureClass> is malformed: \n${errors.joinToString(
            separator = "\n",
            prefix = "  - "
        )}"
    )

    internal class AfterMethodExecutionException(exceptions: List<Throwable>) :
        RuntimeException("One or more exceptions were thrown during execution of @After methods") {

        init {
            exceptions.forEach { addSuppressed(it) }
        }
    }
}
