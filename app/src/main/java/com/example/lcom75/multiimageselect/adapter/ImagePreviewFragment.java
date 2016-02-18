package com.example.lcom75.multiimageselect.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.NotificationCenter;
import com.example.lcom75.multiimageselect.customviews.PhotoPreviewCell;
import com.example.lcom75.multiimageselect.customviews.PhotoViewer;
import com.example.lcom75.multiimageselect.customviews.PhotoViewerList;

import java.util.ArrayList;

/**
 * Created by lcom75 on 16/2/16.
 */
public class ImagePreviewFragment extends Fragment implements NotificationCenter.NotificationCenterDelegate {
    /**
     * The argument key for the page number this fragment represents.
     */
    public static final String ARG_PAGE = "page";
    public static final String ARG_PHOTOENTRY = "photo_entry";
    AndroidUtilities.PhotoEntry photoEntry = null;
    ArrayList<Object> arrayList = new ArrayList<>();
    /**
     * The fragment's page number, which is set to the argument value for {@link #ARG_PAGE}.
     */
    private int mPageNumber;
    PhotoViewer.PhotoViewerProvider photoViewerProvider;
    PhotoViewerList singleInstace;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        photoViewerProvider = (PhotoViewer.PhotoViewerProvider) context;

    }

    /**
     * Factory method for this fragment class. Constructs a new fragment for the given page number.
     */


    public static ImagePreviewFragment create(int pageNumber, AndroidUtilities.PhotoEntry mphotoEntry) {
        ImagePreviewFragment fragment = new ImagePreviewFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, pageNumber);
        args.putSerializable(ARG_PHOTOENTRY, mphotoEntry);
        fragment.setArguments(args);
        return fragment;
    }

    public ImagePreviewFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageNumber = getArguments().getInt(ARG_PAGE);
        photoEntry = (AndroidUtilities.PhotoEntry) getArguments().getSerializable(ARG_PHOTOENTRY);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout containing a title and body text.
//        ViewGroup view = (ViewGroup) inflater
//                .inflate(R.layout.pager_item, container, false);
        singleInstace = new PhotoViewerList();
        PhotoPreviewCell view = new PhotoPreviewCell(getContext());
        singleInstace.setParentActivity(getActivity());
//        PhotoViewerList.getInstance().setImageIndex(position, false);
//    } else {
        arrayList.add(photoEntry);
        if (view != null) {
            singleInstace.setIsVisible(false);
            singleInstace.openPhotoForSelect(arrayList, 0, 0, photoViewerProvider, getActivity(), view);
        }
//        else
//            PhotoViewerList.getInstance().openPhotoForSelect(arrayList, position, 0, ChatAttachView.this, baseFragment);

//        BackupImageView imageView = view.photoImage;
//        imageView.setSize(1080, 1120);
//        imageView.setTag(mPageNumber);
//        view.setTag(mPageNumber);
//        boolean showing = false;
//        imageView.setOrientation(0, true);
//        if (photoEntry != null) {
//            if (photoEntry.thumbPath != null) {
//                imageView.setImage(photoEntry.thumbPath, null, getContext().getResources().getDrawable(R.drawable.nophotos));
//            } else if (photoEntry.path != null) {
//                imageView.setOrientation(photoEntry.orientation, true);
//                if (photoEntry.isVideo) {
//                    imageView.setImage("vthumb://" + photoEntry.imageId + ":" + photoEntry.path, null, getContext().getResources().getDrawable(R.drawable.nophotos));
//                } else {
////                    imageView.setImage(photoEntry.path, null, getContext().getResources().getDrawable(R.drawable.nophotos));
//                    String path;
//                    if (photoEntry.imagePath != null) {
//                        path = photoEntry.imagePath;
//                    } else {
//                        path = photoEntry.path;
//                    }
//                    int size = (int) (AndroidUtilities.getPhotoSize() / AndroidUtilities.density);
//                    String filter = String.format(Locale.US, "%d_%d", size, size);
//                    imageView.setImage(path, filter, null, null, 0);
//                }
//            } else {
//                imageView.setImageResource(R.drawable.nophotos);
//            }
//        }
//        imageView.getImageReceiver().setVisible(!showing, true);
//            cell.checkBox.setVisibility(View.GONE);
        return view;
    }

    /**
     * Returns the page number represented by this fragment object.
     */
    public int getPageNumber() {
        return mPageNumber;
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
    }

    public void startEditMode() {
        if (singleInstace != null) {
            if (singleInstace.getCurrentEditMode() == 1) {
                singleInstace.applyCurrentEditMode();
            } else {
                singleInstace.switchToEditMode(1);
            }
        }
    }

    public void saveBitmap(Bitmap bitmap) {
        if (singleInstace != null) {
            singleInstace.applyCurrentEditMode(bitmap);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}

