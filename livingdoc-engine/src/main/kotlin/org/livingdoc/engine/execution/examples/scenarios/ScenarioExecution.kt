package org.livingdoc.engine.execution.examples.scenarios

import org.livingdoc.api.disabled.Disabled
import org.livingdoc.api.fixtures.scenarios.Binding
import org.livingdoc.engine.execution.Result
import org.livingdoc.engine.execution.examples.executeWithBeforeAndAfter
import org.livingdoc.engine.execution.examples.scenarios.model.ScenarioResult
import org.livingdoc.engine.execution.examples.scenarios.model.StepResult
import org.livingdoc.engine.fixtures.FixtureMethodInvoker
import org.livingdoc.engine.fixtures.FixtureMethodInvoker.FixtureMethodInvocationException
import org.livingdoc.repositories.model.scenario.Scenario
import java.lang.reflect.Parameter

internal class ScenarioExecution(
    private val fixtureClass: Class<*>,
    scenario: Scenario,
    document: Any?
) {

    private val fixtureModel = ScenarioFixtureModel(fixtureClass)
    private val scenarioResult = ScenarioResult.from(scenario)
    private val methodInvoker = FixtureMethodInvoker(document)

    /**
     * Executes the configured [Scenario].
     *
     * Does not throw any kind of exception.
     * Exceptional state of the execution is packaged inside the [ScenarioResult] in
     * the form of different result objects.
     */
    fun execute(): ScenarioResult {
        if (fixtureClass.isAnnotationPresent(Disabled::class.java)) {
            markScenarioAsDisabled(fixtureClass.getAnnotation(Disabled::class.java).value)
            return scenarioResult
        }

        try {
            assertFixtureIsDefinedCorrectly()
            executeScenario()
            markScenarioAsSuccessfullyExecuted()
        } catch (e: Exception) {
            markScenarioAsExecutedWithException(e)
        } catch (e: AssertionError) {
            markScenarioAsExecutedWithException(e)
        }
        setSkippedStatusForAllUnknownResults()
        return scenarioResult
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
        var previousResult: Result = Result.Executed
        for (step in scenarioResult.steps) {
            if (previousResult == Result.Executed) {
                executeStep(fixture, step)
                previousResult = step.result
            } else {
                step.result = Result.Skipped
            }
        }
    }

    private fun executeStep(fixture: Any, step: StepResult) {
        val result = fixtureModel.getMatchingStepTemplate(step.value)
        val method = fixtureModel.getStepMethod(result.template)
        val parameterList = method.parameters
            .map { parameter ->
                result.variables.getOrElse(
                    getParameterName(parameter),
                    { error("Missing parameter value: ${getParameterName(parameter)}") })
            }
            .toTypedArray()
        step.result = invokeExpectingException {
            methodInvoker.invoke(method, fixture, parameterList)
        }
    }

    private fun getParameterName(parameter: Parameter): String {
        return parameter.getAnnotationsByType(Binding::class.java).firstOrNull()?.value
            ?: parameter.name
    }

    private fun invokeExpectingException(function: () -> Unit): Result {
        return try {
            function.invoke()
            Result.Executed
        } catch (e: AssertionError) {
            Result.Failed(e)
        } catch (e: Exception) {
            Result.Exception(e)
        }
    }

    private fun createFixtureInstance(): Any {
        return fixtureClass.newInstance()
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

    private fun markScenarioAsSuccessfullyExecuted() {
        scenarioResult.result = Result.Executed
    }

    private fun markScenarioAsDisabled(reason: String) {
        scenarioResult.result = Result.Disabled(reason)
    }

    private fun markScenarioAsExecutedWithException(e: Throwable) {
        scenarioResult.result = Result.Exception(e)
    }

    private fun setSkippedStatusForAllUnknownResults() {
        for (step in scenarioResult.steps) {
            if (step.result === Result.Unknown) {
                step.result = Result.Skipped
            }
        }
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
