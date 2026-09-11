plugins {
	`java-library`
	`maven-publish`
}

repositories {
	mavenCentral()
}

dependencies {
	api("org.slf4j:slf4j-api:2.0.16")
	compileOnly("org.jetbrains:annotations:26.0.2")
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
	options.encoding = "UTF-8"
}
