package com.mofunsky.photocropper.library;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

public class CropHelper {
    public static final String TAG = "CropHelper";

    /**
     * request code of Activities or Fragments You will have to change the
     * <p/>
     * values of the request codes below if they conflict with your own.
     */

    public static final int REQUEST_CROP = 127;
    public static final int REQUEST_CAMERA = 128;
    public static final int REQUEST_PIC_KITKAT = 129;
    public static final String CROP_CACHE_FILE_NAME = "crop_cache_file.jpg";

    public static Uri buildUri(String path) {
        return Uri.fromFile(Environment.getExternalStorageDirectory())
                .buildUpon()
                .appendPath(TextUtils.isEmpty(path) ? CROP_CACHE_FILE_NAME : path)
                .build();

    }

    public static void handleResult(CropHandler handler, int requestCode,
                                    int resultCode, Intent data) {
        if (handler == null)
            return;
        if (resultCode == Activity.RESULT_CANCELED) {
            handler.onCropCancel();
        } else if (resultCode == Activity.RESULT_OK) {
            CropParams cropParams = handler.getCropParams();
            Activity context = handler.getContext();
            if (cropParams == null) {
                handler.onCropFailed("CropHandler's params MUST NOT be null!");
                return;
            }
            switch (requestCode) {
                case REQUEST_CROP:
                    handler.onPhotoCropped(cropParams.uri);
                    break;
                case REQUEST_CAMERA:
                    Intent intent = buildCropFromUriIntent(cropParams);
                    if (context != null) {
                        context.startActivityForResult(intent, REQUEST_CROP);
                    } else {
                        handler.onCropFailed("CropHandler's context MUST NOT be null!");
                    }
                    break;

                case REQUEST_PIC_KITKAT:
                    Uri selectedImageUri = data.getData();
                    if (selectedImageUri != null && context != null) {
                        String imagePath = getPath(context, selectedImageUri);
                        Uri uri = Uri.parse("file:///" + imagePath);
                        context.startActivityForResult(buildCropIntent("com.android.camera.action" +
                                ".CROP", cropParams, uri), REQUEST_CROP);
                    } else {
                        handler.onCropFailed("CropHandler's context MUST NOT be null!");
                    }
                    break;
            }
        }
    }

    public static boolean clearCachedCropFile(Uri uri) {
        if (uri == null)
            return false;
        File file = new File(uri.getPath());
        if (file.exists()) {
            boolean result = file.delete();
            if (result)
                Log.i(TAG, "Cached crop file cleared.");
            else
                Log.e(TAG, "Failed to clear cached crop file.");
            return result;
        } else {
            Log.w(TAG,
                    "Trying to clear cached crop file but it does not exist.");
        }
        return false;
    }

    public static Intent buildCropFromUriIntent(CropParams params) {
        return buildCropIntent("com.android.camera.action.CROP", params);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static Intent buildCropFromGalleryIntent(CropParams params) {
        String action;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            action = Intent.ACTION_OPEN_DOCUMENT;
        } else {
            action = Intent.ACTION_GET_CONTENT;
        }
        return buildCropIntent(action, params);
    }

    public static Intent buildCaptureIntent(Uri uri) {
        return new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(
                MediaStore.EXTRA_OUTPUT, uri);
    }

    public static Intent buildCropIntent(String action, CropParams params,
                                         Uri fromUri) {
        Intent intent = new Intent(action, null);
        if (fromUri != null) {
            intent.setDataAndType(fromUri, params.type);
        } else {
            intent.setDataAndType(params.uri, params.type);
        }
        intent.putExtra("crop", params.crop).putExtra("scale", params.scale)
                .putExtra("aspectX", params.aspectX)
                .putExtra("aspectY", params.aspectY)
                .putExtra("outputX", params.outputX)
                .putExtra("outputY", params.outputY)
                .putExtra("return-data", params.returnData)
                .putExtra("outputFormat", params.outputFormat)
                .putExtra("noFaceDetection", params.noFaceDetection)
                .putExtra("scaleUpIfNeeded", params.scaleUpIfNeeded)
                .putExtra(MediaStore.EXTRA_OUTPUT, params.uri);
        return intent;

    }

    public static Intent buildCropIntent(String action, CropParams params) {
        return buildCropIntent(action, params, null);
    }

    public static Bitmap decodeUriAsBitmap(Context context, Uri uri) {
        if (context == null || uri == null)
            return null;
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(context.getContentResolver()
                    .openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    public static int getGalleryCode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            return REQUEST_PIC_KITKAT;
        } else {
            return REQUEST_CROP;
        }
    }

    @SuppressLint("NewApi")
    public static String getPath(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());

    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());

    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());

    }
}
