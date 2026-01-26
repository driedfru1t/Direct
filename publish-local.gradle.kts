apply(plugin = "maven-publish")

val groupIdVal = "direct"
val versionVal = "0.2.0-alpha"

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