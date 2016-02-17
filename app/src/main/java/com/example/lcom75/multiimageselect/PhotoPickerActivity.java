package com.example.lcom75.multiimageselect;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.example.lcom75.multiimageselect.customviews.BackupImageView;
import com.example.lcom75.multiimageselect.customviews.FileLoader;
import com.example.lcom75.multiimageselect.customviews.PhotoPickerPhotoCell;
import com.example.lcom75.multiimageselect.customviews.PhotoViewer;
import com.example.lcom75.multiimageselect.customviews.ViewProxy;
import com.example.lcom75.multiimageselect.tgnet.TLRPC;
import com.example.lcom75.multiimageselect.volley.RequestQueue;

import java.util.ArrayList;
import java.util.HashMap;

public class PhotoPickerActivity extends AppCompatActivity implements View.OnClickListener, PhotoViewer.PhotoViewerProvider {

    @Override
    public void onClick(View v) {
        if (v == ivBack) {
            onBackPressed();
        } else if (v == tvDone) {
            onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        ApplicationLoader.selectedPhotos = selectedPhotos;
        super.onBackPressed();
    }

    public interface PhotoPickerActivityDelegate {
        void selectedPhotosChanged();

        void actionButtonPressed(boolean canceled);

        boolean didSelectVideo(String path);
    }

    private RequestQueue requestQueue;

    private int type;
    private HashMap<String, AndroidUtilities.SearchImage> selectedWebPhotos;
    private HashMap<Integer, AndroidUtilities.PhotoEntry> selectedPhotos = new HashMap<>();
    private ArrayList<AndroidUtilities.SearchImage> recentImages;

    private ArrayList<AndroidUtilities.SearchImage> searchResult = new ArrayList<>();
    private HashMap<String, AndroidUtilities.SearchImage> searchResultKeys = new HashMap<>();
    private HashMap<String, AndroidUtilities.SearchImage> searchResultUrls = new HashMap<>();

    private boolean searching;
    private String nextSearchBingString;
    private boolean giphySearchEndReached = true;
    private String lastSearchString;
    private boolean loadingRecent;
    private int nextGiphySearchOffset;
    private int giphyReqId;
    private int lastSearchToken;

    private AndroidUtilities.AlbumEntry selectedAlbum;

    private GridView listView;
    private ListAdapter listAdapter;
    //    private PickerBottomLayout pickerBottomLayout;
    private FrameLayout progressView;
    private TextView emptyView;
    //    private ActionBarMenuItem searchItem;
    private int itemWidth = 100;
    private boolean sendPressed;
    private boolean singlePhoto;
//    private ChatActivity chatActivity;

    private PhotoPickerActivityDelegate delegate;
    TextView pickerBottomLayout;
    ImageView ivBack;
    TextView tvAlbumTitle, tvDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_picker);
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
        pickerBottomLayout = (TextView) findViewById(R.id.pickerBottomLayout);
        this.selectedAlbum = ApplicationLoader.albumEntry;
        this.selectedPhotos = ApplicationLoader.selectedPhotos;
        ivBack = (ImageView) findViewById(R.id.ivBack);
        ivBack.setOnClickListener(this);
        tvAlbumTitle = (TextView) findViewById(R.id.tvAlbumTitle);
        tvAlbumTitle.setText(selectedAlbum.bucketName);
        tvDone = (TextView) findViewById(R.id.tvDone);
        tvDone.setOnClickListener(this);
        listView = (GridView) findViewById(R.id.gridView);
        listView.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        listView.setClipToPadding(false);
        listView.setDrawSelectorOnTop(true);
        listView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        listView.setHorizontalScrollBarEnabled(false);
        listView.setVerticalScrollBarEnabled(false);
        listView.setNumColumns(GridView.AUTO_FIT);
        listView.setVerticalSpacing(AndroidUtilities.dp(4));
        listView.setHorizontalSpacing(AndroidUtilities.dp(4));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (selectedAlbum != null && selectedAlbum.isVideo) {
                    if (i < 0 || i >= selectedAlbum.photos.size()) {
                        return;
                    }
                    if (delegate.didSelectVideo(selectedAlbum.photos.get(i).path)) {
//                        finishFragment();
                    }
                } else {
                    ArrayList<Object> arrayList;
                    if (selectedAlbum != null) {
                        arrayList = (ArrayList) selectedAlbum.photos;
                    } else {
                        if (searchResult.isEmpty() && lastSearchString == null) {
                            arrayList = (ArrayList) recentImages;
                        } else {
                            arrayList = (ArrayList) searchResult;
                        }
                    }
                    if (i < 0 || i >= arrayList.size()) {
                        return;
                    }
//                    PhotoViewer.getInstance().setParentActivity(PhotoPickerActivity.this);
//                    PhotoViewer.getInstance().openPhotoForSelect(arrayList, i, singlePhoto ? 1 : 0, PhotoPickerActivity.this, PhotoPickerActivity.this);
                }
            }
        });
        delegate = new PhotoPickerActivityDelegate() {
            @Override
            public void selectedPhotosChanged() {
                if (pickerBottomLayout != null) {
                    pickerBottomLayout.setText(getResources().getQuantityString(R.plurals.selected_images, selectedPhotos.size(), selectedPhotos.size()));
                }
            }

            @Override
            public void actionButtonPressed(boolean canceled) {

            }

            @Override
            public boolean didSelectVideo(String path) {
                return false;
            }
        };

        listView.setAdapter(listAdapter = new ListAdapter(this));
        if (pickerBottomLayout != null) {
            pickerBottomLayout.setText(getResources().getQuantityString(R.plurals.selected_images, selectedPhotos.size(), selectedPhotos.size()));
        }
    }

    public void setDelegate(PhotoPickerActivityDelegate delegate) {
        this.delegate = delegate;
    }

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return selectedAlbum != null;
        }

        @Override
        public boolean isEnabled(int i) {
            if (selectedAlbum == null) {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return i < recentImages.size();
                } else {
                    return i < searchResult.size();
                }
            }
            return true;
        }

        @Override
        public int getCount() {
            if (selectedAlbum == null) {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return recentImages.size();
                } else if (type == 0) {
                    return searchResult.size() + (nextSearchBingString == null ? 0 : 1);
                } else if (type == 1) {
                    return searchResult.size() + (giphySearchEndReached ? 0 : 1);
                }
            }
            return selectedAlbum.photos.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int viewType = getItemViewType(i);
            if (viewType == 0) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                if (view == null) {
                    view = new PhotoPickerPhotoCell(mContext);
                    cell = (PhotoPickerPhotoCell) view;
                    cell.checkFrame.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int index = (Integer) ((View) v.getParent()).getTag();
                            if (selectedAlbum != null) {
                                AndroidUtilities.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                                if (selectedPhotos.containsKey(photoEntry.imageId)) {
                                    selectedPhotos.remove(photoEntry.imageId);
                                    photoEntry.imagePath = null;
                                    photoEntry.thumbPath = null;
                                    updatePhotoAtIndex(index);
                                } else {
                                    selectedPhotos.put(photoEntry.imageId, photoEntry);
                                }
                                ((PhotoPickerPhotoCell) v.getParent()).setChecked(selectedPhotos.containsKey(photoEntry.imageId), true);
                            } else {
//                                AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                                AndroidUtilities.SearchImage photoEntry;
                                if (searchResult.isEmpty() && lastSearchString == null) {
                                    photoEntry = recentImages.get((Integer) ((View) v.getParent()).getTag());
                                } else {
                                    photoEntry = searchResult.get((Integer) ((View) v.getParent()).getTag());
                                }
                                if (selectedWebPhotos.containsKey(photoEntry.id)) {
                                    selectedWebPhotos.remove(photoEntry.id);
                                    photoEntry.imagePath = null;
                                    photoEntry.thumbPath = null;
                                    updatePhotoAtIndex(index);
                                } else {
                                    selectedWebPhotos.put(photoEntry.id, photoEntry);
                                }
                                ((PhotoPickerPhotoCell) v.getParent()).setChecked(selectedWebPhotos.containsKey(photoEntry.id), true);
                            }
//                            pickerBottomLayout.updateSelectedCount(selectedPhotos.size() + selectedWebPhotos.size(), true);
                            delegate.selectedPhotosChanged();
                        }
                    });
                    cell.checkFrame.setVisibility(singlePhoto ? View.GONE : View.VISIBLE);
                }
                cell.itemWidth = itemWidth;
                BackupImageView imageView = ((PhotoPickerPhotoCell) view).photoImage;
                imageView.setTag(i);
                view.setTag(i);
                boolean showing = false;
                imageView.setOrientation(0, true);

                if (selectedAlbum != null) {
                    AndroidUtilities.PhotoEntry photoEntry = selectedAlbum.photos.get(i);
                    if (photoEntry.thumbPath != null) {
                        imageView.setImage(photoEntry.thumbPath, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.path != null) {
                        imageView.setOrientation(photoEntry.orientation, true);
                        if (photoEntry.isVideo) {
                            imageView.setImage("vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                        } else {
                            imageView.setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                        }
                    } else {
                        imageView.setImageResource(R.drawable.nophotos);
                    }
                    cell.setChecked(selectedPhotos.containsKey(photoEntry.imageId), false);
//                    showing = PhotoViewer.getInstance().isShowingImage(photoEntry.path);
                } else {
                    AndroidUtilities.SearchImage photoEntry;
                    if (searchResult.isEmpty() && lastSearchString == null) {
                        photoEntry = recentImages.get(i);
                    } else {
                        photoEntry = searchResult.get(i);
                    }
                    if (photoEntry.thumbPath != null) {
                        imageView.setImage(photoEntry.thumbPath, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.thumbUrl != null && photoEntry.thumbUrl.length() > 0) {
                        imageView.setImage(photoEntry.thumbUrl, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else if (photoEntry.document != null && photoEntry.document.thumb != null) {
                        imageView.setImage(photoEntry.document.thumb.location, null, mContext.getResources().getDrawable(R.drawable.nophotos));
                    } else {
                        imageView.setImageResource(R.drawable.nophotos);
                    }
                    cell.setChecked(selectedWebPhotos.containsKey(photoEntry.id), false);
                    if (photoEntry.document != null) {
//                        showing = PhotoViewer.getInstance().isShowingImage(FileLoader.getPathToAttach(photoEntry.document, true).getAbsolutePath());
                    } else {
//                        showing = PhotoViewer.getInstance().isShowingImage(photoEntry.imageUrl);
                    }
                }
                imageView.getImageReceiver().setVisible(!showing, true);
                cell.checkBox.setVisibility(singlePhoto || showing ? View.GONE : View.VISIBLE);
            } else if (viewType == 1) {
                if (view == null) {
                    LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = li.inflate(R.layout.media_loading_layout, viewGroup, false);
                }
                ViewGroup.LayoutParams params = view.getLayoutParams();
                params.width = itemWidth;
                params.height = itemWidth;
                view.setLayoutParams(params);
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (selectedAlbum != null || searchResult.isEmpty() && lastSearchString == null && i < recentImages.size() || i < searchResult.size()) {
                return 0;
            }
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            if (selectedAlbum != null) {
                return selectedAlbum.photos.isEmpty();
            } else {
                if (searchResult.isEmpty() && lastSearchString == null) {
                    return recentImages.isEmpty();
                } else {
                    return searchResult.isEmpty();
                }
            }
        }
    }

    private PhotoPickerPhotoCell getCellForIndex(int index) {
        int count = listView.getChildCount();

        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            if (view instanceof PhotoPickerPhotoCell) {
                PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
                int num = (Integer) cell.photoImage.getTag();
                if (selectedAlbum != null) {
                    if (num < 0 || num >= selectedAlbum.photos.size()) {
                        continue;
                    }
                } else {
                    ArrayList<AndroidUtilities.SearchImage> array;
                    if (searchResult.isEmpty() && lastSearchString == null) {
                        array = recentImages;
                    } else {
                        array = searchResult;
                    }
                    if (num < 0 || num >= array.size()) {
                        continue;
                    }
                }
                if (num == index) {
                    return cell;
                }
            }
        }
        return null;
    }


    @Override
    public PhotoViewer.PlaceProviderObject getPlaceForPhoto(Object messageObject, TLRPC.FileLocation fileLocation, int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            int coords[] = new int[2];
            cell.photoImage.getLocationInWindow(coords);
            PhotoViewer.PlaceProviderObject object = new PhotoViewer.PlaceProviderObject();
            object.viewX = coords[0];
            object.viewY = coords[1] - AndroidUtilities.statusBarHeight;
            if (Build.VERSION.SDK_INT < 11) {
                float scale = ViewProxy.getScaleX(cell.photoImage);
                if (scale != 1) {
                    int width = cell.photoImage.getMeasuredWidth();
                    object.viewX += (width - width * scale) / 2;
                    object.viewY += (width - width * scale) / 2;
                }
            }
            object.parentView = listView;
            object.imageReceiver = cell.photoImage.getImageReceiver();
            object.thumb = object.imageReceiver.getBitmap();
            object.scale = ViewProxy.getScaleX(cell.photoImage);
            cell.checkBox.setVisibility(View.GONE);
            return object;
        }
        return null;
    }

    @Override
    public Bitmap getThumbForPhoto(Object messageObject, TLRPC.FileLocation fileLocation, int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            return cell.photoImage.getImageReceiver().getBitmap();
        }
        return null;
    }

    @Override
    public void willSwitchFromPhoto(Object messageObject, TLRPC.FileLocation fileLocation, int index) {
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            if (view.getTag() == null) {
                continue;
            }
            PhotoPickerPhotoCell cell = (PhotoPickerPhotoCell) view;
            int num = (Integer) view.getTag();
            if (selectedAlbum != null) {
                if (num < 0 || num >= selectedAlbum.photos.size()) {
                    continue;
                }
            } else {
                ArrayList<AndroidUtilities.SearchImage> array;
                if (searchResult.isEmpty() && lastSearchString == null) {
                    array = recentImages;
                } else {
                    array = searchResult;
                }
                if (num < 0 || num >= array.size()) {
                    continue;
                }
            }
            if (num == index) {
                cell.checkBox.setVisibility(View.VISIBLE);
                break;
            }
        }
    }

    @Override
    public void willHidePhotoViewer() {
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean isPhotoChecked(int index) {
        if (selectedAlbum != null) {
            return !(index < 0 || index >= selectedAlbum.photos.size()) && selectedPhotos.containsKey(selectedAlbum.photos.get(index).imageId);
        } else {
            ArrayList<AndroidUtilities.SearchImage> array;
            if (searchResult.isEmpty() && lastSearchString == null) {
                array = recentImages;
            } else {
                array = searchResult;
            }
            return !(index < 0 || index >= array.size()) && selectedWebPhotos.containsKey(array.get(index).id);
        }
    }

    @Override
    public void setPhotoChecked(int index) {
        boolean add = true;
        if (selectedAlbum != null) {
            if (index < 0 || index >= selectedAlbum.photos.size()) {
                return;
            }
            AndroidUtilities.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
            if (selectedPhotos.containsKey(photoEntry.imageId)) {
                selectedPhotos.remove(photoEntry.imageId);
                add = false;
            } else {
                selectedPhotos.put(photoEntry.imageId, photoEntry);
            }
        } else {
            AndroidUtilities.SearchImage photoEntry;
            ArrayList<AndroidUtilities.SearchImage> array;
            if (searchResult.isEmpty() && lastSearchString == null) {
                array = recentImages;
            } else {
                array = searchResult;
            }
            if (index < 0 || index >= array.size()) {
                return;
            }
            photoEntry = array.get(index);
            if (selectedWebPhotos.containsKey(photoEntry.id)) {
                selectedWebPhotos.remove(photoEntry.id);
                add = false;
            } else {
                selectedWebPhotos.put(photoEntry.id, photoEntry);
            }
        }
        int count = listView.getChildCount();
        for (int a = 0; a < count; a++) {
            View view = listView.getChildAt(a);
            int num = (Integer) view.getTag();
            if (num == index) {
                ((PhotoPickerPhotoCell) view).setChecked(add, false);
                break;
            }
        }
        pickerBottomLayout.setText(getResources().getQuantityString(R.plurals.selected_images, selectedPhotos.size(), selectedPhotos.size()));
        delegate.selectedPhotosChanged();
    }

    @Override
    public boolean cancelButtonPressed() {
        delegate.actionButtonPressed(true);

        return true;
    }

    @Override
    public void sendButtonPressed(int index) {
        if (selectedAlbum != null) {
            if (selectedPhotos.isEmpty()) {
                if (index < 0 || index >= selectedAlbum.photos.size()) {
                    return;
                }
                AndroidUtilities.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                selectedPhotos.put(photoEntry.imageId, photoEntry);
            }
        } else if (selectedPhotos.isEmpty()) {
            ArrayList<AndroidUtilities.SearchImage> array;
            if (searchResult.isEmpty() && lastSearchString == null) {
                array = recentImages;
            } else {
                array = searchResult;
            }
            if (index < 0 || index >= array.size()) {
                return;
            }
            AndroidUtilities.SearchImage photoEntry = array.get(index);
            selectedWebPhotos.put(photoEntry.id, photoEntry);
        }
//        sendSelectedPhotos();
    }

    @Override
    public int getSelectedCount() {
        return selectedPhotos.size() + selectedWebPhotos.size();
    }

    public void updatePhotoAtIndex(int index) {
        PhotoPickerPhotoCell cell = getCellForIndex(index);
        if (cell != null) {
            if (selectedAlbum != null) {
                cell.photoImage.setOrientation(0, true);
                AndroidUtilities.PhotoEntry photoEntry = selectedAlbum.photos.get(index);
                if (photoEntry.thumbPath != null) {
                    cell.photoImage.setImage(photoEntry.thumbPath, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.path != null) {
                    cell.photoImage.setOrientation(photoEntry.orientation, true);
                    if (photoEntry.isVideo) {
                        cell.photoImage.setImage("vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                    } else {
                        cell.photoImage.setImage("thumb://" + photoEntry.imageId + ":" + photoEntry.path, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                    }
                } else {
                    cell.photoImage.setImageResource(R.drawable.nophotos);
                }
            } else {
                ArrayList<AndroidUtilities.SearchImage> array;
                if (searchResult.isEmpty() && lastSearchString == null) {
                    array = recentImages;
                } else {
                    array = searchResult;
                }
                AndroidUtilities.SearchImage photoEntry = array.get(index);
                if (photoEntry.document != null && photoEntry.document.thumb != null) {
                    cell.photoImage.setImage(photoEntry.document.thumb.location, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.thumbPath != null) {
                    cell.photoImage.setImage(photoEntry.thumbPath, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else if (photoEntry.thumbUrl != null && photoEntry.thumbUrl.length() > 0) {
                    cell.photoImage.setImage(photoEntry.thumbUrl, null, cell.getContext().getResources().getDrawable(R.drawable.nophotos));
                } else {
                    cell.photoImage.setImageResource(R.drawable.nophotos);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fixLayout();
    }

    private void fixLayout() {
        if (listView != null) {
            ViewTreeObserver obs = listView.getViewTreeObserver();
            obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    fixLayoutInternal();
                    if (listView != null) {
                        listView.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    return true;
                }
            });
        }
    }

    private void fixLayoutInternal() {
        int position = listView.getFirstVisiblePosition();
        WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Activity.WINDOW_SERVICE);
        int rotation = manager.getDefaultDisplay().getRotation();
        int columnsCount;
        if (AndroidUtilities.isTablet()) {
            columnsCount = 3;
        } else {
            if (rotation == Surface.ROTATION_270 || rotation == Surface.ROTATION_90) {
                columnsCount = 5;
            } else {
                columnsCount = 3;
            }
        }
        listView.setNumColumns(columnsCount);
        if (AndroidUtilities.isTablet()) {
            itemWidth = (AndroidUtilities.dp(490) - ((columnsCount + 1) * AndroidUtilities.dp(4))) / columnsCount;
        } else {
            itemWidth = (AndroidUtilities.displaySize.x - ((columnsCount + 1) * AndroidUtilities.dp(4))) / columnsCount;
        }
        listView.setColumnWidth(itemWidth);
        listAdapter.notifyDataSetChanged();
        listView.setSelection(position);
    }
}
