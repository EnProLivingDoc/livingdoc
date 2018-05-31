package org.livingdoc.repositories

import org.livingdoc.repositories.config.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RepositoryManager {

    companion object {

        private val log: Logger = LoggerFactory.getLogger(RepositoryManager::class.java)

        fun from(config: Configuration): RepositoryManager {
            log.debug("creating new repository manager...")
            val repositoryManager = RepositoryManager()
            config.repositories.forEach { (name, factory, configData) ->

                val factory = factory ?: throw NoRepositoryFactoryException(name)

                val factoryClass = Class.forName(factory)
                if (DocumentRepositoryFactory::class.java.isAssignableFrom(factoryClass)) {
                    val factoryInstance = factoryClass.newInstance() as DocumentRepositoryFactory<*>
                    val repository = factoryInstance.createRepository(name, configData)
                    repositoryManager.registerRepository(name, repository)
                } else {
                    throw NotARepositoryFactoryException(name)
                }

            }
            log.debug("...repository manager successfully created!")
            return repositoryManager
        }

    }

    private val repositoryMap: MutableMap<String, DocumentRepository> = mutableMapOf()

    fun registerRepository(name: String, repository: DocumentRepository) {
        log.debug("registering document repository '{}' of type {}", name, repository.javaClass.canonicalName)
        if (repositoryMap.containsKey(name)) {
            log.error("duplicate repository definition: '{}'", name)
            throw RepositoryAlreadyRegisteredException(name, repository)
        }
        repositoryMap[name] = repository
    }

    fun getRepository(name: String): DocumentRepository? = repositoryMap[name]

    class RepositoryAlreadyRegisteredException(name: String, repository: DocumentRepository) :
        RuntimeException("Document repository with name '$name' already registered! Cant register instance of ${repository.javaClass.canonicalName}")

    class NoRepositoryFactoryException(name: String) :
        RuntimeException("Repository declaration '$name' does not specify a 'factory' property.")

    class NotARepositoryFactoryException(name: String) :
        RuntimeException("Repository declaration '$name' does not specify a 'factory' property with a class of type '${DocumentRepositoryFactory::class.java.simpleName}'.")

}
