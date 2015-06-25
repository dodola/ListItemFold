package com.dodola.flip.dummy;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dodola.flip.CircularProgressDrawable;
import com.dodola.flip.R;
import com.dodola.flip.RecyclerFragment;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dodola on 15/6/25.
 */
public class RecyclerDataAdapter extends RecyclerView.Adapter<RecyclerDataAdapter.ViewHolder> {

    private LayoutInflater mLayoutInflater;
    private ArrayList<SimpleData> mDatas;
    private Context mContext;
    private IOnRecyclerItemClick mOnItemClick;

    public void setOnItemClick(IOnRecyclerItemClick onItemClick) {
        mOnItemClick = onItemClick;
    }

    public interface IOnRecyclerItemClick {
        void onItemClick(SimpleData data, View view);
    }


    public RecyclerDataAdapter(Context context) {
        mLayoutInflater = LayoutInflater.from(context);
        mDatas = new ArrayList<>();
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final View convertView = mLayoutInflater.inflate(R.layout.item_data, viewGroup, false);
        ViewHolder holder = new ViewHolder(convertView);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        final SimpleData simpleData = mDatas.get(i);
        viewHolder.myContent.setText(simpleData.content);
        viewHolder.myTitle.setText(simpleData.title);
        viewHolder.myImageView.setImageURI(Uri.parse(simpleData.picUrl));
    }

    @Override
    public int getItemCount() {
        return mDatas == null ? 0 : mDatas.size();
    }

    public void addAll(List<SimpleData> resultDatas) {
        if (mDatas == null) {
            mDatas = new ArrayList<>();
        }
        mDatas.addAll(resultDatas);
        notifyDataSetChanged();
    }

    public SimpleData getItemAtPosition(int position) {
        return mDatas.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private SimpleDraweeView myImageView;
        private TextView myTitle;
        private TextView myContent;

        public ViewHolder(View convertView) {
            super(convertView);
            myImageView = (SimpleDraweeView) convertView.findViewById(R.id.my_image_view);
            myTitle = (TextView) convertView.findViewById(R.id.my_title);
            myContent = (TextView) convertView.findViewById(R.id.my_content);
            GenericDraweeHierarchyBuilder builder =
                    new GenericDraweeHierarchyBuilder(mContext.getResources());
            GenericDraweeHierarchy hierarchy = builder
                    .setFadeDuration(200)
                    .setPlaceholderImage(new ColorDrawable(0xf2f2f2))
                    .setProgressBarImage(new CircularProgressDrawable())
                    .build();
            myImageView.setHierarchy(hierarchy);
            convertView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            final int adapterPosition = getAdapterPosition();
            final SimpleData itemAtPosition = getItemAtPosition(adapterPosition);
            if (mOnItemClick != null) {
                mOnItemClick.onItemClick(itemAtPosition, v);
            }
        }
    }
}
