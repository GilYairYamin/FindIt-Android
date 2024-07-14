package com.example.findit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * LabelHandlerService is a service that handles image recognition and uploading tasks.
 * It uses Firebase ML Kit for image labeling and Firebase Storage for storing images with metadata.
 */
public class LabelHandlerService extends Service implements LocationListener {

    public static final String EXTRA_IMAGE = "com.example.findit.EXTRA_IMAGE"; // Key for passing image data through Intent
    private static final String CHANNEL_ID = "recognizeImageChannel"; // Notification channel ID
    private static final String CHANNEL_NAME = "Image Recognition Channel"; // Notification channel name
    private static final String PREFS_NAME = "FindItPrefs"; // SharedPreferences file name
    private static final String KEY_TOAST_SHOWN = "locationPermissionToastShown"; // Key for tracking if the toast has been shown
    private Bitmap imageBitmap; // Bitmap of the image to be processed
    private LocationManager locationManager; // LocationManager for accessing device location
    private String locationString = "Location not available."; // String to hold the location information
    private String bestLabel; // Best label found in image recognition
    private Handler handler; // Handler to post tasks to the main thread

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper()); // Initialize handler for main thread tasks
    }

    /**
     * Handles the service start command.
     * Processes the image and recognizes labels using Firebase ML Kit.
     *
     * @param intent  Intent containing the image data
     * @param flags   Flags for the start request
     * @param startId Start ID for the request
     * @return START_NOT_STICKY to indicate that the service should not be recreated if it is killed
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null && intent.hasExtra(EXTRA_IMAGE))
        {
            byte[] byteArray = intent.getByteArrayExtra(EXTRA_IMAGE);
            if (byteArray != null)
            {
                imageBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                createNotificationChannel();
                recognizeImage(() -> {
                    if (isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION))
                        requestSingleLocationUpdate();
                    else
                    {
                        showLocationPermissionDeniedToastOnce();
                        uploadImageToStorage(bestLabel, locationString);
                    }
                });
            }

            else
            {
                handler.post(() -> Toast.makeText(this, "Failed to load image data.", Toast.LENGTH_SHORT).show());
                stopSelf();
            }
        }

        else
            stopSelf();


        return START_NOT_STICKY;
    }

    /**
     * Creates a notification channel for displaying image recognition results.
     */
    private void createNotificationChannel()
    {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        NotificationChannel notificationChannel = new NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    /**
     * Checks if notifications are enabled.
     *
     * @return true if notifications are enabled, false otherwise
     */
    private boolean areNotificationsEnabled()
    {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean("notifications_enabled", true);
    }

    /**
     * Sends a notification with the given title and text if notifications are enabled.
     *
     * @param title The title of the notification
     * @param text  The text content of the notification
     */
    private void sendNotification(String title, String text)
    {
        if (areNotificationsEnabled())
        {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            Intent intent = new Intent(this, SearchPageActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            int notificationId = new Random().nextInt(100000); // Generate a random notification ID

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setContentIntent(pendingIntent)
                    .build();

            notificationManager.notify(notificationId, notification);
        }
    }

    /**
     * Recognizes labels in the image using Firebase ML Kit.
     *
     * @param onComplete Runnable to execute after recognition is complete
     */
    private void recognizeImage(Runnable onComplete)
    {
        new Thread(() -> {
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
            FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();

            labeler.processImage(image)
                    .addOnSuccessListener(firebaseVisionImageLabels -> {
                        if (firebaseVisionImageLabels.isEmpty())
                        {
                            sendNotification("FindIt Result", "No labels found.");
                            handler.post(() -> Toast.makeText(this, "No labels found.", Toast.LENGTH_LONG).show());
                            onComplete.run();
                            return;
                        }

                        bestLabel = "Nothing Found";
                        float highestConfidence = 0;

                        for (FirebaseVisionImageLabel label : firebaseVisionImageLabels)
                        {
                            if (label.getConfidence() > highestConfidence)
                            {
                                highestConfidence = label.getConfidence();
                                bestLabel = label.getText();
                            }
                        }

                        sendNotification("FindIt Result", "Found object: " + bestLabel);
                        handler.post(() -> Toast.makeText(this, "Found object: " + bestLabel, Toast.LENGTH_LONG).show());
                        broadcastLabelResult(bestLabel);
                        onComplete.run();
                    })
                    .addOnFailureListener(e -> {
                        sendNotification("FindIt Result", "Failed to get data.");
                        handler.post(() -> Toast.makeText(this, "Failed to get data.", Toast.LENGTH_LONG).show());
                        broadcastLabelResult("Failed to get data.");
                        onComplete.run();
                    });
        }).start();
    }

    private void broadcastLabelResult(String label)
    {
        Intent intent = new Intent("FindIt.LABEL_RESULT");
        intent.putExtra("label", label);
        sendBroadcast(intent);
    }

    /**
     * Requests a single location update.
     */
    @SuppressLint("MissingPermission")
    private void requestSingleLocationUpdate()
    {
        locationManager = getSystemService(LocationManager.class);
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
        else
            uploadImageToStorage(bestLabel, "Location not available.");
    }

    /**
     * Uploads the image to Firebase Storage with metadata.
     *
     * @param name     The name of the image
     * @param location The location metadata for the image
     */
    private void uploadImageToStorage(String name, String location)
    {
        new Thread(() -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null)
            {
                Random random = new Random();
                int id = random.nextInt(1000000000);

                String email = user.getEmail();
                FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
                StorageReference storageRef = firebaseStorage.getReference().child("images/" + email + "/" + name + "_" + id + ".jpg");

                StorageMetadata metadata = new StorageMetadata.Builder()
                        .setCustomMetadata("location", location)
                        .setCustomMetadata("author", email)
                        .build();

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] imageBytes = stream.toByteArray();
                UploadTask uploadTask = storageRef.putBytes(imageBytes, metadata);

                uploadTask.addOnFailureListener(e -> stopSelf())
                        .addOnSuccessListener(taskSnapshot -> stopSelf());
            }

            else
            {
                handler.post(() -> Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show());
                stopSelf();
            }
        }).start();
    }

    /**
     * Shows a toast message indicating that location permission is denied, but only once.
     */
    private void showLocationPermissionDeniedToastOnce()
    {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean toastShown = prefs.getBoolean(KEY_TOAST_SHOWN, false);
        if (!toastShown)
        {
            handler.post(() -> Toast.makeText(this, "Location permission denied. Using default location.", Toast.LENGTH_SHORT).show());
            prefs.edit().putBoolean(KEY_TOAST_SHOWN, true).apply();
        }
    }

    /**
     * Check if the network is available.
     *
     * @return true if the network is available, false otherwise.
     */
    private boolean isNetworkAvailable()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        if (network != null)
        {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }

        return false;
    }

    /**
     * Callback for when the location has changed.
     *
     * @param location The new location
     */
    @Override
    public void onLocationChanged(Location location)
    {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        locationString = latitude + ", " + longitude;

        if (Geocoder.isPresent())
        {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try
            {
                List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                if (addressList != null && !addressList.isEmpty())
                {
                    Address address = addressList.get(0);
                    locationString = address.getAddressLine(0);
                }
            }

            catch (Exception e)
            {
                if (!isNetworkAvailable())
                {
                    handler.post(() -> Toast.makeText(this, "Network not available. Using location coordinates.", Toast.LENGTH_SHORT).show());
                    return;
                }

                handler.post(() -> Toast.makeText(this, "Failed to get address from location", Toast.LENGTH_SHORT).show());
            }
        }
        locationManager.removeUpdates(this);
        uploadImageToStorage(bestLabel, locationString);
    }

    /**
     * Callback for when the GPS provider is enabled.
     *
     * @param provider The provider that was enabled
     */
    @Override
    public void onProviderEnabled(String provider)
    {
        handler.post(() -> Toast.makeText(this, "GPS Enabled!", Toast.LENGTH_LONG).show());
    }

    /**
     * Callback for when the GPS provider is disabled.
     *
     * @param provider The provider that was disabled
     */
    @Override
    public void onProviderDisabled(String provider)
    {
        handler.post(() -> Toast.makeText(this, "GPS Disabled!", Toast.LENGTH_LONG).show());
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    /**
     * Checks if a specific permission is granted.
     *
     * @param permission The permission to check
     * @return true if the permission is granted, false otherwise
     */
    private boolean isPermissionGranted(String permission)
    {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
