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
	implementation("org.bstats:bstats-bukkit:3.2.1")
	implementation("net.java.dev.jna:jna:5.17.0")
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
		include(dependency("org.bstats:bstats-base"))
		include(dependency("org.bstats:bstats-bukkit"))
		include(dependency("net.java.dev.jna:jna"))
	}
	relocate("org.objectweb.asm", "org.skriptlang.skript.platform.bukkit.asm")
	relocate("org.bstats", "ch.njol.skript.bstats")
}

tasks.assemble {
	dependsOn(tasks.shadowJar)
}
