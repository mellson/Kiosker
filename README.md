Kiosker
=======
## Overview
Kiosker lets you create a kiosk application where you provide content via a webpage.
It is highly configurable through multiple JSON settings.

Kiosker been designed to work on the Nexus 7 (2013) tablet, but should also work fine on other Android devices.

[The Wiki](https://github.com/mofus/Kiosker/wiki/) contains information about how to setup and use Kiosker.

This source code is an Android Studio project.
Please report any issues you find.

### Entering the settings screen
Inorder to get to the settings screen on a running Kiosker app you need to tap 5 times on the screen.
This will bring up the settings screen protected by a password if you have set one.
You can change the number of taps of method of opening the settings in method `addTapToSettings` from the file  [WebController.java](https://github.com/mofus/Kiosker/blob/master/Kiosker/src/main/java/dk/itu/kiosker/controllers/WebController.java)
