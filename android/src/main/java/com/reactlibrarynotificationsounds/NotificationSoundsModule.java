package com.reactlibrarynotificationsounds;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

public class NotificationSoundsModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private static final int REQUEST_CODE = 102;
    private static final String ATTR_ACTION = "action";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_CATEGORY = "category";
    private static final String TAG_EXTRA = "extra";
    private static final String ATTR_DATA = "data";
    private static final String ATTR_FLAGS = "flags";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_CLASS_NAME = "className";
    private static final String ATTR_CHANNEL_NAME = "channelName";
    private static final String ATTR_CHANNEL_ID = "channelId";
    private static final String NOTIFICATION_CHANNEL_ID = "rn-push-notification-channel-id";
    private static final String NOTIFICATION_CHANNEL_NAME = "rn-push-notification-channel";
    Promise promise;
    String channelId;
    private ReactApplicationContext reactContext;
    private MediaPlayer thePlayer;

    public NotificationSoundsModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "NotificationSounds";
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }

    @ReactMethod
    public void getNotifications(String soundType, final Promise promise) {
        RingtoneManager manager = new RingtoneManager(this.reactContext);
        Integer ringtoneManagerType;

        if (soundType.equals("alarm")) {
            ringtoneManagerType = RingtoneManager.TYPE_ALARM;
        } else if (soundType.equals("ringtone")) {
            ringtoneManagerType = RingtoneManager.TYPE_RINGTONE;
        } else if (soundType.equals("notification")) {
            ringtoneManagerType = RingtoneManager.TYPE_NOTIFICATION;
        } else {
            ringtoneManagerType = RingtoneManager.TYPE_ALL;
        }

        manager.setType(ringtoneManagerType);
        Cursor cursor = manager.getCursor();
        WritableArray list = Arguments.createArray();

        while (cursor.moveToNext()) {
            String notificationTitle = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            String notificationUri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX);
            String id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX);

            WritableMap newSound = Arguments.createMap();
            newSound.putString("title", notificationTitle);
            newSound.putString("url", notificationUri + "/" + id );
            newSound.putString("soundID", id );

            list.pushMap(newSound);
            Log.d("getNotifications: ", notificationUri + id);
        }
        promise.resolve(list);
    }


    @ReactMethod
    public void playSample(String uri){
        try {
            Uri notification;
            if (uri == null || uri.length() == 0) {
                notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            } else {
                notification = Uri.parse(uri);
            }
            if (thePlayer != null) thePlayer.stop();
            thePlayer = MediaPlayer.create(this.reactContext, notification);
            thePlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void stopSample() {
        try {
            if (thePlayer != null) thePlayer.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @ReactMethod
    public void startAndroidActivity(ReadableMap params, final Promise promise) {
        this.promise = promise;
        Intent intent = new Intent();
        if (params.hasKey(ATTR_CLASS_NAME)) {
            ComponentName cn;
            if (params.hasKey(ATTR_PACKAGE_NAME)) {
                cn = new ComponentName(params.getString(ATTR_PACKAGE_NAME), params.getString(ATTR_CLASS_NAME));
            } else {
                cn = new ComponentName(getReactApplicationContext(), params.getString(ATTR_CLASS_NAME));
            }
            intent.setComponent(cn);
        }
        if (params.hasKey(ATTR_ACTION)) {
            intent.setAction(params.getString(ATTR_ACTION));
        }
        // setting data resets type; and setting type resets data; if you have both, you need to set them at the same time
        // https://developer.android.com/guide/components/intents-filters.html#Types (see 'Data' section)
        if (params.hasKey(ATTR_DATA) && params.hasKey(ATTR_TYPE)) {
            intent.setDataAndType(Uri.parse(params.getString(ATTR_DATA)), params.getString(ATTR_TYPE));
        } else {
            if (params.hasKey(ATTR_DATA)) {
                intent.setData(Uri.parse(params.getString(ATTR_DATA)));
            }
            if (params.hasKey(ATTR_TYPE)) {
                intent.setType(params.getString(ATTR_TYPE));
            }
        }
        if (params.hasKey(TAG_EXTRA)) {
            channelId = params.getMap(TAG_EXTRA).getString("android.provider.extra.CHANNEL_ID");
            String channelName = params.hasKey(ATTR_CHANNEL_NAME) ? params.getString(ATTR_CHANNEL_NAME) : null;
            if (channelId != null) {
                NotificationManager manager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (manager.getNotificationChannel(channelId) == null) {
                        NotificationChannel channel = new NotificationChannel(channelId, channelName != null ? channelName : NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                        channel.enableLights(true);
                        channel.enableVibration(true);
                        manager.createNotificationChannel(channel);
                    }
                }
            }
            intent.putExtras(Arguments.toBundle(params.getMap(TAG_EXTRA)));
        }
        if (params.hasKey(ATTR_FLAGS)) {
            intent.addFlags(params.getInt(ATTR_FLAGS));
        }
        if (params.hasKey(ATTR_CATEGORY)) {
            intent.addCategory(params.getString(ATTR_CATEGORY));
        }
        getReactApplicationContext().startActivityForResult(intent, REQUEST_CODE, null);
    }

    @ReactMethod
    public void getAppNotificationSound(ReadableMap inputs, final Promise promise) {
        WritableMap params = Arguments.createMap();
        if (reactContext == null)
        {
            promise.reject(new RuntimeException("reactContext is null."));
        }
        NotificationManager manager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = inputs.hasKey(ATTR_CHANNEL_ID) ? inputs.getString(ATTR_CHANNEL_ID) : null;;
            String channelName = inputs.hasKey(ATTR_CHANNEL_NAME) ? inputs.getString(ATTR_CHANNEL_NAME) : null;;
            channelId = channelId != null ? channelId : NOTIFICATION_CHANNEL_ID;
            channelName = channelName != null ? channelName : NOTIFICATION_CHANNEL_NAME;
            NotificationChannel channel = manager.getNotificationChannel(channelId);

            if (channel == null) {
                channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(true);
                channel.enableVibration(true);
                manager.createNotificationChannel(channel);
            }
            uri = channel.getSound();
            Ringtone ringtone = RingtoneManager.getRingtone(reactContext, uri);
            String title = ringtone.getTitle(reactContext);
            params.putString("soundName", title);
        }
        promise.resolve(params);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
        if (requestCode != REQUEST_CODE) {
            return;
        }
        WritableMap params = Arguments.createMap();
        if (intent != null) {
            params.putInt("resultCode", resultCode);

            Uri data = intent.getData();
            if (data != null) {
                params.putString("data", data.toString());
            }

            Bundle extras = intent.getExtras();
            if (extras != null) {
                params.putMap("extra", Arguments.fromBundle(extras));
            }
        }
        NotificationManager manager = (NotificationManager) reactContext.getSystemService(Context.NOTIFICATION_SERVICE);
        Uri uri;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = manager.getNotificationChannel(this.channelId != null ? this.channelId : NOTIFICATION_CHANNEL_ID);
            if (channel != null) {
                uri = channel.getSound();
                Ringtone ringtone = RingtoneManager.getRingtone(reactContext, uri);
                String title = ringtone.getTitle(reactContext);
                params.putString("soundName", title);
            }
        }
        this.promise.resolve(params);
    }

    @Override
    public void onNewIntent(Intent intent) {
    }
}
