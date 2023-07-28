val data_version: String by extra

plugins {
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.serialization)
	alias(libs.plugins.android.library)
	alias(libs.plugins.jetbrains.kover)
	id("com.github.gmazzo.buildconfig")
}

group = "com.darkrockstudios.apps.hammer"
version = libs.versions.app.get()

repositories {
    mavenCentral()
}

kotlin {
    android()
    jvm("desktop") {
        compilations.all {
			kotlinOptions.jvmTarget = libs.versions.jvm.get()
        }
    }
    ios {

    }

    sourceSets {
        val commonMain by getting {
            dependencies {
				implementation(libs.serialization.core)
				implementation(libs.coroutines.core)
				implementation(libs.datetime)
				implementation(libs.serialization.json)
				//implementation("org.kotlincrypto.endians:endians:0.1.0")
				//api("io.getstream:stream-result:1.1.0")
				api(libs.uuid)
				api(libs.korlibs.krypto)
				//api("com.goncalossilva:murmurhash:0.4.0")
				api(libs.cryptohash)
				api(libs.korlibs.korio)
			}
        }
    }
}

android {
	namespace = "com.darkrockstudios.apps.hammer.base"
	compileSdk = libs.versions.android.sdk.compile.get().toInt()
    defaultConfig {
		minSdk = libs.versions.android.sdk.min.get().toInt()
		targetSdk = libs.versions.android.sdk.target.get().toInt()
	}
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

buildConfig {
    className("BuildMetadata")
    useKotlinOutput { internalVisibility = false }

	buildConfigField("String", "APP_VERSION", "\"${libs.versions.app.get()}\"")
	buildConfigField("String", "DATA_VERSION", "\"$data_version\"")
}

val GIT_TASK_NAME = "install-git-hooks"
tasks.register<Copy>(GIT_TASK_NAME) {
    from(layout.projectDirectory.file("../.gitHooks/pre-commit"))
    into(layout.projectDirectory.dir("../.git/hooks"))

    doLast {
        val file = layout.projectDirectory.file("../.git/hooks")
        file.asFile.setExecutable(true)
    }
}

afterEvaluate {
    val gitTask = tasks[GIT_TASK_NAME]
    for (task in tasks) {
        if (task != gitTask)
            task.dependsOn(gitTask)
    }
}