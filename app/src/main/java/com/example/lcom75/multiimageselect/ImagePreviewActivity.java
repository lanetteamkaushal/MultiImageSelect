package com.example.lcom75.multiimageselect;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.andexert.library.RippleView;
import com.example.lcom75.multiimageselect.adapter.ImagePreviewAdapter;
import com.example.lcom75.multiimageselect.adapter.ImagePreviewFragment;
import com.example.lcom75.multiimageselect.customviews.ChatAttachView;
import com.example.lcom75.multiimageselect.customviews.PhotoPreviewCell;
import com.example.lcom75.multiimageselect.customviews.PhotoViewer;
import com.example.lcom75.multiimageselect.customviews.ViewProxy;
import com.example.lcom75.multiimageselect.tgnet.TLRPC;

import java.util.HashMap;

import iamxam.crop.cropwindow.CropImageView;

public class ImagePreviewActivity extends AppCompatActivity implements PhotoViewer.PhotoViewerProvider, View.OnClickListener {

    FrameLayout frameLayout;
    ChatAttachView chatAttachView;
    ViewPager flPreview;
    ImagePreviewAdapter adapter;
    HashMap<Integer, AndroidUtilities.PhotoEntry> selectedPhotos = new HashMap<>();
    private String TAG = ImagePreviewActivity.class.getSimpleName();
    ImagePreviewFragment currentFragment = null;
    private static final int DEFAULT_ASPECT_RATIO_VALUES = 10;
    private static final int ROTATE_NINETY_DEGREES = 90;
    private static final String ASPECT_RATIO_X = "ASPECT_RATIO_X";
    private static final String ASPECT_RATIO_Y = "ASPECT_RATIO_Y";
    public static String IMAGE_PATH = "image-path";
    private static final int ON_TOUCH = 1;
    private int mAspectRatioX = DEFAULT_ASPECT_RATIO_VALUES;
    private int mAspectRatioY = DEFAULT_ASPECT_RATIO_VALUES;
    private ContentResolver mContentResolver;
    private Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.JPEG;
    RippleView rlcancel, rotateLeft, rlSave, rlcrop, rlrotateUndo;
    Bitmap croppedImage;
    Uri _uri;
    CropImageView cropImageView;
    ImageView croppedImageView;
    boolean is_crop_click = false, isInCropMode = false;
    LinearLayout myimagenon, myimageedit, flCropView;
    ImageView ivSend;

    private void bindCropView() {

        mContentResolver = getContentResolver();
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
        ivSend = (ImageView) findViewById(R.id.rotateRight);
        rlcancel.setOnClickListener(this);
        rlcrop.setOnClickListener(this);
        rotateLeft.setOnClickListener(this);
        rlSave.setOnClickListener(this);
        rlrotateUndo.setOnClickListener(this);
        cropImageView.setGuidelines(1);
        myimagenon.setVisibility(View.GONE);
        myimageedit.setVisibility(View.VISIBLE);
        cropImageView.hideLayout(true);
    }

