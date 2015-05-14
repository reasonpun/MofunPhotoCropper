package com.mofunsky.mofunphotocropper;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.mofunsky.library.BasePhotoCropActivity;
import com.mofunsky.library.CropHelper;
import com.mofunsky.library.CropParams;

/**
 * Created with Android Studio.
 * User: ryan@xisue.com
 * Date: 10/3/14
 * Time: 11:44 AM
 * Desc: TestActivity
 */
public class TestActivity extends BasePhotoCropActivity implements View.OnClickListener {

    public static final String TAG = "TestActivity";

    ImageView mImageView;

    CropParams mCropParams = new CropParams("1234567890.jpg");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);

        mImageView = (ImageView) findViewById(R.id.image);

        findViewById(R.id.bt_capture).setOnClickListener(this);
        findViewById(R.id.bt_gallery).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_capture:
                Intent intent = CropHelper.buildCaptureIntent(mCropParams.uri);
                startActivityForResult(intent, CropHelper.REQUEST_CAMERA);
                break;
            case R.id.bt_gallery:
                startActivityForResult(CropHelper.buildCropFromGalleryIntent(mCropParams),CropHelper.getGalleryCode());
                break;
        }
    }

    @Override
    public CropParams getCropParams() {
        return mCropParams;
    }

    @Override
    public void onPhotoCropped(Uri uri) {
        Log.d(TAG, "Crop Uri in path: " + uri.getPath());
        Toast.makeText(this, "Photo cropped!", Toast.LENGTH_LONG).show();
        mImageView.setImageBitmap(CropHelper.decodeUriAsBitmap(this, mCropParams.uri));
    }

    @Override
    public void onCropCancel() {
        Toast.makeText(this, "Crop canceled!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onCropFailed(String message) {
        Toast.makeText(this, "Crop failed:" + message, Toast.LENGTH_LONG).show();
    }
}
