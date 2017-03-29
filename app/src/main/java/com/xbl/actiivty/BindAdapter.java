package com.xbl.actiivty;

import android.bluetooth.BluetoothDevice;
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

public class BindAdapter extends BaseAdapter {
    private Context context;
    private List<BluetoothDevice> bindList;

    public BindAdapter(Context context, List<BluetoothDevice> bindList) {
        this.context = context;
        this.bindList = bindList;
    }

    @Override
    public int getCount() {
        return bindList.size();
    }

    @Override
    public Object getItem(int position) {
        return bindList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view= LayoutInflater.from(context).inflate(R.layout.item_device_list,null);
        TextView text= (TextView) view.findViewById(R.id.device_info);
        text.setText(bindList.get(position).getName());
        return view;
    }
}
