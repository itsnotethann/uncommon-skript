import java.io.File
import java.net.URI
import java.security.MessageDigest
import java.util.HexFormat

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

val benchScripts = layout.projectDirectory.dir("bench-scripts")
val benchWork = layout.buildDirectory.dir("bench")

val bench by tasks.registering(JavaExec::class) {
	group = "verification"
	dependsOn(tasks.classes)
	classpath = sourceSets.main.get().runtimeClasspath
	mainClass.set("org.skriptlang.skript.testhost.bench.BenchHost")
	for (property in listOf("bench.warmup", "bench.rounds", "bench.fires", "bench.baseline", "bench.only")) {
		providers.gradleProperty(property).orNull?.let { systemProperty(property, it) }
	}
	providers.gradleProperty("bench.jfr").orNull?.let {
		jvmArgs("-XX:StartFlightRecording=settings=profile,filename=$it,dumponexit=true")
	}
	args(benchScripts.asFile.absolutePath, benchWork.get().asFile.absolutePath)
	inputs.dir(benchScripts)
	outputs.upToDateWhen { false }
}

val addonHost by configurations.creating

dependencies {
	addonHost(project(":bukkit"))
}

val addonScripts = layout.projectDirectory.dir("addon-scripts")
val addonWork = layout.buildDirectory.dir("addontest")
val addonJarsDir = layout.buildDirectory.dir("addon-jars")
val addonJarsOverride = providers.gradleProperty("addonJars")
val addonJars = addonJarsOverride.orElse(addonJarsDir.map { it.asFile.absolutePath })
val bukkitShadowJar = project(":bukkit").tasks.named<Jar>("shadowJar")

fun sha256(file: File): String =
	HexFormat.of().formatHex(
		MessageDigest.getInstance("SHA-256").digest(file.readBytes()))

val addonJarSources = mapOf(
	"skript-reflect-2.6.3.jar" to listOf(
		"https://github.com/SkriptLang/skript-reflect/releases/download/v2.6.3/skript-reflect-2.6.3.jar",
		"0b422b828e17aac9e8417b6876924c1dcc221f6a065fb0a21b00da07fc298877"),
	"oopsk-1.0-beta2.jar" to listOf(
		"https://github.com/sovdeeth/oopsk/releases/download/1.0-beta2/oopsk-1.0-beta2.jar",
		"1951d99a826a425f69414ef51237ae2e17d20bdace16553ac859d9202dcdf62f"))

val fetchAddonJars by tasks.registering {
	val target = addonJarsDir
	val sources = addonJarSources
	val skip = addonJarsOverride.isPresent
	onlyIf { !skip }
	doLast {
		val dir = target.get().asFile
		dir.mkdirs()
		for ((name, source) in sources) {
			val url = source[0]
			val want = source[1]
			val jar = File(dir, name)
			if (jar.isFile && sha256(jar) == want)
				continue
			logger.lifecycle("fetching $name")
			URI(url).toURL().openStream().use { input ->
				jar.outputStream().use { output -> input.copyTo(output) }
			}
			val got = sha256(jar)
			if (got != want)
				throw GradleException("$name sha256 mismatch: expected $want, got $got")
		}
	}
}

val cleanAddonTest by tasks.registering(Delete::class) {
	delete(addonWork)
}

val addonTest by tasks.registering(JavaExec::class) {
	group = "verification"
	dependsOn(tasks.classes, cleanAddonTest, fetchAddonJars, bukkitShadowJar)
	classpath = files(bukkitShadowJar.flatMap { it.archiveFile }) + sourceSets.main.get().runtimeClasspath + addonHost
	mainClass.set("org.skriptlang.skript.testhost.TestHost")
	argumentProviders.add(CommandLineArgumentProvider {
		listOf(addonScripts.asFile.absolutePath, addonWork.get().asFile.absolutePath, "4", "all", "0", addonJars.get())
	})
	inputs.dir(addonScripts)
	outputs.upToDateWhen { false }
}
