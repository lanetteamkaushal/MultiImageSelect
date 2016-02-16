/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package com.example.lcom75.multiimageselect.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;


import com.example.lcom75.multiimageselect.AndroidUtilities;
import com.example.lcom75.multiimageselect.MediaController;
import com.example.lcom75.multiimageselect.customviews.PhotoCell;

import java.util.HashMap;

public class PhotoAttachAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private PhotoAttachAdapterDelegate delegate;
    private HashMap<Integer, AndroidUtilities.PhotoEntry> selectedPhotos = new HashMap<>();

    public interface PhotoAttachAdapterDelegate {
        void selectedPhotosChanged();
    }

    private class Holder extends RecyclerView.ViewHolder {

        public Holder(View itemView) {
            super(itemView);
        }
    }

    public PhotoAttachAdapter(Context context) {
        mContext = context;
    }

    public void clearSelectedPhotos() {
        if (!selectedPhotos.isEmpty()) {
            for (HashMap.Entry<Integer, AndroidUtilities.PhotoEntry> entry : selectedPhotos.entrySet()) {
                AndroidUtilities.PhotoEntry photoEntry = entry.getValue();
                photoEntry.imagePath = null;
                photoEntry.thumbPath = null;
                photoEntry.caption = null;
            }
            selectedPhotos.clear();
            delegate.selectedPhotosChanged();
            notifyDataSetChanged();
        }
    }

    public HashMap<Integer, AndroidUtilities.PhotoEntry> getSelectedPhotos() {
        return selectedPhotos;
    }

    public void setDelegate(PhotoAttachAdapterDelegate photoAttachAdapterDelegate) {
        delegate = photoAttachAdapterDelegate;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        PhotoCell cell = (PhotoCell) holder.itemView;
        AndroidUtilities.PhotoEntry photoEntry = AndroidUtilities.allPhotosAlbumEntry.photos.get(position);
        cell.setPhotoEntry(photoEntry, position == AndroidUtilities.allPhotosAlbumEntry.photos.size() - 1);
        cell.setChecked(selectedPhotos.containsKey(photoEntry.imageId), false);
        cell.getImageView().setTag(position);
        cell.setTag(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        PhotoCell cell = new PhotoCell(mContext, false);
        cell.setDelegate(new PhotoCell.PhotoAttachPhotoCellDelegate() {
            @Override
            public void onCheckClick(PhotoCell v) {
                AndroidUtilities.PhotoEntry photoEntry = v.getPhotoEntry();
                if (selectedPhotos.containsKey(photoEntry.imageId)) {
                    selectedPhotos.remove(photoEntry.imageId);
                    v.setChecked(false, true);
                    photoEntry.imagePath = null;
                    photoEntry.thumbPath = null;
                    v.setPhotoEntry(photoEntry, (Integer) v.getTag() == AndroidUtilities.allPhotosAlbumEntry.photos.size() - 1);
                } else {
                    selectedPhotos.put(photoEntry.imageId, photoEntry);
                    v.setChecked(true, true);
                }
                delegate.selectedPhotosChanged();
            }
        });
        return new Holder(cell);
    }

    @Override
    public int getItemCount() {
        return (AndroidUtilities.allPhotosAlbumEntry != null ? AndroidUtilities.allPhotosAlbumEntry.photos.size() : 0);
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }
}
