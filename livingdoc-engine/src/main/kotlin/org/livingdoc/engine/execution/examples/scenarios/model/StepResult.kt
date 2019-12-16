package org.livingdoc.engine.execution.examples.scenarios.model

import org.livingdoc.engine.execution.Status
import org.livingdoc.engine.execution.examples.decisiontables.model.RowResult
import java.lang.reflect.Method
import java.util.*

data class StepResult private constructor(
    val value: String,
    val status: Status,
    val fixtureMethod: Optional<Method>
) {
    class Builder {
        private var status: Status = Status.Unknown
        private var value: String = ""
        private var fixtureMethod: Method? = null

        fun withStatus(status: Status): Builder {
            this.status = status
            return this
        }

        fun withValue(value: String): Builder {
            this.value = value
            return this
        }

        fun withFixtureMethod(fixtureMethod: Method): Builder {
            this.fixtureMethod = fixtureMethod
            return this
        }

        /**
         * Build an immutable [StepResult]
         *
         * @returns A new [StepResult] with the data from this builder
         * @throws IllegalStateException If the builder is missing data to build a [StepResult]
         */
        fun build(): StepResult {
            // Check status
            if (this.status == Status.Unknown) {
                throw IllegalStateException("Cannot build StepResult with unknown status")
            }

            return StepResult(this.value, this.status, Optional.ofNullable(this.fixtureMethod))
        }
    }
}
