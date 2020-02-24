package org.livingdoc.jvm.decisiontable

/**
 *  This exception is thrown whenever an exception was expected but not thrown
 */
internal class NoExpectedExceptionThrownException : AssertionError(
    "No exception thrown but exception was expected to be thrown by fixture"
)
