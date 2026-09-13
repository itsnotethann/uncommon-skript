plugins {
	`java-library`
	`maven-publish`
}

repositories {
	mavenCentral()
}

dependencies {
	api(project(":common"))
	api("org.ow2.asm:asm:9.8")
	api("com.google.guava:guava:33.5.0-jre")
	api("org.yaml:snakeyaml:1.32")
	api("net.kyori:adventure-text-minimessage:4.26.1")
	api("net.kyori:adventure-text-serializer-ansi:4.26.1")
	api("ch.qos.logback:logback-classic:1.5.32")
	compileOnly("org.jetbrains:annotations:26.0.2")
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
	options.encoding = "UTF-8"
}
