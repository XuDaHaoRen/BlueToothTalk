package com.xbl.actiivty;

/**
 * Created by April on 2017/3/8.
 * 存放接受的消息和发送的消息的一个类
 */

public class ChatMessage {
    //主动发出的消息
    static public final int MSG_SENDER_ME = 0;
    //接收到的消息
    static public final int MSG_SENDER_OTHERS = 1;
    public int messageSender;//判断当前是接收到的消息还是发送的消息
    public String messageContent;//消息内容
}
