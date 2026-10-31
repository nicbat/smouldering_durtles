# Smoky Durtles

For ongoing development, start with [TASKS.md](TASKS.md) and [HANDOFF.md](HANDOFF.md).
[UI_GUIDE.md](UI_GUIDE.md) records the Material 3 principles, implementation patterns and visual review checklist.

Smoky Durtles is a personal fork of Smouldering Durtles with a separate installation
identity, a one-tap toolbar search fix and a modern Material 3 interface across the app.
The first full design pass is implemented; phone feedback and measured optimization are
next, with widgets last. Work is tracked in [FORK_PLAN.md](FORK_PLAN.md).

## Appearance

**Settings → Display settings → Modern interface** controls browsing, subject details,
lessons/reviews, settings, customization and utility screens. Turn it off to use Classic.
**Wallpaper colors in interface** enables dynamic color on supported Android 12+ devices;
older devices use static light/dark palettes. Modern screens follow system light/dark mode.
Existing subject/SRS palettes remain independent of Material accents.

**Modern dashboard** and **Wallpaper colors on dashboard** control the dashboard separately.
Its Compose Material 3 overview launches the existing study actions. Custom forecast,
SRS, level and statistics Views retain their calculations and navigation, with refreshed
surfaces and scalable labels. Recent/critical/burned subjects keep WaniKani colors.

The rest of the app keeps its existing Views and behavior, with Material menus/dialogs,
outlined inputs, sliders, rounded actions and coherent page styling. Advanced filters,
font/color tools and audio-download controls are included. Study grading, scheduling
and submission logic are unchanged. The color wheel, custom subject diagrams and
Android-owned pickers retain their specialized UI.

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
up; the inherited release build currently also uses debug signing. Development is tracked in [the personal fork](https://github.com/nicbat/smouldering_durtles).
No dedicated support address or store listing is configured.

Run the focused identity and toolbar regression tests on a connected emulator/device:

```sh
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.smouldering_durtles.wk.test.SearchExpansionTest,com.smouldering_durtles.wk.test.ForkIdentityTest
```

The historical migration fixtures have been restored from Git history. See
[HANDOFF.md](HANDOFF.md) for current instrumentation evidence and remaining device checks.
API 21 is still declared, but its existing Jackson dependency fails to initialize in
serialization-dependent tests; the full app is not currently verified on that version.
Never run instrumentation on an account-bearing phone; use an explicitly targeted emulator.

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

