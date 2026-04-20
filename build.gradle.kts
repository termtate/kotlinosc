plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    `java-library`
}

group = "io.github.termtate.kotlinosc"
version = "0.1.0"

repositories {
    mavenCentral()
}

val exampleMainClasses = mapOf(
    "message-codec" to "examples.messagecodec.MainKt",
    "bundle-dsl" to "examples.bundledsl.MainKt",
    "udp-client-send" to "examples.udpclientsend.MainKt",
    "udp-server-route" to "examples.udpserverroute.MainKt",
    "client-server-roundtrip" to "examples.clientserverroundtrip.MainKt",
    "address-pattern-routing" to "examples.addresspatternrouting.MainKt",
    "scheduled-bundle-dispatch" to "examples.scheduledbundledispatch.MainKt",
)

val examplesSourceSet = sourceSets.create("examples") {
    java.srcDir("examples")
    compileClasspath += sourceSets["main"].output
    runtimeClasspath += output + compileClasspath
}

configurations.named(examplesSourceSet.implementationConfigurationName) {
    extendsFrom(configurations["implementation"])
}

configurations.named(examplesSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations["runtimeOnly"])
}

dependencies {
    implementation(libs.kotlin.logging)
    api(libs.kotlin.coroutines)
    implementation(libs.slf4j.api)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
    add(examplesSourceSet.implementationConfigurationName, sourceSets["main"].output)
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

tasks.test {
    useJUnitPlatform {
        excludeTags("interop")
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileExamplesKotlin") {
    dependsOn(tasks.named("compileKotlin"))
    source("examples")
}

tasks.register<JavaExec>("runExample") {
    group = "application"
    description = "Runs an example. Use -Pexample=<${exampleMainClasses.keys.joinToString("|")}>."
    dependsOn(tasks.named(examplesSourceSet.classesTaskName))
    classpath = examplesSourceSet.runtimeClasspath

    doFirst {
        val exampleName = providers.gradleProperty("example").orNull
            ?: error("Missing -Pexample. Available examples: ${exampleMainClasses.keys.joinToString(", ")}")
        val exampleMain = exampleMainClasses[exampleName]
            ?: error("Unknown example '$exampleName'. Available examples: ${exampleMainClasses.keys.joinToString(", ")}")
        mainClass.set(exampleMain)
    }
}

val interopTest by tasks.registering(org.gradle.api.tasks.testing.Test::class) {
    description = "Runs interop tests (tagged with @Tag(\"interop\"))."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("interop")
    }
    shouldRunAfter(tasks.test)
}

tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml") {
    outputDirectory.set(layout.buildDirectory.dir("dokka/html").get().asFile)
}

tasks.named<org.jetbrains.dokka.gradle.DokkaTask>("dokkaGfm") {
    outputDirectory.set(layout.projectDirectory.dir("docs/api").asFile)
}

mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("release").isPresent) {
        signAllPublications()
    }

    coordinates(
        groupId = project.group.toString(),
        artifactId = rootProject.name,
        version = project.version.toString()
    )

    pom {
        name.set(rootProject.name)
        description.set("Kotlin/JVM OSC (Open Sound Control) library with codec, routing, transport, and DSL support.")
        url.set("https://github.com/termtate/kotlinosc")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("termtate")
                name.set("termtate")
                url.set("https://github.com/termtate")
            }
        }

        scm {
            url.set("https://github.com/termtate/kotlinosc")
            connection.set("scm:git:git://github.com/termtate/kotlinosc.git")
            developerConnection.set("scm:git:ssh://git@github.com/termtate/kotlinosc.git")
        }
    }
}
