@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "Flowtune"
include(":app")
include(":innertube")
include(":packages:kugou")
include(":packages:lrclib")
include(":packages:betterlyrics")
include(":packages:simpmusic")

project(":packages:betterlyrics").projectDir = file("packages/betterlyrics")
project(":packages:kugou").projectDir = file("packages/kugou")
project(":packages:lrclib").projectDir = file("packages/lrclib")
project(":packages:simpmusic").projectDir = file("packages/simpmusic")