    private void saveOutput(Bitmap croppedImage, Uri mSaveUri) {
        cancelCropMode();
        try {
            if (currentFragment != null) {
                currentFragment.saveBitmap(croppedImage);
            } else {
                ImagePreviewFragment imagePreviewFragment = (ImagePreviewFragment) adapter.instantiateItem(flPreview, flPreview.getCurrentItem());
                if (imagePreviewFragment != null) {
                    imagePreviewFragment.saveBitmap(croppedImage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cancelCropMode() {
        isInCropMode = false;
        flCropView.setVisibility(View.GONE);
        flPreview.setVisibility(View.VISIBLE);
        ivSend.setImageResource(R.drawable.ic_keyboard_arrow_right_white_24dp);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        bindViews();
        bindCropView();
        flPreview = (ViewPager) findViewById(R.id.flPreview);
        flPreview.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                try {
                    currentFragment = (ImagePreviewFragment) adapter.instantiateItem(flPreview, flPreview.getCurrentItem());
                    if (chatAttachView != null) {
                        chatAttachView.selectItemAtPosition(position);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        flCropView = (LinearLayout) findViewById(R.id.flCropView);
        selectedPhotos = ApplicationLoader.selectedPhotos;
        flPreview.setAdapter(adapter = new ImagePreviewAdapter(getSupportFragmentManager(), this, selectedPhotos));
        frameLayout = (FrameLayout) findViewById(R.id.chatAttachView);
        chatAttachView = new ChatAttachView(this);
        chatAttachView.init(this);
        chatAttachView.setDelegate(new ChatAttachView.ChatAttachViewDelegate() {
            @Override
            public void didPressedButton(int button) {

            }

            @Override
            public void didItemClicked(int position) {
                if (adapter.getCount() > position) {
                    flPreview.setCurrentItem(position, true);
                }
            }
        });
        frameLayout.addView(chatAttachView);
        startActivity(new Intent(ImagePreviewActivity.this, MultiImagePicker.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            selectedPhotos = ApplicationLoader.selectedPhotos;
            if (adapter != null) {
                adapter.replace(selectedPhotos);
                adapter.notifyDataSetChanged();
            }
            if (chatAttachView != null) {
                chatAttachView.replace(selectedPhotos);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PhotoPreviewCell getCellForIndex(int index) {
        View view = ((ImagePreviewFragment) adapter.instantiateItem(flPreview, flPreview.getCurrentItem())).getView();
        if (view instanceof PhotoPreviewCell) {
            return (PhotoPreviewCell) view;
        }
        return null;
    }

    @Override
    public void updatePhotoAtIndex(int index) {
        PhotoPreviewCell cell = getCellForIndex(index);
        if (cell != null) {
            cell.getImageView().setOrientation(0, true);
            AndroidUtilities.PhotoEntry photoEntry = AndroidUtilities.allPhotosAlbumEntry.photos.get(index);
            if (photoEntry.thumbPath != null) {
                cell.getImageView().setImage(photoEntry.thumbPath, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
            } else if (photoEntry.path != null) {
                cell.getImageView().setOrientation(photoEntry.orientation, true);
                cell.getImageView().setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
            } else {
                cell.getImageView().setImageResource(R.drawable.nophotos);
            }
        }
    }

    @Override
    public void willSwitchFromPhoto(Object messageObject, TLRPC.FileLocation fileLocation, int index) {

    }

    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(Object messageObject, TLRPC.FileLocation fileLocation, int index) {
        PhotoPreviewCell cell = getCellForIndex(index);
        if (cell != null) {
            int coords[] = new int[2];
            cell.getImageView().getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1] - (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
            object.parentView = flPreview;
            object.imageReceiver = cell.getImageView().getImageReceiver();
            object.thumb = object.imageReceiver.getBitmap();
            object.scale = ViewProxy.getScaleX(cell.getImageView());
            object.clipBottomAddition = (Build.VERSION.SDK_INT >= 21 ? 0 : -AndroidUtilities.statusBarHeight);
            return object;
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(Object messageObject, TLRPC.FileLocation fileLocation, int index) {
        PhotoPreviewCell cell = getCellForIndex(index);
        if (cell != null) {
            return cell.getImageView().getImageReceiver().getBitmap();
        }
        return null;
    }

    @Override
    public void willHidePhotoViewer() {
    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;
    }

    @Override
    public void setPhotoChecked(int index) {

    }

    @Override
    public boolean cancelButtonPressed() {
        return false;
    }

    @Override
    public void sendButtonPressed(int index) {

    }

    @Override
    public int getSelectedCount() {
        return 1;
    }

    @Override
    public void onClick(View v) {
        if (v == rlcancel) {
            cancelCropMode();
        } else if (v == rlcrop) {
            if (!isInCropMode) {
                isInCropMode = true;
                ivSend.setImageResource(R.drawable.ic_check_white_24dp);
                try {
                    if (currentFragment != null) {
                        int pageNo = currentFragment.getPageNumber();
                        startEditMode(pageNo);
                    } else {
                        ImagePreviewFragment imagePreviewFragment = (ImagePreviewFragment) adapter.instantiateItem(flPreview, flPreview.getCurrentItem());
                        if (imagePreviewFragment != null) {
                            int pageNo = imagePreviewFragment.getPageNumber();
                            startEditMode(pageNo);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (is_crop_click) {
                    croppedImage = cropImageView.getCroppedImage();
                    saveOutput(croppedImage, _uri);
                } else {
                    is_crop_click = true;
                    myimagenon.setVisibility(View.GONE);
                    myimageedit.setVisibility(View.VISIBLE);
                    cropImageView.hideLayout(false);
                    cropImageView.setImageResourceURL(Uri.parse(IMAGE_PATH), ImagePreviewActivity.this);
                    cropImageView.setAspectRatio(DEFAULT_ASPECT_RATIO_VALUES, DEFAULT_ASPECT_RATIO_VALUES);
                }
            }
        } else if (v == rotateLeft) {
            if (is_crop_click)
                cropImageView.rotateImage(ROTATE_NINETY_DEGREES);
        } else if (v == rlSave) {
            if (isInCropMode) {
                if (is_crop_click) {
                    Log.d("TAAGG", "rlSave with crop-->");
                    croppedImage = cropImageView.getCroppedImage();
                    saveOutput(croppedImage, _uri);
                } else {
                    Log.e("TAAGG", "rlSave with crop-->");
                    cancelCropMode();
                }
            }
        } else if (v == rlrotateUndo) {
            if (is_crop_click) {
                cropImageView.undoRotateImage();
            }
        }
    }

    private void startEditMode(int pageNo) {
        try {
            AndroidUtilities.PhotoEntry photoEntry = selectedPhotos.get(pageNo);
            Log.d(TAG, "Photo Entry :" + photoEntry.imagePath + ":" + photoEntry.path);
            flPreview.setVisibility(View.GONE);
            flCropView.setVisibility(View.VISIBLE);
            if (photoEntry != null) {
                if (!TextUtils.isEmpty(photoEntry.thumbPath)) {
                    IMAGE_PATH = photoEntry.thumbPath;
                } else if (!TextUtils.isEmpty(photoEntry.imagePath)) {
                    IMAGE_PATH = photoEntry.imagePath;
                } else if (!TextUtils.isEmpty(photoEntry.path)) {
                    IMAGE_PATH = photoEntry.path;
                }
                _uri = getImageContentUri(ImagePreviewActivity.this, IMAGE_PATH);
                cropImageView.setImageResourceURL(Uri.parse(IMAGE_PATH), ImagePreviewActivity.this);
                cropImageView.setAspectRatio(DEFAULT_ASPECT_RATIO_VALUES, DEFAULT_ASPECT_RATIO_VALUES);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
