package com.example.lcom75.multiimageselect;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.lcom75.multiimageselect.customviews.PhotoPickerAlbumsCell;
import com.example.lcom75.multiimageselect.customviews.PhotoPickerSearchCell;

import java.util.ArrayList;

public class MultiImagePicker extends AppCompatActivity implements NotificationCenter.NotificationCenterDelegate {

    int classGuid = 1202;
    private ArrayList<AndroidUtilities.AlbumEntry> albumsSorted = null;
    private ArrayList<AndroidUtilities.AlbumEntry> videoAlbumsSorted = null;
    ListView listView;
    ProgressBar progressView;
    TextView emptyView;
    private int columnsCount = 2;
    private ListAdapter listAdapter;
    private boolean loading = false;
    private ArrayList<AndroidUtilities.SearchImage> recentWebImages = new ArrayList<>();
    private ArrayList<AndroidUtilities.SearchImage> recentGifImages = new ArrayList<>();
    private boolean allowGifs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_image_picker);
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
        listView = (ListView) findViewById(R.id.lvImages);
        listView.setAdapter(listAdapter = new ListAdapter(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.albumsDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.recentImagesDidLoaded);
        NotificationCenter.getInstance().removeObserver(this, NotificationCenter.closeChats);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AndroidUtilities.loadGalleryPhotosAlbums(classGuid);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.albumsDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.recentImagesDidLoaded);
        NotificationCenter.getInstance().addObserver(this, NotificationCenter.closeChats);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.albumsDidLoaded) {
            int guid = (Integer) args[0];
            if (classGuid == guid) {
                albumsSorted = (ArrayList<AndroidUtilities.AlbumEntry>) args[1];
                videoAlbumsSorted = (ArrayList<AndroidUtilities.AlbumEntry>) args[3];
                if (progressView != null) {
                    progressView.setVisibility(View.GONE);
                }
                if (listView != null && listView.getEmptyView() == null) {
                    listView.setEmptyView(emptyView);
                }
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
                loading = false;
            }
        }
    }

    private boolean singlePhoto;
    private int selectedMode;

    private class ListAdapter extends BaseFragmentAdapter {
        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int i) {
            return true;
        }

        @Override
        public int getCount() {
            if (singlePhoto || selectedMode == 0) {
                if (singlePhoto) {
                    return albumsSorted != null ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0;
                }
                return 1 + (albumsSorted != null ? (int) Math.ceil(albumsSorted.size() / (float) columnsCount) : 0);
            } else {
                return (videoAlbumsSorted != null ? (int) Math.ceil(videoAlbumsSorted.size() / (float) columnsCount) : 0);
            }
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
            int type = getItemViewType(i);
            if (type == 0) {
                PhotoPickerAlbumsCell photoPickerAlbumsCell;
                if (view == null) {
                    view = new PhotoPickerAlbumsCell(mContext);
                    photoPickerAlbumsCell = (PhotoPickerAlbumsCell) view;
                    photoPickerAlbumsCell.setDelegate(new PhotoPickerAlbumsCell.PhotoPickerAlbumsCellDelegate() {
                        @Override
                        public void didSelectAlbum(AndroidUtilities.AlbumEntry albumEntry) {
                            openPhotoPicker(albumEntry, 0);
                        }
                    });
                } else {
                    photoPickerAlbumsCell = (PhotoPickerAlbumsCell) view;
                }
                photoPickerAlbumsCell.setAlbumsCount(columnsCount);
                for (int a = 0; a < columnsCount; a++) {
                    int index;
                    if (singlePhoto || selectedMode == 1) {
                        index = i * columnsCount + a;
                    } else {
                        index = (i - 1) * columnsCount + a;
                    }
                    if (singlePhoto || selectedMode == 0) {
                        if (index < albumsSorted.size()) {
                            AndroidUtilities.AlbumEntry albumEntry = albumsSorted.get(index);
                            photoPickerAlbumsCell.setAlbum(a, albumEntry);
                        } else {
                            photoPickerAlbumsCell.setAlbum(a, null);
                        }
                    } else {
                        if (index < videoAlbumsSorted.size()) {
                            AndroidUtilities.AlbumEntry albumEntry = videoAlbumsSorted.get(index);
                            photoPickerAlbumsCell.setAlbum(a, albumEntry);
                        } else {
                            photoPickerAlbumsCell.setAlbum(a, null);
                        }
                    }
                }
                photoPickerAlbumsCell.requestLayout();
            } else if (type == 1) {
                if (view == null) {
                    view = new PhotoPickerSearchCell(mContext, allowGifs);
                    ((PhotoPickerSearchCell) view).setDelegate(new PhotoPickerSearchCell.PhotoPickerSearchCellDelegate() {
                        @Override
                        public void didPressedSearchButton(int index) {
                            openPhotoPicker(null, index);
                        }
                    });
                }
            }
            return view;
        }

        @Override
        public int getItemViewType(int i) {
            if (singlePhoto || selectedMode == 1) {
                return 0;
            }
            if (i == 0) {
                return 1;
            }
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            if (singlePhoto || selectedMode == 1) {
                return 1;
            }
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return getCount() == 0;
        }
    }

    private void openPhotoPicker(AndroidUtilities.AlbumEntry albumEntry, int type) {
        ArrayList<AndroidUtilities.SearchImage> recentImages = null;
        if (albumEntry == null) {
            if (type == 0) {
                recentImages = recentWebImages;
            } else if (type == 1) {
                recentImages = recentGifImages;
            }
        }
//        PhotoPickerActivity fragment = new PhotoPickerActivity(type, albumEntry, selectedPhotos, selectedWebPhotos, recentImages, singlePhoto, chatActivity);
//        fragment.setDelegate(new PhotoPickerActivity.PhotoPickerActivityDelegate() {
//            @Override
//            public void selectedPhotosChanged() {
//                if (pickerBottomLayout != null) {
//                    pickerBottomLayout.updateSelectedCount(selectedPhotos.size() + selectedWebPhotos.size(), true);
//                }
//            }
//
//            @Override
//            public void actionButtonPressed(boolean canceled) {
//                removeSelfFromStack();
//                if (!canceled) {
//                    sendSelectedPhotos();
//                }
//            }
//
//            @Override
//            public boolean didSelectVideo(String path) {
//                removeSelfFromStack();
//                return delegate.didSelectVideo(path);
//            }
//        });
//        presentFragment(fragment);
    }
}
