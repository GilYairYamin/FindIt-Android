package com.example.findit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import java.util.List;

public class ImageRecognitionService extends Service {

    private static final String CHANNEL_ID = "recognizeImageChannel";
    private static final String CHANNEL_NAME = "Image Recognition Channel";
    private static final int NOTIFICATION_ID = 1;
    public static final String ACTION_IMAGE_RECOGNIZED = "com.example.findit.ACTION_IMAGE_RECOGNIZED";
    public static final String EXTRA_BEST_LABEL = "com.example.findit.EXTRA_BEST_LABEL";
    private Bitmap imageBitmap;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("imageBitmap")) {
            byte[] byteArray = intent.getByteArrayExtra("imageBitmap");
            imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            recognizeImage();
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        NotificationChannel notificationChannel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    private void recognizeImage() {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();

        labeler.processImage(image)
                .addOnSuccessListener(firebaseVisionImageLabels -> {
                    String bestLabel = getBestLabel(firebaseVisionImageLabels);
                    sendNotification("FindIt Result", "Found object: " + bestLabel);
                    sendRecognitionBroadcast(bestLabel);
                    stopSelf(); // Stop the service after sending the notification
                })
                .addOnFailureListener(e -> {
                    Log.e("ImageRecognitionService", "Failed to recognize image", e);
                    sendNotification("FindIt Result", "Failed to recognize image.");
                    sendRecognitionBroadcast(null);
                    stopSelf(); // Stop the service after sending the notification
                });
    }

    private String getBestLabel(List<FirebaseVisionImageLabel> labels) {
        String bestLabel = "Nothing Found";
        float highestConfidence = 0;

        for (FirebaseVisionImageLabel label : labels) {
            if (label.getConfidence() > highestConfidence) {
                highestConfidence = label.getConfidence();
                bestLabel = label.getText();
            }
        }
        return bestLabel;
    }

    private void sendNotification(String title, String text) {
        if (areNotificationsEnabled()) {
            createNotificationChannel();
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            Intent intent = new Intent(this, SearchPageActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(pendingIntent)
                    .build();

            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private boolean areNotificationsEnabled() {
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        return prefs.getBoolean("notifications_enabled", true);
    }

    private void sendRecognitionBroadcast(@Nullable String bestLabel) {
        Intent intent = new Intent(ACTION_IMAGE_RECOGNIZED);
        intent.putExtra(EXTRA_BEST_LABEL, bestLabel);
        sendBroadcast(intent);
    }
}
