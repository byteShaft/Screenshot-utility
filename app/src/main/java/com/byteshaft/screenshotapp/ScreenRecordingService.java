package com.byteshaft.screenshotapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class ScreenRecordingService extends Service {

    private WindowManager mWindowManager;
    private View mFloatingView;
    private static final int PORT = 42380;
    private static final int TIMEOUT = 1000;
    private static final int NOTIFY_ID = 9906;
    static final String EXTRA_RESULT_CODE = "resultCode";
    static final String EXTRA_RESULT_INTENT = "resultIntent";
    static final String ACTION_RECORD =
            BuildConfig.APPLICATION_ID + ".RECORD";
    static final String ACTION_SHUTDOWN =
            BuildConfig.APPLICATION_ID + ".SHUTDOWN";
    static final int VIRT_DISPLAY_FLAGS =
            DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY |
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private MediaProjection projection;
    private VirtualDisplay vdisplay;
    final private HandlerThread handlerThread =
            new HandlerThread(getClass().getSimpleName(),
                    android.os.Process.THREAD_PRIORITY_BACKGROUND);
    private Handler handler;
    private MediaProjectionManager mgr;
    private WindowManager wmgr;
    private ImageTransmogrifier it;
    private int resultCode;
    private Intent resultData;
    private WindowManager.LayoutParams params;
    private boolean clicked = false;
    private String nameOfTheFile = "";

    public ScreenRecordingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Inflate the floating view layout we created
        mgr = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        //Add the view to the window.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        } else {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        addOverlay();

        //Drag and move floating view using user's touch action.
    }

    private void addOverlay() {
        if (mWindowManager != null && mFloatingView != null) {
            try {
                mWindowManager.removeViewImmediate(mFloatingView);
            } catch (IllegalArgumentException e) {
                Log.i("TAG", "info");
            }
        }
        mFloatingView = null;
        mWindowManager = null;
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);
        wmgr = (WindowManager) getSystemService(WINDOW_SERVICE);
        //Add the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        //Set the close button
        ImageView closeButtonCollapsed = mFloatingView.findViewById(R.id.close_btn);
        closeButtonCollapsed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //close the service and remove the from from the window
                stopSelf();
            }
        });
        mFloatingView.findViewById(R.id.root_container).setOnTouchListener(new View.OnTouchListener() {

            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (event.getRawX() - initialTouchX);
                        int Ydiff = (int) (event.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            Date now = new Date();
                            android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
                            nameOfTheFile = now.toString();
                            startCapture(now.toString());
                            mFloatingView.setVisibility(View.GONE);
                            mWindowManager.removeViewImmediate(mFloatingView);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //Calculate the X and Y coordinates of the view.
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        //Update the layout with new X & Y coordinate
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (i.getAction() == null) {
            resultCode = i.getIntExtra(EXTRA_RESULT_CODE, 1337);
            resultData = i.getParcelableExtra(EXTRA_RESULT_INTENT);
            foregroundify();
        } else if (ACTION_RECORD.equals(i.getAction())) {
            if (resultData != null) {
                Date now = new Date();
                android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);
                startCapture(now.toString());
            } else {
            }
        } else if (ACTION_SHUTDOWN.equals(i.getAction())) {
            stopForeground(true);
            stopSelf();
        }

        return (START_NOT_STICKY);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
    }

    WindowManager getWindowManager() {
        return (wmgr);
    }

    Handler getHandler() {
        return (handler);
    }

    private boolean processingImage = false;

    void processImage(final byte[] png, final String fileName) {
        addOverlay();
        if (processingImage) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                processingImage = true;
                String mPath = Environment.getExternalStorageDirectory() + File.separator +
                        getString(R.string.app_name);
                File folder = new File(mPath);
                if (!folder.exists()) {
                    folder.mkdirs();
                }
                File file = new File(mPath, (fileName + ".png"));
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    file.delete();
                }
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(png);
                    fos.flush();
                    fos.getFD().sync();
                    fos.close();

                    MediaScannerConnection.scanFile(ScreenRecordingService.this,
                            new String[]{file.getAbsolutePath()},
                            new String[]{"image/png"},
                            null);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Exception writing out screenshot", e);
                }
                processingImage = false;
            }
        }.start();
        stopCapture();
    }

    private void stopCapture() {
        if (projection != null) {
            projection.stop();
            vdisplay.release();
            projection = null;
        }
        String mPath = Environment.getExternalStorageDirectory() + File.separator +
                getString(R.string.app_name);
        Toast.makeText(this, mPath + File.separator + nameOfTheFile, Toast.LENGTH_SHORT).show();

        Intent imageIntent = new Intent(getApplicationContext(), DialogActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("image", mPath + File.separator + nameOfTheFile + ".png");
        imageIntent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(imageIntent);
        startActivity(imageIntent);

//        Intent i =
//                new Intent(this, ScreenRecordingService.class)
//                        .putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
//                        .putExtra(ScreenRecordingService.EXTRA_RESULT_INTENT, resultData);
//
//        startService(i);

    }

    private void startCapture(String fileName) {
        projection = mgr.getMediaProjection(resultCode, resultData);
        it = new ImageTransmogrifier(this, fileName);
        MediaProjection.Callback cb = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                vdisplay.release();
            }
        };

        vdisplay = projection.createVirtualDisplay("ScreenShot App",
                it.getWidth(), it.getHeight(),
                getResources().getDisplayMetrics().densityDpi,
                VIRT_DISPLAY_FLAGS, it.getSurface(), null, handler);
        projection.registerCallback(cb, handler);
    }

    private void foregroundify() {
        String channelId = getPackageName();
        NotificationChannel channel;
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(this, channelId);
        b.setSound(null);
        b.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL);
        b.setContentTitle(getString(R.string.app_name))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(getString(R.string.app_name));
        b.addAction(R.mipmap.ic_launcher,
                "Capture",
                buildPendingIntent(ACTION_RECORD));

        b.addAction(android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                buildPendingIntent(ACTION_SHUTDOWN));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            CharSequence name = getString(R.string.app_name);
            String description = "Service for screenshot";
            final int importance = NotificationManager.IMPORTANCE_NONE;
            channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system
        }

        startForeground(NOTIFY_ID, b.build());
    }

    private PendingIntent buildPendingIntent(String action) {
        Intent i = new Intent(this, getClass());
        i.setAction(action);
        return (PendingIntent.getService(this, 0, i, 0));
    }


    // new

}
