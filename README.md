
# react-native-notification-sounds

## Changes in this fork

#### iOS

For notification sounds
1. query the sound files in Library/Sounds directory and application bundle
2. Only support "caf", "aiff", "m4r", "wav" file types
3. do not support the nested directories query, do not query the hidden files
4. query results include the system default notification sound
5. adding promise to playSample and stopSample react-native api


#### Android

1. New api `startAndroidActivity` to redirect user to settings -> app & notification -> <app> -> notification page
2. New api `getAppNotificationSound` to get the currently selected notification sound name
3. Promise resolves the selected sound name 
4. Create a default notification channel within the api `getAppNotificationSound` if the channel does not exist yet. 

#### index.js

1. remove the exported `playSampleSound` and `stopSampleSound` api
