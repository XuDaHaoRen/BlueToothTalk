package com.xbl.actiivty;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BindActivity extends Activity {
    private ListView deviceListView;//存放搜索信息的ListView
    private List<BluetoothDevice> deviceList = new ArrayList<>();
    private List<BluetoothDevice> bindList = new ArrayList<>();
    private BluetoothAdapter bluetoothAdapter;
    private ListView mBindListView;
    private BindAdapter bindAdapter;
    private BlueDeviceAdapter deviceAdapter;
    public BluetoothDevice device;
    private DeviceReceiver deviceReceiver;
    class DeviceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //蓝牙扫描过程开始
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.e("TAG", "蓝牙开始扫描");
                //初始化数据列表
                deviceList.clear();
                deviceAdapter.notifyDataSetChanged();
                //扫描过程结束
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.e("TAG", "扫描过程结束");
                //搜索到任意一个蓝牙设备
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //找到一个，添加一个
                deviceList.add(device);
                deviceAdapter.notifyDataSetChanged();
                //蓝牙扫描状态发生改变
            } else if (BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)) {
                //EXTRA_SCAN_MODE扫描模式的一种
                int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, 0);
                //可见状态
                if (scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                } else {
                }
                //当前设备链接状态改变判断有没有连接上
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (remoteDevice == null) {
                    Toast.makeText(context, "没有资源", Toast.LENGTH_SHORT).show();
                    return;
                }
                int status = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
                //如果当时状态是已连接
                if (status == BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(context, "Bonded " + remoteDevice.getName(), Toast.LENGTH_SHORT).show();
                    //状态是正在连接
                } else if (status == BluetoothDevice.BOND_BONDING) {
                    Toast.makeText(context, "Bonding " + remoteDevice.getName(), Toast.LENGTH_SHORT).show();
                    //状态是还未链接
                } else if (status == BluetoothDevice.BOND_NONE) {
                    Toast.makeText(context, "Not bond " + remoteDevice.getName(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bind);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BlueToothContral contral = new BlueToothContral();
        //将蓝牙暴露并开始搜索
        contral.openBlueTooth(BindActivity.this);
        contral.searchDevice();
        //注册设备搜索广播
        registerBrocast();
        bindUI();
        Log.e("TAG", deviceList.size() + "");
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device=deviceList.get(position);
                //取消可能正在进行的搜索
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();

                }
                //进行连接
                if (device.createBond()) {
                    bindList.add(device);
                    bindAdapter.notifyDataSetChanged();
                }

            }
        });
        mBindListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //点击链接之后将信息传递给第一个界面
                 device = deviceList.get(position);
                Intent i = new Intent();
                //将设备地址存储到Intent当中
                i.putExtra("DEVICE_ADDR", device.getAddress());
                //将数据结果返回给ChatActivity，并关闭当前的Activity界面
                setResult(RESULT_OK, i);
                finish();
            }
        });


    }

    private void bindUI() {
        deviceListView = (ListView) findViewById(R.id.device_list_view);
        mBindListView = (ListView) findViewById(R.id.bind_list_view);
        //点击listView进行蓝牙的绑定
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = deviceList.get(position);
                device.createBond();//绑定
                bindAdapter.notifyDataSetChanged();

            }
        });
        deviceAdapter = new BlueDeviceAdapter(BindActivity.this, deviceList);
        deviceListView.setAdapter(deviceAdapter);
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        Iterator<BluetoothDevice> it = pairedDevices.iterator();
        while (it.hasNext()) {
            bindList.add(it.next());
        }
        bindAdapter = new BindAdapter(BindActivity.this, bindList);
        mBindListView.setAdapter(bindAdapter);
    }

    private void registerBrocast() {
        //蓝牙搜索绑定的广播注册
        IntentFilter foundFilter = new IntentFilter();
        //开始查找
        foundFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //结束查找
        foundFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //查找设备
        foundFilter.addAction(BluetoothDevice.ACTION_FOUND);
        //设备扫描模式改变
        foundFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        //绑定状态
        foundFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        deviceReceiver=new DeviceReceiver();
        registerReceiver(deviceReceiver, foundFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceReceiver!=null){
            unregisterReceiver(deviceReceiver);
        }
    }
}






