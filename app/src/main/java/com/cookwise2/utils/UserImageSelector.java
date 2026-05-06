package com.cookwise2.utils;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cookwise2.R;

import java.io.File;

public class UserImageSelector {

    private AppCompatActivity activity;
    private ImageView imageView;
    private Uri imageUri;
    private Bitmap imageBitmap;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private static final String TAG = "UserImageSelector";

    // --- הוספת ה-Listener ---
    private ImageSelectionListener listener;

    // --- ממשק ה-Callback ---
    public interface ImageSelectionListener {
        void onImageSelected(Uri uri);
    }


    public UserImageSelector(AppCompatActivity activity, ImageView imageView){
        this.activity = activity;
        this.imageView = imageView;
        this.imageUri = null;
        this.imageBitmap = null;
        initResultLaunchers();
    }

    public void setOnImageSelectedListener(ImageSelectionListener listener) {
        this.listener = listener;
    }


    public void showImageSourceDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(activity);

        android.view.View view = activity.getLayoutInflater().inflate(R.layout.dialog_select_image_source, null);

        // טיפול בלחיצה על מצלמה
        view.findViewById(R.id.btnCamera).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Log.d(TAG, "User chose to take a photo");
            openCamera();
        });

        // טיפול בלחיצה על גלריה
        view.findViewById(R.id.btnGallery).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            Log.d(TAG, "User chose to pick from gallery");
            openImagePicker();
        });

        bottomSheetDialog.setContentView(view);
        bottomSheetDialog.show();
    }
    private void openCamera() {
        Log.d(TAG, "openCamera: start");
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
        Log.d(TAG, "openCamera: end");
    }

    private void openImagePicker() {
        Log.d(TAG, "openImagePicker: start");
        pickMedia.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
        Log.d(TAG, "openImagePicker: end");
    }

    private void initResultLaunchers()
    {
        Log.d(TAG, "InitResultLaunchers: start");
        pickMedia =
                activity.registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) {
                        Log.d(TAG, "PhotoPicker: Selected URI: " + uri);
                        this.imageUri = uri;
                        if (imageView != null) {
                            imageView.setImageURI(uri);
                        }
                        // --- קריאה ל-Listener עם התוצאה ---
                        if (listener != null) {
                            listener.onImageSelected(uri);
                        }
                    } else {
                        Log.d(TAG, "PhotoPicker: No media selected");
                    }
                });

        cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Got a camera result");
                    if (result.getResultCode() == RESULT_OK) {
                        // הגישה הישנה לצילום מחזירה Bitmap. כדי לקבל URI נצטרך לשמור את התמונה קודם.
                        // לצורך העניין נשאיר את זה כרגע ככה, זה לא רלוונטי לקריסה שלך.
                        // אם תרצה לתקן את זה בעתיד, תצטרך לשמור את ה-Bitmap לקובץ ולקבל את ה-URI שלו.
                        Log.d(TAG, "Camera result code is ok");
                        Intent data = result.getData();
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        if (bitmap != null) {
                            Log.d(TAG, "setting bitmap");
                            if (imageView != null) {
                                imageView.setImageBitmap(bitmap);
                            }
                            this.imageBitmap = bitmap;
                            // חסר כאן URI. כרגע ה-Listener לא יקרא אחרי צילום.
                        } else {
                            Log.e(TAG, "Error retrieving image from camera intent");
                        }
                    } else {
                        Log.d(TAG, "Invalid code returned from camera intent");
                    }
                }
        );
        Log.d(TAG, "InitResultLaunchers: done");
    }

    public File createImageFile()
    {
        if(imageUri != null)
        {
            Log.d(TAG, "createImageFile: creating image from uri");
            return ImageFileCreator.createTempFileFromUri(imageUri, activity);
        }
        else if(imageBitmap != null)
        {
            Log.d(TAG, "createImageFile: creating image from bitmap");
            return ImageFileCreator.createTempFileFromBitmap(imageBitmap, activity);
        }

        Log.w(TAG, "createImageFile: source is not defined");
        return null;
    }

    public Uri getImageUri() {
        return imageUri;
    }
}
