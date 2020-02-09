package org.livingdoc.reports.confluence

import com.atlassian.confluence.api.model.content.AttachmentUpload
import com.atlassian.confluence.api.model.content.id.ContentId
import com.atlassian.confluence.rest.client.RemoteAttachmentServiceImpl
import com.atlassian.confluence.rest.client.RestClientFactory
import com.atlassian.confluence.rest.client.authentication.AuthenticatedWebResourceProvider
import com.google.common.util.concurrent.MoreExecutors
import org.livingdoc.api.documents.ExecutableDocument
import org.livingdoc.config.YamlUtils
import org.livingdoc.reports.html.HtmlReportRenderer
import org.livingdoc.reports.spi.Format
import org.livingdoc.reports.spi.ReportRenderer
import org.livingdoc.results.documents.DocumentResult
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.ZonedDateTime

private const val MINOR_VERSION = false

@Format("confluence")
class ConfluenceReportRenderer : ReportRenderer {

    override fun render(documentResult: DocumentResult, config: Map<String, Any>) {
        val confluenceConfig = YamlUtils.toObject(config, ConfluenceReportConfig::class)
        val repositoryName = extractRepositoryName(documentResult)

        // Check for matching repository; only generate report on match
        if (repositoryName != confluenceConfig.repositoryName) {
            return
        }

        // Render html report
        val html = HtmlReportRenderer().render(documentResult)

        // Upload report to confluence

        val contentId = extractContentId(documentResult)

        uploadReport(html, contentId, confluenceConfig)
    }

    /**
     * Uploads a report to a conflucne page as an attachment
     *
     * @param report The report text to upload
     * @param contentId The [ContentId] of the page to attach the report to
     * @param confluenceConfig A [ConfluenceReportConfig] containing further settings for the upload
     */
    fun uploadReport(report: String, contentId: ContentId, confluenceConfig: ConfluenceReportConfig) {

        val authenticatedWebResourceProvider = AuthenticatedWebResourceProvider(
            RestClientFactory.newClient(),
            confluenceConfig.baseURL,
            confluenceConfig.path
        )
        authenticatedWebResourceProvider.setAuthContext(
            confluenceConfig.username, confluenceConfig.password.toCharArray()
        )

        val contentFile = Files.createTempFile(confluenceConfig.filename, null)
        Files.write(contentFile, report.toByteArray(StandardCharsets.UTF_8))

        val comment = if (confluenceConfig.comment.isNotEmpty()) {
            confluenceConfig.comment
        } else {
            "Report from " + ZonedDateTime.now().toString()
        }

        val attachment = RemoteAttachmentServiceImpl(
            authenticatedWebResourceProvider, MoreExecutors.newDirectExecutorService()
        )
        val atUp = AttachmentUpload(
            contentFile.toFile(), confluenceConfig.filename, "text/html",
            comment, MINOR_VERSION
        )

        // Look for already existing attachment
        val attachmentId = attachment
            .find()
            .withContainerId(contentId)
            .withFilename(confluenceConfig.filename)
            .fetchCompletionStage()
            .toCompletableFuture()
            .get()
            .orElseGet { null }
            ?.id

        if (attachmentId == null) {
            // Add new attachment
            attachment.addAttachmentsCompletionStage(contentId, listOf(atUp))
        } else {
            // Update existing attachment
            attachment.updateDataCompletionStage(attachmentId, atUp)
        }
    }

    internal fun extractContentId(documentResult: DocumentResult): ContentId {
        try {
            val testAnnotation = documentResult.documentClass
                .getAnnotation(ExecutableDocument::class.java).value
            // Extract the content id from the page link
            val numId = Regex("(?<=://)[0-9]+").find(testAnnotation)!!.groupValues[0].toLong()

            return ContentId.of(numId)
        } catch (e: Exception) {
            throw IllegalArgumentException("No content id could be extracted form the given document")
        }
    }

    internal fun extractRepositoryName(documentResult: DocumentResult): String {
        try {
            val testAnnotation = documentResult.documentClass
                .getAnnotation(ExecutableDocument::class.java).value

            return Regex("^.*(?=://)").find(testAnnotation)!!.groupValues[0]
        } catch (e: Exception) {
            throw IllegalArgumentException("No repository name could be extracted form the given document")
        }
    }
}
