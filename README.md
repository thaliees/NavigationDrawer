# Navigation Drawer

[Project created with Android Studio V3.4.2] [Api >= 16]

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
* Set up Google Play Services (build.gradle)
* Specify Permissions (AndroidManifest)

Location settings
* Set up a location request
* Get current location settings and prompt the user to change location setting (if the location settings are not satisfied).

Receive location updates
* Location update callback

Use Geofence
* Creation BroadcastReceiver and JobIntentService
* Specify Permission (AndroidManifest)
* Set up Receiver and Service (AndroidManifest)
* Set up GeofencingClient, create and add geofences (and remove geofences), specify geofences and initial triggers (GeofencingRequest.Builder)
* Define a broadcast receiver for geofence transitions