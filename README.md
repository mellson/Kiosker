Kiosker
=======
## Outline
[Overview](#Overview)

[Lock](#Lock)

[JSON settings](#JSON settings)

[Settings rundown](#Settings rundown)

[Kiosker Watchdog](#Kiosker Watchdog)

[Suggested configuration for Nexus 7 devices](#Suggested configuration for Nexus 7 devices)

## Overview
Kiosker lets you create an app based on Android's webview.
It is highly configurable via downloaded JSON settings you provide yourself.

You can find the app on the [play store](https://play.google.com/store/apps/details?id=dk.itu.kiosker).

This has been designed to work on the Nexus 7 (2013) tablet, but should also work fine on other Android devices.
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


### Settings rundown
As seen in the [example JSON settings file](https://github.com/mofus/Kiosker/blob/master/example.json) there are a lot of options to be set on Kiosker.
This will explain every current possible setting.

- `layout` - `int` that decides how many webviews should be on screen.
    - Example: `"layout": 0`
    - Possible layout configs are:
        - `0` Fullscreen, only one webview is shown, which makes the sites urls unnecessary.
        - `1` 50/50 split between two webviews.
        - `2` 60/40 split between two webviews.
        - `3` 70/30 split between two webviews.
        - `4` 80/20 split between two webviews.
- `home` - `Array of {url, title}` to be used as the main website.
    - Currently only the first url is used.
    - Example: `"home": [{ "url":"http://google.com", "title":"Google"}]`
- `sites` - `Array of {url, title}` to be used on the secondary panels.
    - Example: `"sites": [{ "url":"http://itu.dk", "title":"ITU"}]`
- `screensavers` - `Array of {url, title}` to be used as screensavers.
    - Example: `"screensavers": [{"url":"http://www.cafeanalog.dk/", "title":"Cafe Analog"}]`
- `screenSavePeriodMins` - `int` that decides after how many minutes the screensaver kicks in.
    - The app will randomly load a url from the `screensavers` array.
    - Example: `"screenSavePeriodMins": 30`
- `screenSaveLengthMins` - `int` that decides how many minutes the screensaver will run before going back to the `home` url.
    - Example: `"screenSaveLengthMins": 2`
- `idlePeriodMins` - `int` that decides after how many minutes the app will dim the display to the `dimmedBrightness` value.
    - Example: `"idlePeriodMins": 120`
- `resetToHomeMins` - `int` that decides after how many minutes the app will return to the `home` url if that is not already showing.
    - Example: `"resetToHomeMins": 2`
- `masterPasswordHash` - Hash value of your master password.
    - Example: `"masterPasswordHash": "MqWMDQjArAHAlbCSywKedct3y9DY"`
- `masterPasswordSalt` - Salt value of your master password.
    - Example: `"masterPasswordSalt": "uxKOb>lLhk5lb=pFzYrs2dWsVEakI"`
- `standbyStartTime` - Decides when the app should turn off the screen and enter a standby state.
    - Once this time has been reached the app will start the normal display sleep timer. If this timeout has been set to 5 minutes the device will enter the standby state at `18.05` in this example.
    - Example: `"standbyStartTime": "18.00"`
- `standbyStopTime` - Decides when the app should wake from the standby state.
    - Example: `"standbyStopTime": "07.30"`
- `reloadPeriodMins` - `int` that decides after how many minutes the webviews should be reloaded.
    - Example: `"reloadPeriodMins": 360`
- `allowSwitching` - `bool` that if true, allows the user to switch between sites in the `sites` array from a navigation panel.
    - Example: `"allowSwitching": true`
- `autoCycleSecondary` - `bool` that if true, will cycle the secondary panel's web pages in a round robin fashion.
    - Example: `"autoCycleSecondary": true`
- `autoCycleSecondaryPeriodMins` - `int` that decides how long each secondary panel cycle should be.
    - Example: `"autoCycleSecondaryPeriodMins": 20`
- `errorReloadMins` - `int` that decides how long after an error occurred a webview should be reloaded.
    - Example: `"errorReloadMins": 5`
- `allowHome` - `bool` that decides if the navigation buttons are visible or not.
    - If this is set to false the user is locked in the app.
    - Example: `"allowHome": false`
- `volume` - `int` that sets the volume of the device in percent from 0 to 100.
    - Example: `"volume": 30`
- `mute` - `bool` that decides if the device should be mutes.
    - Example: `"mute": false`
- `brightness` - `int` that sets the brightness of the device in percent form 0 to 100.
    - For this to work set the brightness of the device to manual.
    - Example: `"brightness": 100`
- `dimmedBrightness` - `int` that sets the brightness level of the device in percent form 0 to 100 during the `dim` period.
    - For this to work set the brightness of the device to manual.
    - Example: `"dimmedBrightness": 70`
- `quietHoursStartTime` - Decides when the device should start a silent period where the volume is set to 0 (muted).
    - Example: `"quietHoursStartTime": "18.00"`
- `quietHoursStopTime` - Decides when the device should stop the silent period and return to the normal `volume`.
    - Example: `"quietHoursStopTime": "07.30"`
- `manualWifi` - `bool` that decides if the device should manually connect to the provided `wifiSSID`.
- `wifiSSID` - The SSID which the device should connect to.

## Kiosker Watchdog
Kiosker has a companion app which can be run in the background.
This app checks every 10 minutes that Kiosker is in the foreground.
If it is not it will be started again by the watchdog app.

This app in also available on the play store [play store](https://play.google.com/store/apps/details?id=dk.itu.mellson.kioskerwatchdog).

And the source code is available on [github](https://github.com/mofus/Kiosker-Watchdog).

## Suggested configuration for Nexus 7 devices
1. Install min. Android 4.4.2.

2. Root device (with Nexus Root Toolkit - http://www.wugfresh.com/nrt/)

3. Complete root installation by installing busy box.

4. Configure SuperSU to accept privileges across app updates.

5. Configure SuperSU to not show toasts.

6. Turn off screen lock on device.

7. Turn off daydream under display on device.

8. Set sleep time of device to 1 min.

9. Set brightness to manual and turn it all the way up.

10. Boot into fastboot and change powermode (so that device turns on when it is plugged into a charger)
    - Change the powermode by running this command: fastboot oem off-mode-charge 0

11. Turn off USB debugging.

12. Install Kiosker from the Play App store or from your own build.

13. Install KioskerWatchdog from the Play App store or from your own build.

14. Configure Kiosker with a base url.

15. Configure the specific deviceid if you have more devices.

16. Set Kiosker as the default launcher.

17. Start KioskerWatchdog.