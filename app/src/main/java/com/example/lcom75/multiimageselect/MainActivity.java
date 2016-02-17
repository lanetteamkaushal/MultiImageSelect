package com.example.lcom75.multiimageselect;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.example.lcom75.multiimageselect.adapter.ImagePreviewAdapter;
import com.example.lcom75.multiimageselect.adapter.ImagePreviewFragment;
import com.example.lcom75.multiimageselect.customviews.ChatAttachView;
import com.example.lcom75.multiimageselect.customviews.PhotoAttachPhotoCell;
import com.example.lcom75.multiimageselect.customviews.PhotoPreviewCell;
import com.example.lcom75.multiimageselect.customviews.PhotoViewer;
import com.example.lcom75.multiimageselect.customviews.ViewProxy;
import com.example.lcom75.multiimageselect.tgnet.TLRPC;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements PhotoViewer.PhotoViewerProvider {

    FrameLayout frameLayout;
    ChatAttachView chatAttachView;
    ViewPager flPreview;
    ImagePreviewAdapter adapter;
    HashMap<Integer, AndroidUtilities.PhotoEntry> selectedPhotos = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        flPreview = (ViewPager) findViewById(R.id.flPreview);
        selectedPhotos = ApplicationLoader.selectedPhotos;
        flPreview.setAdapter(adapter = new ImagePreviewAdapter(getSupportFragmentManager(), this, 1180, selectedPhotos));
        frameLayout = (FrameLayout) findViewById(R.id.chatAttachView);
        chatAttachView = new ChatAttachView(this);
        chatAttachView.init(this);
        frameLayout.addView(chatAttachView);
        startActivity(new Intent(MainActivity.this, MultiImagePicker.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (adapter != null) {
                selectedPhotos = ApplicationLoader.selectedPhotos;
                adapter.replace(selectedPhotos);
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PhotoPreviewCell getCellForIndex(int index) {
        int count = 1;//attachPhotoRecyclerView.getChildCount();
//        for (int a = 0; a < count; a++) {
            View view = ((ImagePreviewFragment) adapter.instantiateItem(flPreview,flPreview.getCurrentItem())).getView();
            if (view instanceof PhotoPreviewCell) {
                PhotoPreviewCell cell = (PhotoPreviewCell) view;
//                int num = (Integer) cell.getImageView().getTag();
//                if (num < 0 || num >= AndroidUtilities.allPhotosAlbumEntry.photos.size()) {
//                    continue;
//                }
//                if (num == index) {
                    return cell;
//                }
//            }
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
        PhotoPreviewCell cell = getCellForIndex(index);
//        if (cell != null) {
//            cell.getCheckBox().setVisibility(View.VISIBLE);
//        }
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
//            cell.getCheckBox().setVisibility(View.GONE);
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
//        int count = attachPhotoRecyclerView.getChildCount();
//        for (int a = 0; a < count; a++) {
//            View view = attachPhotoRecyclerView.getChildAt(a);
//            if (view instanceof PhotoAttachPhotoCell) {
//                PhotoAttachPhotoCell cell = (PhotoAttachPhotoCell) view;
//                if (cell.getCheckBox().getVisibility() != View.VISIBLE) {
//                    cell.getCheckBox().setVisibility(View.VISIBLE);
//                }
//            }
//        }
    }

    @Override
    public boolean isPhotoChecked(int index) {
        return false;//!(index < 0 || index >= AndroidUtilities.allPhotosAlbumEntry.photos.size()) && photoAttachAdapter.getSelectedPhotos().containsKey(AndroidUtilities.allPhotosAlbumEntry.photos.get(index).imageId);
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
}
