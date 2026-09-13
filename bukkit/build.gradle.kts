plugins {
	`java-library`
	`maven-publish`
	id("com.gradleup.shadow") version "9.3.0"
}

repositories {
	mavenCentral()
}

dependencies {
	api(project(":common"))
	implementation("org.ow2.asm:asm:9.10.1")
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

tasks.jar {
	archiveClassifier.set("plain")
}

tasks.shadowJar {
	archiveClassifier.set("")
	dependencies {
		include(dependency("org.ow2.asm:asm"))
	}
	relocate("org.objectweb.asm", "org.skriptlang.skript.platform.bukkit.asm")
}

tasks.assemble {
	dependsOn(tasks.shadowJar)
}
