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
 * Created by lenovo on 2017/1/10.
 */

public class BlueDeviceAdapter extends BaseAdapter {
    private Context context;
    private List<BluetoothDevice> mList;

    public BlueDeviceAdapter(Context context, List<BluetoothDevice> mList) {
        this.context = context;
        this.mList = mList;
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder=null;
        if (convertView==null){
            holder=new ViewHolder();
            convertView= LayoutInflater.from(context).inflate(R.layout.item_device_list,null);
            convertView.setTag(holder);
        }else{
            holder= (ViewHolder) convertView.getTag();
        }
        holder.text_view1= (TextView) convertView.findViewById(R.id.device_name);
        holder.text_view2= (TextView) convertView.findViewById(R.id.device_info);
        //获取对应的蓝牙设备，将List中的每个item赋值给device
        BluetoothDevice device= (BluetoothDevice) getItem(position);
        holder.text_view1.setText(device.getName());//得到蓝牙设备的名称
        holder.text_view2.setText(device.getAddress());//获得蓝牙地址
        return convertView;
    }
    class ViewHolder{
        TextView text_view1;
         TextView text_view2;
    }
    //刷新数据
    public void refresh(List<BluetoothDevice> data) {
        mList = data;
        notifyDataSetChanged();
    }
}
