// Managed by mc-cli: `mod update` rewrites this file.
//
// What every subproject shares: Java 25, the mod's group and version, and
// JUnit for the tests of src/test, which fail when no test ran.

plugins {
    java
}

val mod = ModToml.read(rootDir.resolve("mod.toml"))

group = mod.pkg
if (version == Project.DEFAULT_VERSION) {
    version = "0.0.0"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    "testImplementation"(platform("org.junit:junit-bom:$JUNIT"))
    "testImplementation"("org.junit.jupiter:junit-jupiter")
    "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
}

junit(tasks.named<Test>("test"), "Write at least one test in ${project.name}/src/test/java.")
