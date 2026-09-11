plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "uncommon-skript"
include("spi")
include("compat")
include("common")
include("domain")
