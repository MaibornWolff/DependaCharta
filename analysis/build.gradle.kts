plugins {
    application
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    jacoco
    id("org.cyclonedx.bom") version "2.4.1"
    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

application {
    mainClass.set("de.maibornwolff.dependacharta.pipeline.MainKt")
}
group = "de.maibornwolff"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.bonede:tree-sitter:0.25.3")
    implementation("io.github.bonede:tree-sitter-java:0.23.4")
    implementation("io.github.bonede:tree-sitter-c-sharp:0.23.1")
    implementation("io.github.bonede:tree-sitter-cpp:0.23.4")
    implementation("io.github.bonede:tree-sitter-typescript:0.23.2")
    implementation("io.github.bonede:tree-sitter-php:0.23.11")
    implementation("io.github.bonede:tree-sitter-go:0.23.3")
    implementation("io.github.bonede:tree-sitter-python:0.23.4")
    implementation("com.github.ajalt.clikt:clikt:5.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("me.tongfei:progressbar:0.10.1")
    testImplementation(kotlin("test"))
    testImplementation("org.assertj:assertj-core:3.27.6")
    testImplementation(platform("org.junit:junit-bom:5.14.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.2")

    // Falls du Tests schreibst:
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.cyclonedxBom {
    setIncludeConfigs(listOf("runtimeClasspath", "compileClasspath"))
    setSkipConfigs(listOf("testCompileClasspath"))
    setProjectType("application")
}

tasks {
    val fatJar =
        register<Jar>("fatJar") {
            dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
            // set jar name to dependacharta.jar
            archiveFileName.set("dependacharta.jar")
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
            val sourcesMain = sourceSets.main.get()
            val contents =
                configurations.runtimeClasspath
                    .get()
                    .map { if (it.isDirectory) it else zipTree(it) } +
                    sourcesMain.output
            from(contents)
        }

    val generateVersionJson =
        register("generateVersionJson") {
            group = "build"
            description = "Generates a version.json file with the version specified"
            doLast {
                val versionProp = project.findProperty("version")
                if (versionProp == "unspecified") {
                    return@doLast
                }
                val resourcesDir =
                    project.layout.buildDirectory
                        .dir("resources/main/pipeline")
                        .get()
                        .asFile
                val versionJsonFile = File(resourcesDir, "version.json")
                if (!resourcesDir.exists()) {
                    resourcesDir.mkdirs()
                }
                versionJsonFile.writeText("""{ "version": "$versionProp" }""")
            }
        }

    processResources {
        finalizedBy(generateVersionJson)
    }

    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
        dependsOn("cyclonedxBom") // Generates the SBOM
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
    dependsOn(tasks.test) // tests are required to run before generating the report
}

kotlin {
    jvmToolchain(17)
}
