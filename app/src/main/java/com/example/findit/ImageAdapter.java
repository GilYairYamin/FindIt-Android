package com.example.findit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import java.util.List;

public class ImageAdapter extends ArrayAdapter<ImageData>
{
    private final Context context;
    private final List<ImageData> images;
    private final OnImageClickListener onImageClickListener;

    /**
     * Interface for handling image click events.
     */
    public interface OnImageClickListener
    {
        void onImageClick(String imageUrl);
    }

    /**
     * Constructor for ImageAdapter.
     *
     * @param context The context of the calling activity.
     * @param images The list of ImageData objects to be displayed.
     * @param onImageClickListener The listener for image click events.
     */
    public ImageAdapter(@NonNull Context context, @NonNull List<ImageData> images, OnImageClickListener onImageClickListener)
    {
        super(context, 0, images);
        this.context = context;
        this.images = images;
        this.onImageClickListener = onImageClickListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        }

        ImageData imageData = images.get(position);

        TextView nameTextView = convertView.findViewById(R.id.txtImgNameID);
        TextView locationTextView = convertView.findViewById(R.id.txtImgLocationID);
        TextView dateTextView = convertView.findViewById(R.id.txtImgDateID);
        ImageView imageView = convertView.findViewById(R.id.imageView);

        nameTextView.setText(imageData.getName());
        locationTextView.setText(imageData.getLocation());
        dateTextView.setText(imageData.getCreationDate());

        String imageUrl = imageData.getImageUrl();
        Glide.with(context).load(imageUrl).into(imageView);

        imageView.setOnClickListener(v ->
        {
            if (onImageClickListener != null)
            {
                onImageClickListener.onImageClick(imageUrl);
            }
        });

        return convertView;
    }
}
