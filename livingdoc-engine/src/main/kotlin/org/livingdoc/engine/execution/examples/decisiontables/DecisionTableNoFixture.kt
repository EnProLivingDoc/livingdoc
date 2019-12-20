package org.livingdoc.engine.execution.examples.decisiontables

import org.livingdoc.engine.execution.Status
import org.livingdoc.engine.execution.examples.decisiontables.model.DecisionTableResult
import org.livingdoc.engine.execution.examples.decisiontables.model.FieldResult
import org.livingdoc.engine.execution.examples.decisiontables.model.RowResult
import org.livingdoc.engine.fixtures.Fixture
import org.livingdoc.repositories.model.decisiontable.DecisionTable

internal class DecisionTableNoFixture : Fixture<DecisionTable> {
    /**
     * Executes the configured [DecisionTable] without a [DecisionTableFixtureModel].
     *
     * Does not throw any kind of exception.
     * Exceptional state of the execution is packaged inside the [DecisionTableResult] in
     * the form of different status objects.
     */
    override fun execute(testData: DecisionTable): DecisionTableResult {
        val result = DecisionTableResult.Builder().withDecisionTable(testData).withFixture(this)
            .withFixtureSource(this.javaClass)

        if (testData.description.isManual) {
            result.withStatus(Status.Manual)

            testData.rows.forEach { row ->
                val rowResult = RowResult.Builder()
                    .withRow(row)
                    .withStatus(Status.Manual)

                testData.headers.forEach {
                    rowResult.withFieldResult(
                        it,
                        FieldResult.Builder()
                            .withValue(it.name)
                            .withStatus(Status.Manual)
                            .build()
                    )
                }

                result.withRow(rowResult.build())
            }
        }

        return result.build()
    }
}