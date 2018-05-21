package com.byteshaft.screenshotapp;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;

public class DialogActivity extends Activity implements View.OnClickListener {

    private ImageButton mButtonClose;
    private ImageButton mButtonDelete;
    private ImageButton mButtonShare;
    private ImageView mScreenshot;
    private String imagePath = null;

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
        Log.wtf("Image file ", imagePath);

        File imgFile = new File(imagePath);

        if (imgFile.exists()) {
            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
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
                Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show();

                break;
            case R.id.button_share:
                Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
