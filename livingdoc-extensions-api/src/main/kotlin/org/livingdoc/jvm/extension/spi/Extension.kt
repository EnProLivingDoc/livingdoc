package org.livingdoc.jvm.extension.spi

import org.livingdoc.jvm.extension.DocumentFixtureContext
import org.livingdoc.jvm.extension.FixtureContext
import org.livingdoc.jvm.extension.GroupContext

/**
 * The Extensions interface used to extend the lifecycle of the Livingdoc tests. The Extension can listen to different
 * lifecycle events of a test execution in livingdoc. For each lifecycle hook the context of the execution is passed to
 * the Extension. The Context contains reference to the internal representation of the test case and the a reference to
 * the class refection object.
 */
interface Extension {

    fun onBeforeGroup(context: GroupContext) {
        // optional
    }

    fun onBeforeDocument(context: DocumentFixtureContext) {
        // optional
    }

    fun onBeforeFixture(context: FixtureContext<*>) {
        // optional
    }

    fun onAfterFixture(context: FixtureContext<*>) {
        // optional
    }

    fun onAfterDocument(context: DocumentFixtureContext) {
        // optional
    }

    fun onAfterGroup(context: GroupContext) {
        // optional
    }


}
