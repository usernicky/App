package com.leo.appmaster.home;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import com.leo.appmaster.R;
import com.leo.appmaster.imagehide.PhotoItem;
import com.leo.appmaster.ui.MaskImageView;
import com.leo.imageloader.DisplayImageOptions;
import com.leo.imageloader.ImageLoader;
import com.leo.imageloader.core.FadeInBitmapDisplayer;
import com.leo.imageloader.core.ImageScaleType;
import com.leo.imageloader.core.SimpleImageLoadingListener;

/**
 * Created by Jasper on 2015/10/16.
 */
public class PrivacyNewPicAdapter extends PrivacyNewAdaper<PhotoItem> {
    private ImageLoader mImageLoader;

    public PrivacyNewPicAdapter() {
        mImageLoader = ImageLoader.getInstance();
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PrivacyNewHolder holder = null;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.pri_pro_new_pic_item, null);

            holder = new PrivacyNewHolder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.pp_pic_iv);
            holder.checkBox = (CheckBox) convertView.findViewById(R.id.pp_pic_item_cb);

            convertView.setTag(holder);
        } else {
            holder = (PrivacyNewHolder) convertView.getTag();
        }

        final PhotoItem item = (PhotoItem) getItem(position);
        String url = "file://" + item.getPath();

        mImageLoader.displayImage(url, holder.imageView, getMediaOptions());
        boolean isChecked = isChecked(item);
        holder.checkBox.setChecked(isChecked);
        if (holder.imageView instanceof MaskImageView) {
            ((MaskImageView) holder.imageView).setChecked(isChecked);
        }
        holder.checkBox.setClickable(false);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle(item);
            }
        });
        return convertView;
    }

    public DisplayImageOptions getMediaOptions() {
        DisplayImageOptions options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.pic_loading_shape)
                .showImageForEmptyUri(R.drawable.pic_loading_shape)
                .showImageOnFail(R.drawable.pic_loading_shape)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .imageScaleType(ImageScaleType.EXACTLY_STRETCHED)
                .build();

        return options;
    }
}
