package org.livingdoc.reports.spi

import org.livingdoc.engine.execution.DocumentResult

/**
 * The Service Provider Interface that must be implemented by all report renderers. Every [ReportRenderer] must also be
 * annotated with [Format] to specify the unique format of report which is generated by the [ReportRenderer].
 */
interface ReportRenderer {
    /**
     * Renders the report with the specified [Format], [config] and [documentResult]. This function must also store the
     * generated reports in the report specific location which can be configured with the [config] parameter.
     */
    fun render(documentResult: DocumentResult, config: Map<String, Any>)
}
