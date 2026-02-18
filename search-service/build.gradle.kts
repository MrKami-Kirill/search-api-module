group = "ru.tecius.telemed"
version = "1.0.0"
description = "Search Service"

tasks.jar {
    archiveFileName.set("${project.parent?.name}-${project.name}-${version}.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.bootJar {
    enabled = false
}