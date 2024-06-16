# FindIt

## Introduction

FindIt is an Android application designed to provide a seamless experience for recognizing and managing images. Built using Java and Firebase, the app allows users to capture images, upload from the gallery, and perform image recognition using Firebase ML Kit. The project showcases robust permission handling, notification services, and a comprehensive user management system.

## Features

- **Image Recognition**: Utilizes Firebase ML Kit to identify objects within images.
- **Location-based Services**: Integrates GPS to tag images with location metadata.
- **Firebase Integration**: Implements Firebase for authentication, storage, and real-time database.
- **Notification Services**: Provides notifications for image recognition results.
- **User Management**: Supports user authentication, profile management, and search history.

## Components

The application is structured into several components:

- `SearchPageActivity`: The main activity that handles image capture, gallery upload, and search functionalities.
- `SettingsActivity`: Manages app settings such as enabling/disabling notifications, resetting password, and managing permissions.
- `HistoryActivity`: Displays the search history of uploaded images.
- `ImageAdapter`: Custom adapter for displaying images in a list view.
- `User`: Represents user data within the application.

## How It Works

1. **SearchPageActivity**:
    - Users can capture images using the device camera or upload from the gallery.
    - Images can be recognized using Firebase ML Kit, and results are displayed via notifications and on the screen.
    - Location metadata is tagged to images using the device's GPS.
2. **SettingsActivity**:
    - Allows users to enable/disable notifications, manage permissions, reset passwords, and clear search history.
3. **HistoryActivity**:
    - Displays a list of previously uploaded images along with their metadata.

## Running the Application

To run the application locally:

1. Ensure you have Android Studio installed.
2. Clone the repository: `git clone https://github.com/GilYairYamin/findit.git`
3. Open the project in Android Studio.
4. Build the project and run it on an emulator or physical device with internet access.

## Example
Here’s an example of the main interface:

![FindIt Interface](https://github.com/OmriNaor/FindIt/assets/106623821/example.png)

## Testing

To test the application, you can manually interact with the app, ensuring that:

- Permissions are requested and handled correctly.
- Images can be captured and uploaded from the gallery.
- Image recognition returns accurate results and displays notifications.
- Location metadata is correctly tagged to images.
- User authentication and profile management work as expected.

## Remarks

FindIt demonstrates a robust and feature-rich Android application implemented using Java and Firebase. It highlights best practices for handling permissions, integrating third-party services, and providing a smooth user experience. This project serves as an excellent example for those looking to learn or enhance their skills in Android development.
