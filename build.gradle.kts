plugins {
	`maven-publish`
}

group = "com.github.itsnotethann.uncommonskript"
version = "1.0.0-alpha.1"

repositories {
	mavenCentral()
}

subprojects {
	if (name == "testhost") return@subprojects

	apply(plugin = "java")
	apply(plugin = "maven-publish")

	group = rootProject.group

	publishing {
		publications {
			create<MavenPublication>("maven") {
				groupId = rootProject.group.toString()
				artifactId = project.name
				version = rootProject.version.toString()

				from(components["java"])
			}
		}
	}
}
