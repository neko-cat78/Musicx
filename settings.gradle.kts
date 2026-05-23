@file:Suppress("UnstableApiUsage")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        maven { setUrl("https:
        maven { setUrl("https:
    }
}

rootProject.name = "Flowtune"
include(":app")
include(":innertube")
include(":packages:kugou")
include(":packages:lrclib")
include(":packages:kizzy")
include(":packages:lastfm")
include(":packages:betterlyrics")
include(":packages:simpmusic")

project(":packages:betterlyrics").projectDir = file("packages/betterlyrics")
project(":packages:kizzy").projectDir = file("packages/kizzy")
project(":packages:kugou").projectDir = file("packages/kugou")
project(":packages:lastfm").projectDir = file("packages/lastfm")
project(":packages:lrclib").projectDir = file("packages/lrclib")
project(":packages:simpmusic").projectDir = file("packages/simpmusic")