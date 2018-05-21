package com.byteshaft.screenshotapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;
    private static final String TAG = "Tag";
    private static final int REQUEST_SCREENSHOT=59706;
    private MediaProjectionManager mgr;
    protected boolean askedCanDraw = false;
    private boolean foreground = true;
    private Button start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        foreground = true;
        start = findViewById(R.id.start);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(MainActivity.this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
                    askedCanDraw = true;
                    return;
                }
                if (isStoragePermissionGranted()) {
                    mgr = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
                    startActivityForResult(mgr.createScreenCaptureIntent(),
                            REQUEST_SCREENSHOT);
                }
            }
        });
        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //you have to ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
            askedCanDraw = true;
        } else {
            if (isStoragePermissionGranted()) {

            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        foreground = false;

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    /**
     * Set and initialize the view elements.
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
            //resume tasks needing this permission
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==REQUEST_SCREENSHOT) {
            if (resultCode==RESULT_OK) {
                Intent i =
                        new Intent(this, ScreenRecordingService.class)
                                .putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
                                .putExtra(ScreenRecordingService.EXTRA_RESULT_INTENT, data);

                startService(i);
                finish();
            }
        }
        if (requestCode == CODE_DRAW_OVER_OTHER_APP_PERMISSION) {
            Log.i("TAG", "draw over other app" +resultCode);
            //Check if the permission is granted or not.
            if (String.valueOf(resultCode).equals("app0")) {
                Log.i("TAG", "draw over other app ok");
                if (isStoragePermissionGranted()) {
                    mgr= (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
                    startActivityForResult(mgr.createScreenCaptureIntent(),
                            REQUEST_SCREENSHOT);
                }
            } else { //Permission is not available
//                Toast.makeText(this,
//                        "Draw over other app permission not available. Closing the application",
//                        Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
