package com.dodola.flip.dummy;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import com.dodola.flip.CircularProgressDrawable;
import com.dodola.flip.R;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import android.widget.TextView;

public class ItemDataAdapter extends ArrayAdapter<SimpleData> {


    private Context context;
    private LayoutInflater layoutInflater;

    public ItemDataAdapter(Context context) {
        super(context, -1);
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.item_data, null);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.myImageView = (SimpleDraweeView) convertView.findViewById(R.id.my_image_view);
            viewHolder.myTitle = (TextView) convertView.findViewById(R.id.my_title);
            viewHolder.myContent = (TextView) convertView.findViewById(R.id.my_content);
            GenericDraweeHierarchyBuilder builder =
                    new GenericDraweeHierarchyBuilder(context.getResources());
            GenericDraweeHierarchy hierarchy = builder
                    .setFadeDuration(200)
                    .setPlaceholderImage(new ColorDrawable(0xf2f2f2))
                    .setProgressBarImage(new CircularProgressDrawable())
                    .build();
            viewHolder.myImageView.setHierarchy(hierarchy);
            convertView.setTag(viewHolder);
        }
        initializeViews(getItem(position), (ViewHolder) convertView.getTag());
        return convertView;
    }

    private void initializeViews(SimpleData simpleData, ViewHolder holder) {
        holder.myContent.setText(simpleData.content);
        holder.myTitle.setText(simpleData.title);
        holder.myImageView.setImageURI(Uri.parse(simpleData.picUrl));
    }

    protected class ViewHolder {
        private SimpleDraweeView myImageView;
        private TextView myTitle;
        private TextView myContent;
    }
}
