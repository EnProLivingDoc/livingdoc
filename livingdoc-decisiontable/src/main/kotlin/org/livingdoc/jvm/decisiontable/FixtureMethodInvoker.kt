package org.livingdoc.jvm.decisiontable

import org.livingdoc.api.conversion.TypeConverter
import org.livingdoc.api.exception.ExampleSyntax
import org.livingdoc.converters.TypeConverters
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.isAccessible

class FixtureMethodInvoker(
    private val document: Any?
) {

    /**
     * Invoke the given static `method` with the given [String] `arguments`.
     *
     * Before invoking the method, the [String] arguments will be converted into objects of the appropriate types.
     * This is done by looking up a matching [TypeConverter] as follows:
     *
     * 1. `parameter`
     * 2. `method`
     * 3. `class`
     * 4. `document`
     * 5. `default`
     *
     * This method is capable of invoking any static method. If the invoked method is not public, it will be made so.
     *
     * @param method the [KCallable] to invoke
     * @param arguments the arguments for the invoked method as strings
     * @return the result of the invocation or `null` in case the invoked method has not return type (`void` / `Unit`)
     * @throws StaticFixtureMethodInvocationException in case anything went wrong with the invocation
     */
    fun invokeStatic(method: KCallable<*>, arguments: Array<String> = emptyArray()): Any? {
        try {
            return doInvokeStatic(method, arguments)
        } catch (e: Exception) {
            throw StaticFixtureMethodInvocationException(
                method,
                method.javaClass,
                e
            )
        }
    }

    private fun doInvokeStatic(method: KCallable<*>, arguments: Array<String>): Any? {
        val methodParameters = method.parameters
        assertThatAllArgumentsForMethodAreProvided(arguments, methodParameters)
        val convertedArguments = convert(arguments, methodParameters)
        return forceInvocation(method, convertedArguments)
    }

    /**
     * Invoke the given `method` on the given `fixture` instance with the given [String] `arguments`.
     *
     * Before invoking the method, the [String] arguments will be converted into objects of the appropriate types.
     * This is done by looking up a matching [TypeConverter] as follows:
     *
     * 1. `parameter`
     * 2. `method`
     * 3. `class`
     * 4. `document`
     * 5. `default`
     *
     * This method is capable of invoking any method on the given fixture instance. If the invoked method is not
     * public, it will be made so.
     *
     * @param method the [KCallable] to invoke
     * @param fixture the fixture instance to invoke the method on
     * @param arguments the arguments for the invoked method as strings
     * @return the result of the invocation or `null` in case the invoked method has not return type (`void` / `Unit`)
     * @throws FixtureMethodInvocationException in case anything went wrong with the invocation
     * @throws ExpectedException in case the thrown exception was expected
     */
    fun invoke(method: KCallable<*>, fixture: Any, arguments: Array<String> = emptyArray()): Any? {
        try {
            return doInvoke(method, fixture, arguments)
        } catch (e: AssertionError) {
            throw e
        } catch (e: Exception) {
            when {
                arguments.contains(ExampleSyntax.EXCEPTION) && e is java.lang.IllegalArgumentException -> {
                    throw ExpectedOutputIsNotNullableException(
                        method,
                        fixture
                    )
                }
                arguments.contains(ExampleSyntax.EXCEPTION) -> {
                    throw ExpectedException(
                        method,
                        fixture,
                        e
                    )
                }
                else -> {
                    throw FixtureMethodInvocationException(
                        method,
                        fixture,
                        e
                    )
                }
            }
        }
    }

    private fun doInvoke(method: KCallable<*>, fixture: Any, arguments: Array<String>): Any? {
        val methodParameters = method.parameters
        assertThatAllArgumentsForMethodAreProvided(arguments, methodParameters)
        val convertedArguments = convert(arguments, methodParameters)
        return forceInvocation(method, convertedArguments, fixture)
    }

    private fun assertThatAllArgumentsForMethodAreProvided(
        arguments: Array<String>,
        methodParameters: List<KParameter>
    ) {
        val numberOfArguments = arguments.size
        val numberOfMethodParameters = methodParameters.size
        if (numberOfArguments != numberOfMethodParameters) {
            throw MismatchedNumberOfArgumentsException(
                numberOfArguments,
                numberOfMethodParameters
            )
        }
    }

    private fun convert(
        arguments: Array<String>,
        methodParameters: List<KParameter>
    ): Array<Any?> { // TODO: Zip function?
        val convertedArguments = mutableListOf<Any?>()
        for (i in arguments.indices) {
            val argument = arguments[i]
            val methodParameter = methodParameters[i]
            val convertedArgument = convert(argument, methodParameter)
            convertedArguments.add(convertedArgument)
        }
        return convertedArguments.toTypedArray()
    }

    private fun convert(argument: String, methodParameter: KParameter): Any? {
        if (argument == ExampleSyntax.EXCEPTION || argument.isEmpty()) {
            return null
        }

        val documentClass = document?.javaClass
        val typeConverter = TypeConverters.findTypeConverter(methodParameter, documentClass)
            ?: throw NoTypeConverterFoundException(
                methodParameter
            )
        return typeConverter.convert(argument, methodParameter, documentClass)
    }

    @Suppress("SpreadOperator")
    private fun forceInvocation(method: KCallable<*>, arguments: Array<Any?>, instance: Any? = null): Any? {
        method.isAccessible = true
        try {
            return method.call(instance, *arguments)
        } catch (e: Exception) {
            throw e.cause ?: e
        }
    }

    class FixtureMethodInvocationException(method: KCallable<*>, fixture: Any, override val cause: Exception) :
        RuntimeException("Could not invoke method '$method' on fixture '$fixture' because of an exception:", cause)

    class StaticFixtureMethodInvocationException(method: KCallable<*>, fixtureClass: Class<*>, e: Exception) :
        RuntimeException(
            "Could not invoke method '$method' on fixture class '$fixtureClass' because of an exception:",
            e
        )

    class ExpectedException(method: KCallable<*>, fixture: Any, e: Exception) :
        RuntimeException("Indicate expected exception in method '$method' on fixture class '$fixture':", e)

    class ExpectedOutputIsNotNullableException(method: KCallable<*>, fixture: Any) :
        RuntimeException(
            "The expected output parameter in method '$method' on fixture class '$fixture' should be nullable when " +
                    "an exception is expected"
        )

    internal class MismatchedNumberOfArgumentsException(args: Int, params: Int) :
        RuntimeException("Method argument number mismatch: arguments = $args, method parameters = $params")

    internal class NoTypeConverterFoundException(parameter: KParameter) :
        RuntimeException("No type converter could be found to convert method parameter: $parameter")
}
