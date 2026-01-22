apply(plugin = "maven-publish")

val groupIdVal = "com.nikol.direct"
val versionVal = "0.1.0"

afterEvaluate {
    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                groupId = groupIdVal
                artifactId = project.name
                version = versionVal

                if (plugins.hasPlugin("com.android.library")) {
                    from(components["release"])
                } else {
                    from(components["java"])
                }
            }
        }
    }
}