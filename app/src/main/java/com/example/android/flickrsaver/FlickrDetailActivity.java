package com.example.android.flickrsaver;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FlickrDetailActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST = 1;
    private Bitmap mImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flickr_detail);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        String imgUrl = getIntent()
                .getStringExtra(FlickrSearchActivity.URL_KEY);

        ImageView detailImage = findViewById(R.id.detailImageView);
        Glide.with(this).asBitmap().listener(new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e,
                    Object model,
                    Target<Bitmap> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model,
                    Target<Bitmap> target, DataSource dataSource,
                    boolean isFirstResource) {
                mImage = resource;
                return false;
            }
        }).load(imgUrl).into(detailImage);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Called when you request permission to read and write to external storage
        switch (requestCode) {
            case PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // If you get permission, launch the camera
                    saveImage();
                } else {
                    // If you do not get permission, show a Toast
                    Toast.makeText(this, R.string.permission_denied,
                            Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                saveImage();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void saveImage() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        } else {
            // If you have the permission, save the image
            if(mImage != null) {
                String savedImagePath;
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                        Locale.getDefault()).format(new Date());
                String imageFileName = "JPEG_" + timeStamp + ".jpg";
                File storageDir = new File(Environment
                        .getExternalStoragePublicDirectory
                                (Environment.DIRECTORY_PICTURES), "/Flickr_Saver");

                boolean success = true;
                if (!storageDir.exists()) {
                    success = storageDir.mkdirs();
                }

                // Save the new Bitmap
                if (success) {
                    File imageFile = new File(storageDir, imageFileName);
                    savedImagePath = imageFile.getAbsolutePath();
                    try {
                        Log.d("TAG", "saveImage: here");
                        OutputStream fOut = new FileOutputStream(imageFile);
                        mImage.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                        fOut.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // Force a scan of the Media content provider
                    Intent mediaScanIntent = new Intent
                            (Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.fromFile(imageFile);
                    mediaScanIntent.setData(contentUri);
                    this.sendBroadcast(mediaScanIntent);

                    // Show a Toast with the save location
                    String savedMessage =
                            getString(R.string.saved_message, savedImagePath);
                    Toast.makeText(this, savedMessage, Toast.LENGTH_SHORT)
                            .show();
                }

            } else {
                Toast.makeText(this, "Image not ready", Toast.LENGTH_SHORT).show();
            }
        }

    }
}
