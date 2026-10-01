// codex start
plugins {
    id("net.fabricmc.fabric-loom") version "1.17.21"
}
version = "1.0.0"
group = "mod.killaura"
base { archivesName = "killaura-standalone" }
repositories { mavenCentral() }
dependencies {
    minecraft("com.mojang:minecraft:26.3")
    implementation("net.fabricmc:fabric-loader:0.19.5")
    implementation("net.fabricmc.fabric-api:fabric-api:0.160.5+26.3")
}
java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    withSourcesJar()
}
loom {
    runs {
        named("client") { runDir("run-standaloneTodoAi") }
    }
}
tasks.processResources {
    filesMatching("fabric.mod.json") { expand("version" to project.version) }
    filesMatching("**/en_usTodoAi.json") { name = "en_us.json" }
}
tasks.jar { from("LICENSE") }
val verification = sourceSets.create("verification") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}
tasks.register<JavaExec>("verifyCombatTodoAi") {
    dependsOn(verification.classesTaskName)
    classpath = verification.runtimeClasspath
    mainClass = "mod.killaura.CombatMathTestTodoAi"
}
tasks.check { dependsOn("verifyCombatTodoAi") }
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "killaura-standalone-gametest"
        enableGameTests = false
        eula = true
    }
}
//codex end
