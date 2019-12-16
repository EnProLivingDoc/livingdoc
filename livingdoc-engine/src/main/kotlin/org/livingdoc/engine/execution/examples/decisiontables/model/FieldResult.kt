package org.livingdoc.engine.execution.examples.decisiontables.model

import org.livingdoc.engine.execution.Status

data class FieldResult private constructor(
    val value: String,
    val status: Status
) {
    class Builder {
        private var value: String? = null
        private var status: Status = Status.Unknown

        fun withValue(value: String): Builder {
            this.value = value
            return this
        }

        fun withStatus(status: Status): Builder {
            this.status = status
            return this
        }

        fun build(): FieldResult {
            if (this.status == Status.Unknown)
                throw IllegalStateException("Cannot build FieldResult with unknown status")

            val value = this.value ?: throw IllegalArgumentException("Cannot build FieldResult without a value")

            return FieldResult(value, this.status)
        }

    }
}
