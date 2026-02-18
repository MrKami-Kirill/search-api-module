group = "ru.tecius.telemed"
version = "1.0.0"
description = "Search Experimental"

dependencies {
    implementation(project(":search-models"))
    implementation(project(":search-service"))
    annotationProcessor(project(":search-processor"))
}

tasks.compileJava {
    dependsOn(tasks.processResources)
    options.compilerArgs.add("-Asearch.info.resources.dir=${project.projectDir}/src/main/resources")
}

tasks.jar {
    enabled = false
}

tasks.bootJar {
    archiveFileName.set("${project.parent?.name}-${project.name}-${version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}