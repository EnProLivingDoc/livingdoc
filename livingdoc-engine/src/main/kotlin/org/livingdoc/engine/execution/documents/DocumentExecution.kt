package org.livingdoc.engine.execution.documents

import org.livingdoc.api.Before
import org.livingdoc.api.After
import org.livingdoc.engine.DecisionTableToFixtureMatcher
import org.livingdoc.engine.ScenarioToFixtureMatcher
import org.livingdoc.engine.execution.Status
import org.livingdoc.engine.fixtures.FixtureMethodInvoker
import org.livingdoc.repositories.Document
import org.livingdoc.repositories.model.decisiontable.DecisionTable
import org.livingdoc.repositories.model.scenario.Scenario

/**
 * A DocumentExecution represents a single execution of a [DocumentFixture].
 *
 * @see DocumentFixture
 */
internal class DocumentExecution(
    private val documentClass: Class<*>,
    private val document: Document,
    private val decisionTableToFixtureMatcher: DecisionTableToFixtureMatcher,
    private val scenarioToFixtureMatcher: ScenarioToFixtureMatcher
) {
    private val documentFixtureModel: DocumentFixtureModel = DocumentFixtureModel(documentClass)
    private val builder = DocumentResult.Builder().withDocumentClass(documentClass).withStatus(Status.Executed)
    private val methodInvoker: FixtureMethodInvoker = FixtureMethodInvoker(documentClass)
    private val fixture: Any = documentClass.getDeclaredConstructor().newInstance()

    /**
     * Execute performs the actual execution
     *
     * @return a [DocumentResult] describing the outcome of this DocumentExecution
     */
    fun execute(): DocumentResult {
        executeBeforeMethods()
        executeFixtures()
        executeAfterMethods()
        return builder.build()
    }

    /**
     * ExecuteBeforeMethods invokes all [Before] methods on the [DocumentFixture].
     *
     * @see Before
     * @see DocumentFixture
     */
    private fun executeBeforeMethods() {
        documentFixtureModel.beforeMethods.forEach { method -> methodInvoker.invoke(method, fixture) }
    }

    /**
     * ExecuteFixtures runs all examples contained in the document with their corresponding fixture.
     */
    private fun executeFixtures() {
        document.elements.mapNotNull { element ->
            when (element) {
                is DecisionTable -> {
                    decisionTableToFixtureMatcher
                            .findMatchingFixture(element, documentFixtureModel.decisionTableFixtures)
                            .execute(element)
                }
                is Scenario -> {
                    scenarioToFixtureMatcher
                            .findMatchingFixture(element, documentFixtureModel.scenarioFixtures)
                            .execute(element)
                }
                else -> null
            }
        }.forEach { result -> builder.withResult(result) }
    }

    /**
     * ExecuteAfterMethods invokes all [After] methods on the [DocumentFixture].
     *
     * @see After
     * @see DocumentFixture
     */
    private fun executeAfterMethods() {
        documentFixtureModel.afterMethods.forEach { method -> methodInvoker.invoke(method, fixture) }
    }
}
