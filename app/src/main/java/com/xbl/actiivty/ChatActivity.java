package com.xbl.actiivty;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lenovo on 2017/2/4.
 */

public class ChatActivity extends Activity {
    private static final int RESULT_CODE_BTDEVICE = 0;
    private Button sendBtn;
    private ListView chatLV;
    private EditText msgEdt;
    private Button search_btn;
    private MsgAdapter msgAdapter;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private ConnectManager connectManager;
    private List<ChatMessage> msgList = new ArrayList<>();
    /**
     * 监听发送信息的状态
     */
    private final static int MSG_SENT_DATA = 0;
    private final static int MSG_RECEIVE_DATA = 1;
    private final static int MSG_UPDATE_UI = 2;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_SENT_DATA: {
                    //UI线程处理发送成功的数据，
                    //把文字内容展示到主界面上
                    //获取发送的文字内容
                    byte[] data = (byte[]) msg.obj;
                    boolean suc = msg.arg1 == 1;
                    if (data != null && suc) {
                        //发送成功后创建消息
                        ChatMessage chatMsg = new ChatMessage();
                        chatMsg.messageSender = ChatMessage.MSG_SENDER_ME;
                        chatMsg.messageContent = new String(data);
                        //将消息展示到消息列表中
                        msgList.add(chatMsg);
                        msgAdapter.notifyDataSetChanged();
                        msgEdt.setText("");
                        break;
                    }
                }


                case MSG_RECEIVE_DATA: {
                    //UI线程处理接收到的对方发送的数据，
                    //把文字内容展示到主界面上
                    byte[] data = (byte[]) msg.obj;
                    if (data != null) {
                        ChatMessage chatMsg = new ChatMessage();
                        chatMsg.messageSender = ChatMessage.MSG_SENDER_OTHERS;
                        chatMsg.messageContent = new String(data);
                        //将消息展示到消息列表中
                        msgList.add(chatMsg);
                        msgAdapter.notifyDataSetChanged();
                    }
                }
                    break;
                    case MSG_UPDATE_UI: {
                        //更新界面上的按钮等显示状态
                        upButton();
                    }
                    break;
                }
            }
        }

        ;
        //监听状态的变化，并更改按钮的文字
        private ConnectManager.ConnectionListener connectionListener = new ConnectManager.ConnectionListener() {
            @Override
            public void onConnectStateChange(int oldState, int State) {
                //连接状态的变化通知给UI线程，请UI线程处理
                handler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
            }

            @Override
            public void onListenStateChange(int oldState, int State) {
                //监听状态的变化通知给UI线程，请UI线程处理
                handler.obtainMessage(MSG_UPDATE_UI).sendToTarget();
            }

            @Override
            public void onSendData(boolean suc, byte[] data) {
                //将发送的数据交给UI线程，请UI线程处理
                handler.obtainMessage(MSG_SENT_DATA, suc ? 1 : 0, 0, data).sendToTarget();
            }

            @Override
            public void onReadData(byte[] data) {
                //将收到的数据交给UI线程，请UI线程处理
                handler.obtainMessage(MSG_RECEIVE_DATA, data).sendToTarget();

            }
        };

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_chat);
            //实例化ConnectManager对象
            connectManager = new ConnectManager(connectionListener);
            //开启监听的线程
            connectManager.startListen();
            sendBtn = (Button) findViewById(R.id.send_btn);
            chatLV = (ListView) findViewById(R.id.chat_list_view);
            msgEdt = (EditText) findViewById(R.id.msg_edt);
            search_btn = (Button) findViewById(R.id.search_btn);
            msgAdapter = new MsgAdapter(this, msgList);
            chatLV.setAdapter(msgAdapter);
            search_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //根据当前连接到状态，判断对应的响应方式，
                    String state = (String) search_btn.getText();
                    //当前是正在连接或者已连接状态
                    if (state.equals("取消设备")) {
                        if (connectManager.getCurrentConnectState() == connectManager.CONNECT_STATE_CONNECTED) {
                            connectManager.disconnect();
                            search_btn.setText("链接设备");
                        }
                    } else if (state.equals("链接设备")) {
                        //启动DeviceListActivity获取可以连接到设备名称
                        Intent i = new Intent(ChatActivity.this, BindActivity.class);
                        startActivityForResult(i, RESULT_CODE_BTDEVICE);
                        search_btn.setText("取消设备");

                    }
                }
            });
            BlueToothContral contral = new BlueToothContral();
            //先判断蓝牙的状态并打开蓝牙
            if (!contral.openBlueTooth(ChatActivity.this)) {
                Toast.makeText(this, "蓝牙打开失败", Toast.LENGTH_LONG).show();
            }
            sendBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("TAG", "发送按钮被点击到");
                    String msg = msgEdt.getText().toString();
                    if (msg != null) {
                        msg = msg.trim();
                        if (msg.length() > 0) {
                            //利用ConnectionManager发送数据
                            boolean ret = connectManager.sendData(msg.getBytes());
                            if (!ret) {
                                Toast.makeText(ChatActivity.this, "发送失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            });

        }

        //得到从BindActivity的返回数据进行连接
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == RESULT_CODE_BTDEVICE && resultCode == RESULT_OK) {
                //得到在BindActivity中链接的设备
                String address = data.getStringExtra("DEVICE_ADDR");
                //得到蓝牙设备的地址后，就可以通过ConnectionManager模块去连接设备了。
                connectManager.connect(address);
                Log.e("TAG","已连接"+address);
            }
        }

        //更新按钮
        private void upButton() {
            if (connectManager == null) {
                Log.e("TAG","connectManager == null");
                return;
            }
            //设置成连接状态，文字变为取消链接允许点击发送按钮和文字编辑框
            if (connectManager.getCurrentConnectState() == connectManager.CONNECT_STATE_CONNECTED) {
                search_btn.setText("取消设备");
                msgEdt.setEnabled(true);
                sendBtn.setEnabled(true);
            }
            //设置成正在连接状态,不可发送信息
            else if (connectManager.getCurrentConnectState() == connectManager.CONNECT_STATE_CONNECTING) {
                search_btn.setText("取消链接");
                msgEdt.setEnabled(false);
                sendBtn.setEnabled(false);
            }
            //设置成未连接状态，禁止点击发送按钮和文字编辑框
            else if (connectManager.getCurrentConnectState() == connectManager.CONNECT_STATE_IDLE) {
                search_btn.setText("链接设备");
                msgEdt.setEnabled(false);
                sendBtn.setEnabled(false);
            }
        }

        //退出的时候取消监听
        @Override
        protected void onDestroy() {
            //移除Handler中可能存在的各种任务
            handler.removeMessages(MSG_UPDATE_UI);
            handler.removeMessages(MSG_SENT_DATA);
            handler.removeMessages(MSG_RECEIVE_DATA);

            //停止监听
            if (connectManager != null) {
                connectManager.disconnect();
                // connectManager.stopListen();
            }
        }
    }



