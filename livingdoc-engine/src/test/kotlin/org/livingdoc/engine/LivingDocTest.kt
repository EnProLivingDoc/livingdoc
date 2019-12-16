package org.livingdoc.engine

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.livingdoc.config.ConfigProvider
import org.livingdoc.engine.execution.Status
import org.livingdoc.engine.resources.DisabledExecutableDocument
import org.livingdoc.repositories.RepositoryManager
import java.io.File

internal class LivingDocTest {

    @Test
    fun disabledExecutableDocumentExecute() {
        val repoManagerMock = mockkJClass(RepositoryManager::class.java)
        val configProviderMock = mockkJClass(ConfigProvider::class.java)
        val cut = LivingDoc(configProviderMock, repoManagerMock)
        val documentClass = DisabledExecutableDocument::class.java

        val result = cut.execute(documentClass)

        assertThat(result.documentStatus).isInstanceOf(Status.Disabled::class.java)
        assertThat((result.documentStatus as Status.Disabled).reason).isEqualTo("Skip this test document")
    }
}
