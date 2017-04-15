# ST-Better-LIFX-BULB
## Summary
Custom device handler for the LIFX bulb.

## This device handler supports
On / Off
Setting Color
Setting Color Temperature
Setting Brightness

## Installation via GitHub Integration
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My Device Handlers" section in the navigation bar.
3. Click on "Settings".
4. At the bottom of the popup window, click "Add New Repository".
5. Enter "ericvitale" as the namespace.
6. Enter "ST-Better-LIFX-Bulb" as the repository.
7. Ensure that the branch is set to "master", it should be this way by default.
8. Hit "Save".
9. Select "Update from Repo" and select "ST-Better-LIFX-Bulb".
10. Select (check) "better-lifx-bulb.groovy".
11. Check "Publish" and hit "Execute".
12. See the "Preferences" & "How to get your API Token" sections below on how to configure.

## Manual Installation (if that is your thing)
1. Open SmartThings IDE in your web browser and log into your account.
2. Click on the "My Device Handlers" section in the navigation bar.
3. On your this page, click on the "+ Create New Device Handler" button on the right.
4. On the "New Device Type" page, Select the Tab "From Code" , Copy the "better-lifx-bulb.groovy" source code from GitHub and paste it into the IDE editor window.
5. Click the blue "Create" button at the bottom of the page. An IDE editor window containing device handler template should now open.
6. Click the blue "Save" button above the editor window.
7. Click the "Publish" button next to it and select "For Me". You have now self-published your Device Handler.
8. See the "Preferences" & "How to get your API Token" sections below on how to configure.

## Preferences
1. API Token - [Required] You have to get this from LIFX. It is a long character string so text it to yourself and copy and paste it in.
2. Bulb Name - [Required] Must match the exact bulb you want to control. Use the name  you give it in the LIFX app.
3. Log Level - Enter: TRACE, DEBUG, INFO, WARN, ERROR
4. Update Frequency (Frequency, Between Hours) - If you have a lot of these bulbs and control them with different automation, you will often find that the DH doesn't exactly know the state of the bulb since the DH's communicate directly with LIFX. This setting allows you to setup a frequency for the DH to check the status of the bulb and update accordingly. 5 minutes should be good.

## How to get your API Token
Navigate to https://cloud.lifx.com, sign in and then go to the settings section and select generate new token.
