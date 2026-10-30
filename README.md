# Smoky Durtles

Smoky Durtles is a personal fork of Smouldering Durtles. The initial work adds an
independent installation identity and fixes toolbar search; the Material 3
dashboard, widget improvements, and reliability work are tracked in
[FORK_PLAN.md](FORK_PLAN.md).

## Local development build

Use JDK 17 and the Android SDK with platform 36. Set `ANDROID_HOME` to your SDK
directory (or configure `sdk.dir` in an untracked `local.properties`), then run:

```sh
./gradlew assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`, labeled **Smoky Durtles - Dev**.
Its application ID is `com.smoky_durtles.wk.debug`; the release ID is
`com.smoky_durtles.wk`. Both differ from upstream, including provider authorities.
The Java namespace stays unchanged to avoid an unrelated source/package migration.
Subject links use `smoky-durtles://subject-info`.

The development build has its own local data and credentials and does not import
the original app's data automatically. Both apps still access the same WaniKani
account if given that account's token. Sync before switching between them; avoid
running overlapping study sessions while offline submissions are pending.

Development APKs use the local debug signing key. Keep that key for subsequent
development updates. A durable release signing/update process is still to be set
up; the inherited release build currently also uses debug signing. No public
Smoky Durtles repository, support address, or store listing is configured yet.

Run the focused identity and toolbar regression tests on a connected emulator/device:

```sh
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smouldering_durtles.wk.test.SearchExpansionTest,com.smouldering_durtles.wk.test.ForkIdentityTest
```

The existing full instrumentation suite has 20 known missing-schema fixture
failures. See the planning note for the baseline and recovery work.

## Upstream project

Smouldering Durtles is an Android app for WaniKani, forked from the existing codebase of Flaming Durtles, the popular open source client that has fallen into disprear. I'm Joeni over on the site and took on updating the application to support changes in WaniKani's API, in the hopes that Android users will continue to have access.

The following is the original information regarding licencing and sharing:

It's set up as an Android project with Google's default gradle set up.
Use Android Studio or the Gradle command line to build it.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.smouldering_durtles.wk/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.smouldering_durtles.wk)

Or download the latest APK from the [Releases Section](https://github.com/jerryhcooke/smouldering_durtles/releases/latest).

## Preparing to build the code

Before you can build this code, you will have to replace the existing two files containing identification
information for the app with your own versions. This is because the open source license covering this app's 
code does not cover the name I gave the app, and it also doesn't cover my name. See the file LICENSE.md for details.

- Remove any existing Indentification.java file from app/src/main/java/com.smouldering_durtles.wk
- Copy the file app/Identification.java.sample.txt to app/src/main/java/com.smouldering_durtles.wk
- Name the copy Identification.java
- Edit the file to supply your own identification for the app
- Remove any existing strings.xml file from app/src/res/values
- Copy the file app/strings.xml.sample.txt to app/src/main/res/values
- Name the copy strings.xml
- Edit the file to supply your own identification for the app

After this, you are ready to build the code.

