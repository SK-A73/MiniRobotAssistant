try {
    val processEnv = Class.forName("java.lang.ProcessEnvironment")
    for (fieldName in listOf("theUnmodifiableEnvironment", "theEnvironment", "theCaseInsensitiveEnvironment")) {
        runCatching {
            val field = processEnv.getDeclaredField(fieldName)
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            (field.get(null) as? MutableMap<String, String>)?.remove("ANDROID_PREFS_ROOT")
        }
    }
} catch (_: Throwable) {
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MiniRobotAssistant"
include(":app")
