plugins {
	java
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(project(":common"))
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(25)
	options.encoding = "UTF-8"
}

val scripts = layout.projectDirectory.dir("scripts")
val work = layout.buildDirectory.dir("boottest")

val cleanBootTest by tasks.registering(Delete::class) {
	delete(work)
}

fun bootPhase(phase: Int, scriptsToRun: String, loaderThreads: Int, bootBeforeTicking: Boolean = false) = tasks.registering(JavaExec::class) {
	group = "verification"
	dependsOn(tasks.classes)
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("org.skriptlang.skript.testhost.TestHost")
	systemProperty("testhost.bootBeforeTicking", bootBeforeTicking.toString())
	args(scripts.asFile.absolutePath, work.get().asFile.absolutePath, phase.toString(), scriptsToRun, loaderThreads.toString())
	inputs.dir(scripts)
	outputs.upToDateWhen { false }
}

val bootTestPhase1 by bootPhase(1, "all", 0)
val bootTestPhase2 by bootPhase(2, "only", 0)
val bootTestPhase3 by bootPhase(3, "all", 2)
val bootTestPhase5 by bootPhase(5, "only", 2, true)

bootTestPhase1 {
	dependsOn(cleanBootTest)
}

bootTestPhase2 {
	dependsOn(bootTestPhase1)
}

bootTestPhase3 {
	dependsOn(bootTestPhase2)
}

bootTestPhase5 {
	dependsOn(bootTestPhase3)
}

val bootTest by tasks.registering {
	group = "verification"
	dependsOn(bootTestPhase5)
}

tasks.check {
	dependsOn(bootTest)
}
