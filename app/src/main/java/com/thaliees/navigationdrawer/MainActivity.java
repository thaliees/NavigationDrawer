package com.thaliees.navigationdrawer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final int REQUEST_IMAGE_CAPTURE_THUMBNAIL = 1;
    private static final int REQUEST_IMAGE_CAPTURE_FULL_SIZE = 2;
    private static final int REQUEST_PERMISSION_STORAGE = 3;
    private static final int REQUEST_READ_IMAGE = 4;
    private static final String DatetimeFormat = "yyyyMMdd_HHmmss";
    private ImageView picture;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);

        // ImageView where it will show the photo taken
        picture = findViewById(R.id.picture);

        // If and only if your application share the photos among all apps
        //checkPermissionDevice();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        switch (item.getItemId()){
            case R.id.nav_camera:
                // For a thumbnail image, use this method:
                //dispatchTakePictureIntent();
                // For a full-sized images, use this method:
                dispatchTakePictureFullIntent();
                break;

            case R.id.nav_gallery:
                openGalleryIntent();
                break;

            case R.id.nav_location:
                startActivity(new Intent(this, MapsActivity.class));
                break;

            case R.id.nav_tools:
                break;

            case R.id.nav_share:
                break;

            case R.id.nav_send:
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Handle the image data when focus returns to the activity
        if (requestCode == REQUEST_IMAGE_CAPTURE_THUMBNAIL && resultCode == RESULT_OK){
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            picture.setImageBitmap(imageBitmap);
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE_FULL_SIZE && resultCode == RESULT_OK){
            /* Invoke the system's media scanner to add your photo to the Media Provider's database,
               making it available in the Android Gallery application and to other apps.
               Only if you use getExternalStoragePublicDirectory, otherwise
               the media scanner cannot access the files because they are private to your app. */
            //galleryAddPic(); // Uncomment this!
            setPic();
        }
        else if (requestCode == REQUEST_READ_IMAGE && resultCode == RESULT_OK){
            if (data != null){
                Uri uri = data.getData();
                setPic(uri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_STORAGE)  {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "Permission OK", Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(this, "Permission needed!", Toast.LENGTH_SHORT).show();
                requestStoragePermission();
            }
        }
    }

    private void dispatchTakePictureIntent(){
        // Delegate action to other application
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            // Launch of the intent
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_THUMBNAIL);
        }
    }

    private void dispatchTakePictureFullIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null){
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            }
            catch (IOException ex){
                // Error occurred while creating the file
            }

            // Continue only if the file was successfully created
            if (photoFile != null) {
                // Here, is necessary to have configure the FileProvider
                Uri photoURI = FileProvider.getUriForFile(this, "com.thaliees.navigationdrawer.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE_FULL_SIZE);
            }

            // Uncomment if and only if your application share the photos among all apps
            /*if (!isPermissionGranted()) {
                Toast.makeText(this, "Permission needed!", Toast.LENGTH_SHORT).show();
                requestStoragePermission();
            }*/
        }
    }

    private File createImageFile() throws IOException{
        // The Android Camera application saves a full-size photo if you give it a file to save into. Then...

        /* Create a unique name (prefix)
           You must provide a fully qualified file name where the camera app should save the photo */
        String timeStamp = new SimpleDateFormat(DatetimeFormat, Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp;

        /* Indicate the storage dir
           Generally, any photos that user captures with the device camera should be saved on the device in the public external storage so they are accessible by all apps.
           Because the directory provided by this method is shared among all apps, reading and writing to it require the permissions.
           Uncomment the two next lines if you want shared among all apps and comment the line 225: */
        //if (!isPermissionGranted()) return null;
        //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // If you want application saves and remains private only for your application
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Create an image file name
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void checkPermissionDevice() {
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        }
    }

    private void requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_PERMISSION_STORAGE);
        }
    }

    private boolean isPermissionGranted(){
        return ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Scaled to match the size of the destination view?
        // Uncomment the lines 263, 264, 270, 271, 274, 278
        // Note: You don't forget to define size of the view in layout. In this project, ImageView does not have its size defined

        // Get the dimensions of the View
        //int targetW = picture.getWidth();
        //int targetH = picture.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        //int photoW = bmOptions.outWidth;
        //int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        //int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        //bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        picture.setImageBitmap(bitmap);
    }

    private void openGalleryIntent(){
        Intent contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        // Filter to only show results that can be "opened", such as a file (as opposed to a list of contacts or timezones)
        contentIntent.addCategory(Intent.CATEGORY_OPENABLE);
        // Filter to show only images, using the image MIME data type.
        contentIntent.setType("image/*");
        startActivityForResult(contentIntent, REQUEST_READ_IMAGE);
    }

    private void setPic(Uri uri){
        try {
            ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();

            picture.setImageBitmap(image);
        }
        catch (IOException ex){ }
    }
}
