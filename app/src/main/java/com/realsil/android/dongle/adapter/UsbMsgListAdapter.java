package com.realsil.android.dongle.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.realsil.android.dongle.R;

import java.util.List;

public class UsbMsgListAdapter extends BaseAdapter {


    private List<String> mMsgList;
    private Context mContext;

    public UsbMsgListAdapter(Context context, List<String> msgList) {
        this.mMsgList = msgList;
        this.mContext = context;
    }

    public void addMsgItem(String msgStr) {
        mMsgList.add(0, msgStr);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mMsgList.size();
    }

    @Override
    public Object getItem(int position) {
        return mMsgList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_usb_msg, parent, false);
            holder = new ViewHolder();
            holder.tv_msg_text = convertView.findViewById(R.id.tv_msg_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tv_msg_text.setText(mMsgList.get(position));
        return convertView;
    }


    static class ViewHolder {
        TextView tv_msg_text;
    }


}
