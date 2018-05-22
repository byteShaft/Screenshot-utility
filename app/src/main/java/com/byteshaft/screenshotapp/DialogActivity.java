package com.byteshaft.screenshotapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;

public class DialogActivity extends Activity implements View.OnClickListener {

    private ImageButton mButtonClose;
    private ImageButton mButtonDelete;
    private ImageButton mButtonShare;
    private ImageView mScreenshot;
    private String imagePath = null;
    private Bitmap myBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        imagePath = getIntent().getStringExtra("image");
        setContentView(R.layout.activity_dialog);
        mButtonClose = findViewById(R.id.button_close);
        mButtonDelete = findViewById(R.id.button_delete);
        mButtonShare = findViewById(R.id.button_share);
        mScreenshot = findViewById(R.id.screenshot);

        mButtonClose.setOnClickListener(this);
        mButtonDelete.setOnClickListener(this);
        mButtonShare.setOnClickListener(this);
        File imgFile = new File(imagePath);

        if (imgFile.exists()) {
            myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            mScreenshot.setImageBitmap(myBitmap);

        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_close:
                finish();
                break;
            case R.id.button_delete:
                File file = new File(imagePath);
                file.delete();
                getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                finish();

                break;
            case R.id.button_share:
                Intent intent = new Intent(Intent.ACTION_SEND);
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), myBitmap, "", null);
                Uri screenshotUri = Uri.parse(path);
                intent.putExtra(Intent.EXTRA_STREAM, screenshotUri);
                intent.setType("image/*");
                startActivity(Intent.createChooser(intent, "Share image via..."));
                break;
        }
    }
}
