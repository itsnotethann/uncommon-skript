plugins {
	java
	`maven-publish`
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":common"))
	compileOnly("net.kyori:adventure-api:5.2.0")
	compileOnly("org.jetbrains:annotations:26.0.2")
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
	options.encoding = "UTF-8"
}
