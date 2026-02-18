group = "ru.tecius.telemed"
version = "1.0.0"
description = "Search Processor"

val javapoetVersion: String by project
val autoServiceVersion: String by project

dependencies {
    implementation(project(":search-models"))
    implementation("com.squareup:javapoet:$javapoetVersion")
    compileOnly("com.google.auto.service:auto-service-annotations:$autoServiceVersion")
    annotationProcessor("com.google.auto.service:auto-service:$autoServiceVersion")
}

tasks.jar {
    archiveFileName.set("${project.parent?.name}-${project.name}-${version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.bootJar {
    enabled = false
}