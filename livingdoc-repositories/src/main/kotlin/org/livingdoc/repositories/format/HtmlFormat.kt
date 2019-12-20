package org.livingdoc.repositories.format

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.livingdoc.repositories.DocumentFormat
import org.livingdoc.repositories.ParseException
import org.livingdoc.repositories.model.TestData
import org.livingdoc.repositories.model.TestDataDescription
import org.livingdoc.repositories.model.decisiontable.DecisionTable
import org.livingdoc.repositories.model.decisiontable.Field
import org.livingdoc.repositories.model.decisiontable.Header
import org.livingdoc.repositories.model.decisiontable.Row
import org.livingdoc.repositories.model.scenario.Scenario
import org.livingdoc.repositories.model.scenario.Step
import java.io.InputStream
import java.nio.charset.Charset

class HtmlFormat : DocumentFormat {

    private val supportedFileExtensions = setOf("html", "htm")

    override fun canHandle(fileExtension: String): Boolean {
        return supportedFileExtensions.contains(fileExtension.toLowerCase())
    }

    override fun parse(stream: InputStream): HtmlDocument {
        val streamContent = stream.readBytes().toString(Charset.defaultCharset())
        val document = Jsoup.parse(streamContent)
        val elements = parseRecursive(document.body(), ParseContext())
        return HtmlDocument(elements, document)
    }

    /**
     * Parses a html document recursively
     * Extracts [Scenario]s and [DecisionTable]s in the order they occur
     *
     * @param root The root element of the currently processed DOM subtree
     * @param rootContext The [ParseContext] of the currently processed DOM subtree.
     * Holding i.e. the currently valid headline
     *
     * @return A list of all found [Scenario]s and [DecisionTable]s
     */
    private fun parseRecursive(root: Element, rootContext: ParseContext): List<TestData> {
        var context = rootContext

        return root.children().flatMap {
            when (it.tagName()) {
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    context = ParseContext(it.text())
                    emptyList()
                }
                "table" -> {
                    parseTable(it, context)
                }
                "ul", "ol" -> {
                    parseRecursive(it, context) +
                            parseList(it, context)
                }
                else -> parseRecursive(it, context)
            }
        }
    }

    /**
     * Parses a single HTML table and decides whether it is big enough to be a [DecisionTable]
     *
     * @param table A single html table
     * @param context The [ParseContext] of the processed table
     *
     * @return A list of parsed [DecisionTable]s
     */
    private fun parseTable(table: Element, context: ParseContext): List<DecisionTable> {
        fun tableHasAtLeastTwoRows(table: Element) = table.getElementsByTag("tr").size > 1

        return if (tableHasAtLeastTwoRows(table))
            listOf(parseTableToDecisionTable(table, context))
        else emptyList()
    }

    private fun parseTableToDecisionTable(table: Element, context: ParseContext): DecisionTable {
        val tableRows = table.getElementsByTag("tr")
        val headers = extractHeadersFromFirstRow(tableRows)
        val dataRows = parseDataRow(headers, tableRows)
        return DecisionTable(headers, dataRows, TestDataDescription(context.headline, context.isManual()))
    }

    private fun extractHeadersFromFirstRow(tableRows: Elements): List<Header> {
        val firstRowContainingHeaders = tableRows[0]
        val headers = firstRowContainingHeaders.children()
                .filter(::isHeaderOrDataCell)
                .map(Element::text)
                .map(::Header).toList()

        if (headers.size != headers.distinct().size) {
            throw ParseException("Headers must contains only unique values: $headers")
        }
        return headers
    }

    private fun parseDataRow(headers: List<Header>, tableRows: Elements): List<Row> {
        val dataRows = mutableListOf<Row>()
        tableRows.drop(1).forEachIndexed { rowIndex, row ->
            val dataCells = row.children().filter(::isHeaderOrDataCell)

            if (headers.size != dataCells.size) {
                throw ParseException(
                        "Header count must match the data cell count in data row ${rowIndex + 1}. " +
                                "Headers: ${headers.map(Header::name)}, DataCells: $dataCells"
                )
            }

            val rowData = headers.mapIndexed { headerIndex, headerName ->
                headerName to Field(dataCells[headerIndex].text())
            }.toMap()
            dataRows.add(Row(rowData))
        }
        return dataRows
    }

    private fun isHeaderOrDataCell(it: Element) = it.tagName() == "th" || it.tagName() == "td"

    /**
     * Parses a single HTML list and decides whether it is big enough to be a [Scenario]
     *
     * @param list A single HTML list element
     * @param context The [ParseContext] of the processed list
     *
     * @return A list of parsed [Scenario]s
     */
    private fun parseList(list: Element, context: ParseContext): List<Scenario> {
        fun listHasAtLeastTwoItems(htmlList: Element) = htmlList.getElementsByTag("li").size > 1

        return if (listHasAtLeastTwoItems(list))
            listOf(parseListIntoScenario(list, context))
        else emptyList()
    }

    private fun parseListIntoScenario(htmlList: Element, context: ParseContext): Scenario {
        verifyZeroNestedLists(htmlList)

        val listItemElements = htmlList.getElementsByTag("li")
        return Scenario(parseListItems(listItemElements), TestDataDescription(context.headline, context.isManual()))
    }

    private fun parseListItems(listItemElements: Elements): List<Step> {
        return listItemElements.map { Step(it.text()) }.toList()
    }

    /**
     * Checks whether the given HTML list contains nested lists
     *
     * @param htmlList A HTML list element
     */
    private fun verifyZeroNestedLists(htmlList: Element) {
        val innerHtml = htmlList.html()
        if (innerHtml.contains("<ul") || innerHtml.contains("<ol")) {
            throw ParseException("Nested lists within unordered or ordered lists are not supported: ${htmlList.html()}")
        }
    }
}