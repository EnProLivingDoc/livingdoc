package org.livingdoc.repositories.rest

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.features.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import kotlinx.coroutines.runBlocking
import org.livingdoc.repositories.Document
import org.livingdoc.repositories.DocumentRepository
import org.livingdoc.repositories.format.DocumentFormatManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream

/**
 * This implementation of a DocumentRepository uses a remote HTTP server to get the Docuemnts from.
 * The remote host can be configured in the livingdoc.yml config section of the repository. Use baseURL to set the base
 * url of the remote host that hosts the Documents. As default the server on port 80 on the localhost is used as REST
 * Repository.
 */
class RESTRepository(
    private val name: String,
    private val config: RESTRepositoryConfig,
    private val client: HttpClient = HttpClient()
) : DocumentRepository {

    private val log: Logger = LoggerFactory.getLogger(RESTRepository::class.java)

    override fun getDocument(documentIdentifier: String): Document {
        val request =
            runBlocking {
                try {
                    log.debug("Get Document from url {}", config.baseURL + documentIdentifier)
                    client.get<HttpResponse>(config.baseURL + documentIdentifier).receive<InputStream>()
                } catch (e: IOException) {
                    throw RESTDocumentNotFoundException(e, documentIdentifier, config.baseURL)
                } catch (e: ClientRequestException) {
                    throw RESTDocumentNotFoundException(e, documentIdentifier, config.baseURL)
                }
            }

        return DocumentFormatManager.getFormat("html").parse(request)
    }
}