import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

// Configure project's dependencies
repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
    maven {
        url = uri("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    }

    intellijPlatform {
        defaultRepositories()
    }

    flatDir {
        dirs("lib")
    }
}

dependencies {
    testImplementation(libs.junit)

    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        instrumentationTools()
        pluginVerifier()
        testFramework(TestFrameworkType.Platform)
    }

    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation(files("lib/satd_detector.jar"))

    // If com.intellij.ml.llm.template is a local JAR, use this instead:
    // implementation(files("lib/com.intellij.ml.llm.template.jar"))
}

intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"
            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                                .withHeader(false)
                                .withEmptySections(false),
                        Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    runIde {
        val sandBoxPath = project.buildDir.resolve("idea-sandbox/" + providers.gradleProperty("platformType").get() + "-" + providers.gradleProperty("platformVersion").get() + "/plugins/${project.name}")
        val targetPath = sandBoxPath.resolve("SATDBailiff/target")
        val libPath = sandBoxPath.resolve("SATDBailiff")
        val sqlPath = sandBoxPath.resolve("sql")

        doFirst {
            if (!(libPath.exists())) {
                copy {
                    from(file("SATDBailiff"))
                    into(libPath)
                }
            }
            if (!(targetPath.exists())) {
                copy {
                    from(file("SATDBailiff/target"))
                    into(targetPath)
                }
            }
            if (!(sqlPath.exists())) {
                copy {
                    from(file("sql"))
                    into(sqlPath)
                }
            }
        }
    }

    jar {
        exclude("SATDBailiff/**")
        exclude("sql/**")
    }

    buildPlugin {
        from("SATDBailiff") {
            into("SATDBailiff")
        }
        from("sql") {
            into("sql")
        }
        from("SATDBailiff/target") {
            into("SATDBailiff/target")
        }
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                            "-Drobot-server.port=8082",
                            "-Dide.mac.message.dialogs.as.sheets=false",
                            "-Djb.privacy.policy.text=<!--999.999-->",
                            "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }
            plugins {
                robotServerPlugin()
            }
        }
    }
}
