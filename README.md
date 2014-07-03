Kiosker
=======
## Overview
Kiosker lets you create a kiosk application where you provide content via a webpage.
It is highly configurable through multiple JSON settings.

You can find the app on the [play store](https://play.google.com/store/apps/details?id=dk.itu.kiosker).

Kiosker been designed to work on the Nexus 7 (2013) tablet, but should also work fine on other Android devices.

This source code is an Android Studio project.
Please report any issues you find.

### Lock
If your Android device is rooted you can "lock" the user into your app.
Meaning that the navigation buttons disappear and swiping from the top is disabled.
You can still control the app via an admin settings menu.
This menu can be password protected and you tap 5 times on the screen to access it.


## JSON settings
When you start the app for the first time you need to provide a base url.
This url is used to download settings from a JSON file.
You can provide two JSON files, one for base settings and one for device specific settings.

This is meant for multiple device installs.
All devices share the base settings which are downloaded first.
Then the app downloads the device specific settings which are then added to the base settings.
If any setting is in both files the device specific setting will be chosen.

All the possible settings can be found in the wiki - [JSON Settings](https://github.com/mofus/Kiosker/wiki/JSON-Settings).