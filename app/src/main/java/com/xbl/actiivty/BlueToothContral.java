package com.xbl.actiivty;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

/**
 * Created by lenovo on 2017/2/1.
 * 控制蓝牙开关
 * 寻找设备
 */

public class BlueToothContral {
    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    private BluetoothAdapter bluetoothAdapter;

    BlueToothContral() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();//实例化
    }

    //打开蓝牙设备，并将蓝牙设备暴露300s,返回蓝牙是否可以打开
    public boolean openBlueTooth(Activity activity) {
        //通过打开一个界面的方式打开蓝牙
        if (bluetoothAdapter != null) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivity(intent);
            //如果不是暴露状态则将其暴露
            if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                //启动窗口，询问用户是否可以设置成允许被发现
                Intent intent1 = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                //可以一直被别的蓝牙设备发现
                intent1.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);//设置为一直暴露
                activity.startActivity(intent1);
            }

        }
        return bluetoothAdapter.isEnabled();


    }

    //搜索设备
    public void searchDevice() {
        //如果正在搜索蓝牙设备，则停止搜索
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        //开始搜索蓝牙设备
        bluetoothAdapter.startDiscovery();
    }
}
