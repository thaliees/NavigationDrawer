# Navigation Drawer

[Project created with Android Studio V3.4.2] [Api >= 15]

## Project Base.
* Choose: Navigation Drawer Activity

##### Option: Camera
Use of the camera to take photos.
* When the user selects the Camera option, from the side menu, the application can take a photo and display it in the application.
* Shared storage implementation (accessible for other applications) or private (available only for the application).
* The image can be small or actual size.

##### Option: Gallery
* When the user selects the Gallery option, from the side menu, the application show the gallery for select a photo and display it in the application.

##### Option: Location
Creation MapsActivity.
* Configure build.gradle (Module: app)
* Configure AndroidManifest

Use Google Location Services APIs, Fused location provider API for get the last know location.
* Configure Google Play Services (build.gradle)
* Configure Permissions (AndroidManifest)