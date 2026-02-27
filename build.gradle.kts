import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.11.0"
}

group = "com.github.k8spodenv"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaUltimate("2025.1.3")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.16")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

intellijPlatform {
    pluginConfiguration {
        id = "com.github.k8spodenv"
        name = "K8s Pod Env Injector"
        version = project.version.toString()
        description = "Injects Kubernetes Pod environment variables into Spring Boot Run Configurations"
        vendor {
            name = "k8spodenv"
        }
        ideaVersion {
            sinceBuild = "251"
            untilBuild = "253.*"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
