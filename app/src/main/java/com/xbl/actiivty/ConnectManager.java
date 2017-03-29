package com.xbl.actiivty;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by April on 2017/3/8.
 */

public class ConnectManager {
    private BluetoothAdapter bluetoothAdapter;
    private ConnectionListener connectionListener;
    private ConnectedThread connectedThread;
    private AcceptThread acceptThread;
    private static final String BT_NAME = "AnddleChat";
    private final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //监听蓝牙状态变化，并和ChatActivity实时通信的类
    public interface ConnectionListener {
        public void onConnectStateChange(int oldState, int State);//监听当前蓝牙链接状态变化的方法发送给ChatActivity

        public void onListenStateChange(int oldState, int State);//监听状态发生变化

        public void onSendData(boolean suc, byte[] data);//将发送的内容发送给ChatActivity

        public void onReadData(byte[] data);//读取数据
    }
    public ConnectManager(ConnectionListener cl) {
        connectionListener = cl;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    //监听的两种状态
    public static final int LISTEN_STATE_IDLE = 3;//闲置状态
    public static final int LISTEN_STATE_LISTENING = 4;
    //记录当前监听的状态
    private int mListenState = LISTEN_STATE_IDLE;
    //连接的三种状态
    public static final int CONNECT_STATE_IDLE = 0;
    public static final int CONNECT_STATE_CONNECTING = 1;
    public static final int CONNECT_STATE_CONNECTED = 2;
    //记录当前连接的状态
    private int connectStateNow = CONNECT_STATE_IDLE;
    //修改当前连接的状态
    private void setConnectState(int state) {
        ///状态没有发生变化，不用通知
        if (connectStateNow == state) {
            return;
        }
        int oldState = connectStateNow;
        connectStateNow = state;
        //改变状态
        if (connectionListener != null) {
            connectionListener.onConnectStateChange(oldState, connectStateNow);
        }
    }
    //实例化connectedThread创建Socket连接
    public synchronized void connect(String deviceAddr) {
        Log.d("TAG", "ConnectionManager about to connect BT device at:" + deviceAddr);
        if (connectedThread != null) {
            connectedThread.cancel();
        }
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddr);
        try {
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BT_UUID);//实例化服务端
            connected(socket, true);
        } catch (IOException e) {
            Log.e("TAG", "Connect failed", e);
        }

    }
    //停止连接
    public void disconnect() {
        //停止连接线程
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }

    private synchronized void connected(BluetoothSocket socket, boolean needConnect) {
        connectedThread = new ConnectedThread(socket, needConnect);
        connectedThread.start();
    }

    //实例化AcceptThread
    public void startListen() {
        //创建监听线程
        if (acceptThread != null) {
            acceptThread.cancel();
        }
        acceptThread = new AcceptThread();
        acceptThread.start();
    }

    //查询当前监听线程的状态
    public int getCurrentListenState() {
        return mListenState;
    }

    //查询当前连接线程的状态
    public int getCurrentConnectState() {
        return connectStateNow;
    }
    /**
     * 先判断是主动连接还是被动连接
     * 创建输出输入流
     */
    private class ConnectedThread extends Thread {
        private final int MAX_BUFFER_SIZE = 1024;//传递的数据大小
        private BluetoothSocket mSocket;
        private InputStream mInStream;
        private OutputStream mOutStream;
        private boolean mUserCancel;
        private boolean mNeedConnect;
        public ConnectedThread(BluetoothSocket socket, boolean needConnect) {
            Log.d("TAG", "create ConnectedThread");
            setName("ConnectedThread");//线程名字
            mNeedConnect = needConnect;
            mSocket = socket;
            mUserCancel = false;
        }
        @Override
        public void run() {
            Log.d("TAG", "ConnectedThread START");
            setConnectState(CONNECT_STATE_CONNECTING);//设置当前连接状态正在连接
            if (mNeedConnect && !mUserCancel) {
                try {
                    mSocket.connect();
                } catch (IOException e) {
                    Log.d("TAG", "ConnectedThread END at connect(), " + e);//链接完成
                    setConnectState(CONNECT_STATE_IDLE);//连接完成设置链接为闲置状态
                    mSocket = null;
                    connectedThread = null;//结束线程
                    return;
                }
            }
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                Log.d("TAG", "ConnectedThread END at getStream(), " + e); //输入输出流都得到了
                setConnectState(CONNECT_STATE_IDLE);
                mSocket = null;
                connectedThread = null;

                return;
            }
            mInStream = tmpIn;
            mOutStream = tmpOut;
            setConnectState(CONNECT_STATE_CONNECTED);
            byte[] buffer = new byte[MAX_BUFFER_SIZE];
            int bytes;
            while (!mUserCancel) {
                try {
                    Log.d("TAG", "ConnectedThread wait for read data");
                    bytes = mInStream.read(buffer);
                    if (connectionListener != null && bytes > 0) {
                        byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
                        connectionListener.onReadData(data);
                    }
                } catch (IOException e) {
                    Log.d("TAG", "ConnectedThread disconnected, ", e);
                    break;
                }
            }
            setConnectState(CONNECT_STATE_IDLE);
            mSocket = null;
            connectedThread = null;

            if (mUserCancel == true) {
                Log.d("TAG", "ConnectedThread END since user cancel.");
            } else {
                Log.d("TAG", "ConnectedThread END");
            }
        }

        //用户取消链接
        public void cancel() {
            Log.d("TAG", "ConnectedThread cancel START");
            try {
                mUserCancel = true;
                if (mSocket != null) {
                    mSocket.close();
                }

            } catch (IOException e) {
                Log.e("TAG", "ConnectedThread cancel failed", e);
            }

            Log.d("TAG", "ConnectedThread cancel END");
        }
        //发送消息
        public void sendData(byte[] data) {
            try {
                mOutStream.write(data);

                if (connectionListener != null) {
                    connectionListener.onSendData(true, data);
                }
            } catch (IOException e) {
                Log.e("TAG", "send data fail", e);
                if (connectionListener != null) {
                    connectionListener.onSendData(true, data);
                }
            }
        }

    }
    /**
     * 如果有连接请求创建服务端
     */
    class AcceptThread extends Thread {
        private BluetoothServerSocket mServerSocket;
        private boolean mUserCancel;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            mUserCancel = false;
            //创建监听用的ServerSocket
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(
                        BT_NAME, BT_UUID);
            } catch (IOException e) {

            }
            mServerSocket = tmp;
        }

        //监听线程开始运行
        @Override
        public void run() {

            setName("AcceptThread");

            //将ConnectionManger监听的状态设置成“正在监听”
            setListenState(LISTEN_STATE_LISTENING);
            BluetoothSocket socket = null;

            while (!mUserCancel) {
                try {
                    //阻塞在这里，等待别的设备连接
                    socket = mServerSocket.accept();

                } catch (IOException e) {
                    //阻塞过程中，如果其它地方调用了mServerSocket.close()，
                    //将会进入到这个异常当中
                    mServerSocket = null;
                    break;
                }

                if (connectStateNow == CONNECT_STATE_CONNECTED
                        || connectStateNow == CONNECT_STATE_CONNECTING) {
                    //如果当前正在连接别的设备，
                    //或者已经和别的设备连接上了，就放弃这个连接，
                    //因为每次只能和一个设备连接
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (connectStateNow == CONNECT_STATE_IDLE) {
                    //如果当前没有和别的设备连接上，
                    //启动连接线程
                    connectedThread = new ConnectedThread(socket, false);
                    connectedThread.start();
                }
            }

            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mServerSocket = null;
            }
            setListenState(LISTEN_STATE_IDLE);
            acceptThread = null;
        }

        public void cancel() {
            try {
                mUserCancel = true;
                //ServerSocket此时阻塞在accept()方法中，
                //关闭之后，会让accept()方法抛出异常，实现监听线程的退出
                if (mServerSocket != null) {
                    mServerSocket.close();
                }
            } catch (IOException e) {
            }
        }

        //状态发生变化时给出回调
        private synchronized void setListenState(int state) {
            //如果当前没有连接状态的变化就没有通知
            if (mListenState == state) {
                return;
            }
            int oldState = mListenState;
            mListenState = state;
            if (connectionListener != null) {
                Log.d("TAG", "BT state change: " + getState(oldState) + " -> " + getState(mListenState));
                //状态发生变化，发起通知
                connectionListener.onListenStateChange(oldState, mListenState);
            }

        }

        public String getState(int state) {
            switch (state) {
                case CONNECT_STATE_IDLE:
                    return "CONNECT_STATE_IDLE";

                case CONNECT_STATE_CONNECTING:
                    return "CONNECT_STATE_CONNECTING";


                case CONNECT_STATE_CONNECTED:
                    return "CONNECT_STATE_CONNECTED";

                case LISTEN_STATE_IDLE:
                    return "LISTEN_STATE_IDLE";

                case LISTEN_STATE_LISTENING:
                    return "LISTEN_STATE_LISTENING";
            }

            return "UNKNOWN";
        }

    }

    //发送信息并判断是否发送成功
    public synchronized boolean sendData(byte[] data) {
        if(connectedThread != null && connectStateNow == CONNECT_STATE_CONNECTED) {
            connectedThread.sendData(data);

            return true;
        }
        return false;
    }

}
