plugins {
	`kotlin-project-config`
}

dependencies {
	implementation(project(":livingdoc-api"))
	implementation(project(":livingdoc-converters"))
	implementation(project(":livingdoc-extensions-api"))
	implementation(project(":livingdoc-results"))
	implementation(project(":livingdoc-testdata"))
	implementation(project(":livingdoc-reports"))

	api(project(":livingdoc-config"))
	api(project(":livingdoc-repositories"))

	testImplementation("org.assertj:assertj-core:${Versions.assertJ}")
}
