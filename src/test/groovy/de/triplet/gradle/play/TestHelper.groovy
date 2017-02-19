package de.triplet.gradle.play

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

public class TestHelper {

    public static final File FIXTURE_WORKING_DIR = new File("src/test/fixtures/android_app")

    public static Project evaluatableProject() {
        Project project = ProjectBuilder.builder().withProjectDir(FIXTURE_WORKING_DIR).build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.github.triplet.play'
        project.android {
            compileSdkVersion 25
            buildToolsVersion '25.0.2'

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 25
                targetSdkVersion 25
            }

            buildTypes {
                release {
                    signingConfig signingConfigs.debug
                }
            }
        }

        return project
    }

    public static Project noSigningConfigProject() {
        Project project = ProjectBuilder.builder().withProjectDir(FIXTURE_WORKING_DIR).build()
        project.apply plugin: 'com.android.application'
        project.apply plugin: 'com.github.triplet.play'
        project.android {
            compileSdkVersion 25
            buildToolsVersion '25.0.2'

            defaultConfig {
                versionCode 1
                versionName '1.0'
                minSdkVersion 25
                targetSdkVersion 25
            }

            buildTypes {
                release {
                }
            }
        }

        return project
    }
}
