package org.livingdoc.engine

import org.livingdoc.engine.execution.examples.NoFixtureWrapper
import org.livingdoc.engine.execution.examples.decisiontables.DecisionTableFixtureModel
import org.livingdoc.engine.execution.examples.decisiontables.DecisionTableFixtureWrapper
import org.livingdoc.engine.fixtures.FixtureWrapper
import org.livingdoc.repositories.model.decisiontable.DecisionTable

/**
 * Default matcher to find the right fixture classes for a given list of tables.
 */
class DecisionTableToFixtureMatcher {

    fun findMatchingFixture(decisionTable: DecisionTable, fixtures: List<DecisionTableFixtureWrapper>): FixtureWrapper {
        if (decisionTable.description.isManual) {
            return NoFixtureWrapper()
        }

        val headerNames = decisionTable.headers.map { it.name }
        val numberOfHeaders = headerNames.size

        val matchingFixtures = fixtures.filter { fixture ->
            val fixtureModel = DecisionTableFixtureModel(fixture.fixtureClass)
            val aliases = fixtureModel.aliases
            val numberOfAliases = aliases.size
            val numberOfMatchedHeaders = headerNames.filter { aliases.contains(it) }.size

            numberOfMatchedHeaders == numberOfHeaders && numberOfMatchedHeaders == numberOfAliases
        }

        if (matchingFixtures.size > 1) {
            throw MultipleMatchingFixturesException(headerNames, matchingFixtures)
        }
        return matchingFixtures.firstOrNull() ?: throw NoMatchingFixturesException(headerNames, fixtures)
    }

    class MultipleMatchingFixturesException(
        headerNames: List<String>,
        matchingFixtures: List<DecisionTableFixtureWrapper>
    ) : RuntimeException("Could not identify a unique fixture matching the Decision Table's headers " +
            "${headerNames.map { "'$it'" }}. Matching fixtures found: $matchingFixtures"
    )

    class NoMatchingFixturesException(headerNames: List<String>, fixtures: List<DecisionTableFixtureWrapper>) :
        RuntimeException("Could not find any fixture matching the Decision Table's headers " +
                "${headerNames.map { "'$it'" }}. Available fixtures: $fixtures"
        )
}
