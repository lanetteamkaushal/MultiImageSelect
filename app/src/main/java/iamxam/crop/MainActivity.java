
package iamxam.crop;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.andexert.library.RippleView;
import com.example.lcom75.multiimageselect.R;
import com.example.lcom75.multiimageselect.Utilities;

import java.io.IOException;
import java.io.OutputStream;

import iamxam.crop.cropwindow.CropImageView;

public class MainActivity extends Activity implements View.OnClickListener {

    // Static final constants
    private static final int DEFAULT_ASPECT_RATIO_VALUES = 10;
    private static final int ROTATE_NINETY_DEGREES = 90;
    private static final String ASPECT_RATIO_X = "ASPECT_RATIO_X";
    private static final String ASPECT_RATIO_Y = "ASPECT_RATIO_Y";
    public static String IMAGE_PATH = "image-path";
    private static final int ON_TOUCH = 1;

    // Instance variables
    private int mAspectRatioX = DEFAULT_ASPECT_RATIO_VALUES;
    private int mAspectRatioY = DEFAULT_ASPECT_RATIO_VALUES;
    private ContentResolver mContentResolver;
    private Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.JPEG;
    RippleView rlcancel, rotateLeft, rlSave, rlcrop, rlrotateUndo;
    Bitmap croppedImage;
    Uri _uri;
    CropImageView cropImageView;
    ImageView croppedImageView;
    boolean is_crop_click = false;
    LinearLayout myimagenon, myimageedit;
    private String TAG = MainActivity.class.getSimpleName();

    // Saves the state upon rotating the screen/restarting the activity
    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt(ASPECT_RATIO_X, mAspectRatioX);
        bundle.putInt(ASPECT_RATIO_Y, mAspectRatioY);
    }

    // Restores the state upon rotating the screen/restarting the activity
    @Override
    protected void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        mAspectRatioX = bundle.getInt(ASPECT_RATIO_X);
        mAspectRatioY = bundle.getInt(ASPECT_RATIO_Y);
    }

    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_crop_main);
        if (Build.VERSION.SDK_INT > 20) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(R.color.black));
        }
        mContentResolver = getContentResolver();
        _uri = getImageContentUri(MainActivity.this, IMAGE_PATH);
        cropImageView = (CropImageView) findViewById(R.id.CropImageView);
        myimagenon = (LinearLayout) findViewById(R.id.myimagenon);
        myimageedit = (LinearLayout) findViewById(R.id.myimageedit);
        Log.e(TAG, "setImageResourceURL > 1 > " + IMAGE_PATH);
        croppedImageView = (ImageView) findViewById(R.id.croppedImageView);
        croppedImageView.setImageURI(Uri.parse(IMAGE_PATH));
        Log.d(TAG, "Image set");
        rlcancel = (RippleView) findViewById(R.id.rlcancel);
        rlcrop = (RippleView) findViewById(R.id.rlcrop);
        rotateLeft = (RippleView) findViewById(R.id.rlrotateleft);
        rlSave = (RippleView) findViewById(R.id.rlrotateright);
        rlrotateUndo = (RippleView) findViewById(R.id.rlrotateUndo);
        rlcancel.setOnClickListener(this);
        rlcrop.setOnClickListener(this);
        rotateLeft.setOnClickListener(this);
        rlSave.setOnClickListener(this);
        rlrotateUndo.setOnClickListener(this);
        cropImageView.setGuidelines(1);
        myimagenon.setVisibility(View.GONE);
        myimageedit.setVisibility(View.VISIBLE);
        cropImageView.hideLayout(true);
        cropImageView.setImageResourceURL(Uri.parse(IMAGE_PATH), MainActivity.this);
        cropImageView.setAspectRatio(DEFAULT_ASPECT_RATIO_VALUES, DEFAULT_ASPECT_RATIO_VALUES);
//        cropImageView.setFixedAspectRatio(true);
    }

    public void setFont(ViewGroup group, Typeface font) {
        int count = group.getChildCount();
        View v;
        for (int i = 0; i < count; i++) {
            v = group.getChildAt(i);
            if (v instanceof TextView || v instanceof EditText || v instanceof Button) {
                ((TextView) v).setTypeface(font);
            } else if (v instanceof ViewGroup)
                setFont((ViewGroup) v, font);
        }
    }

    private void saveOutput(Bitmap croppedImage, Uri mSaveUri) {

        if (mSaveUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = mContentResolver.openOutputStream(mSaveUri);
                if (outputStream != null) {
                    croppedImage.compress(mOutputFormat, 90, outputStream);
                }
                setResult(RESULT_OK);
                finish();
            } catch (IOException ex) {
                Log.e("TAAGGG", "Cannot open file: " + ex.getMessage());
                setResult(RESULT_CANCELED);
                finish();
            } catch (Exception e) {
                Log.e("TAAGGG", "Cannot open file: " + e.getMessage());
                setResult(RESULT_CANCELED);
                finish();
            } finally {
                Utilities.closeSilently(outputStream);
            }
        } else {
            Log.e("TAAGG", "not defined image url");
            setResult(RESULT_CANCELED);
            finish();
        }
    }


    public Uri getImageContentUri(Context context, String filePath) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DATA, filePath);
            return context.getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == rlcancel) {
            setResult(Activity.RESULT_CANCELED);
            finish();
        }
        if (v == rlcrop) {
            if (is_crop_click) {
                croppedImage = cropImageView.getCroppedImage();
                saveOutput(croppedImage, _uri);
            } else {
                is_crop_click = true;
                myimagenon.setVisibility(View.GONE);
                myimageedit.setVisibility(View.VISIBLE);
                cropImageView.hideLayout(false);
                cropImageView.setImageResourceURL(Uri.parse(IMAGE_PATH), MainActivity.this);
                cropImageView.setAspectRatio(DEFAULT_ASPECT_RATIO_VALUES, DEFAULT_ASPECT_RATIO_VALUES);
            }
        }
        if (v == rotateLeft) {
            if (is_crop_click)
                cropImageView.rotateImage(ROTATE_NINETY_DEGREES);
        }
        if (v == rlSave) {
            if (is_crop_click) {
                Log.e("TAAGG", "rlSave with crop-->");
                croppedImage = cropImageView.getCroppedImage();
                saveOutput(croppedImage, _uri);
            } else {
                Log.e("TAAGG", "rlSave with crop-->");
                setResult(RESULT_OK);
                finish();
            }
        }
        if (v == rlrotateUndo) {
            if (is_crop_click) {
                cropImageView.undoRotateImage();
            }
        }
    }
}
