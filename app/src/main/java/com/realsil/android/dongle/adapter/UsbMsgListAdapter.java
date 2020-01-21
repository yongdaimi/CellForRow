package com.realsil.android.dongle.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.realsil.android.dongle.R;
import com.realsil.android.dongle.entity.UsbMsg;

import java.util.List;

public class UsbMsgListAdapter extends RecyclerView.Adapter<UsbMsgListAdapter.VH> {


    private List<UsbMsg> mMsgList;

    private Context mContext;

    public UsbMsgListAdapter(Context context, List<UsbMsg> msgList) {
        this.mMsgList = msgList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_usb_msg, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        UsbMsg usbMsg = mMsgList.get(position);
        holder.tv_msg_text.setText(usbMsg.getMsgString());

        if (usbMsg.getMsgType() == UsbMsg.MSG_TYPE_ERROR) {
            holder.tv_msg_text.setTextColor(Color.RED);
        } else {
            holder.tv_msg_text.setTextColor(Color.GREEN);
        }
    }

    @Override
    public int getItemCount() {
        return mMsgList.size();
    }

    public void addMsgItem(UsbMsg msgStr) {
        mMsgList.add(0, msgStr);
    }

    static class VH extends RecyclerView.ViewHolder {

        private TextView tv_msg_text;

        VH(@NonNull View itemView) {
            super(itemView);
            tv_msg_text = itemView.findViewById(R.id.tv_msg_text);
        }

    }

    public List<UsbMsg> getMsgList() {
        return mMsgList;
    }

}
