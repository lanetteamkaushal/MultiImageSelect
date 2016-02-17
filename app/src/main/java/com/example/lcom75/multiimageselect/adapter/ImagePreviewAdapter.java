package com.example.lcom75.multiimageselect.adapter;

import android.app.FragmentManager;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.ApplicationLoader;
import com.example.lcom75.multiimageselect.BaseFragmentAdapter;
import com.example.lcom75.multiimageselect.R;
import com.example.lcom75.multiimageselect.customviews.BackupImageView;
import com.example.lcom75.multiimageselect.customviews.PhotoPickerPhotoCell;

import java.util.HashMap;

/**
 * Created by lcom75 on 16/2/16.
 */
public class ImagePreviewAdapter extends FragmentStatePagerAdapter {
    private Context mContext;
    Integer[] selectedKey;
    HashMap<Integer, AndroidUtilities.PhotoEntry> selectedPhotos;
    LayoutInflater mLayoutInflater;

    public ImagePreviewAdapter(android.support.v4.app.FragmentManager fm, Context context, HashMap<Integer, AndroidUtilities.PhotoEntry> mSelectedPhotos) {
        super(fm);
        mContext = context;
        selectedPhotos = mSelectedPhotos;
        selectedKey = mSelectedPhotos.keySet().toArray(new Integer[mSelectedPhotos.size()]);
        mLayoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        if (selectedPhotos == null) {
            return 0;
        }
        return selectedPhotos.size();
    }

    @Override
    public Fragment getItem(int position) {
        return ImagePreviewFragment.create(selectedKey[position], selectedPhotos.get(selectedKey[position]));
    }

    public void replace(HashMap<Integer, AndroidUtilities.PhotoEntry> mSelectedPhotos) {
        selectedPhotos = mSelectedPhotos;
        selectedKey = mSelectedPhotos.keySet().toArray(new Integer[mSelectedPhotos.size()]);
    }
}
