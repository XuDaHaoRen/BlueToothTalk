package com.xbl.actiivty;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by April on 2017/3/4.
 */

public class MsgAdapter extends BaseAdapter {
    private Context context;
    private List<ChatMessage> msgList;

    public MsgAdapter(Context context, List<ChatMessage> msgList) {
        this.context = context;
        this.msgList = msgList;
    }

    @Override
    public int getCount() {
        return msgList.size();
    }

    @Override
    public Object getItem(int position) {
        return msgList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v= LayoutInflater.from(context).inflate(R.layout.iteam_chat,null);
        TextView severText= (TextView) v.findViewById(R.id.server_msg);
        TextView clientText= (TextView) v.findViewById(R.id.client_msg);
        if(msgList.get(position).messageSender==ChatMessage.MSG_SENDER_ME){
            severText.setVisibility(View.VISIBLE);
            clientText.setVisibility(View.GONE);
            severText.setText("我说："+msgList.get(position).messageContent);
        }else{
            clientText.setVisibility(View.VISIBLE);
            severText.setVisibility(View.GONE);
            clientText.setText("对方说："+msgList.get(position).messageContent);
        }
        return v;
    }
}
