package cn.jnvc.toolslib;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class SocketClient {

    private static final String TAG = "SocketClient";
    private static Timer timer = new Timer();
    private static TimerTask task;
    private static Socket socket;
    private static boolean SocketClientState = false; // 获取Socket连接状态，默认false
    private static OutputStream outputStream; // 输出
    private static InputStream inputStream; // 输入
    private static DataInputStream dataInputStream;
    static byte buf[]  = new byte[4096];//缓存数组
    private static String ServerIP = "127.0.0.1";
    private static int ServerPort = 0;
    private static boolean isReConnect = true; // 是否重连,默认true
    private static Context context;
    private static Handler handler = new Handler(Looper.getMainLooper());
    private static CallBack callBack;// tcp回调接口

    public SocketClient() {
    }

    public static class Builder {

        public Builder(Context con){
            context = con;
        }

        /**
         *  设置提示信息
         *  @param ip 服务器ip地址
         *  @param port 服务器端口号
         *  @return
         * */
        public Builder setAddress (String ip, int port) {
            ServerIP = ip;
            ServerPort = port;
            return this;
        }

        /**
         *  设置是否重连
         *  @param isReconnect
         * */
        public Builder setReconnect (boolean isReconnect) {
            isReConnect = isReconnect;
            return this;
        }
    }

    /*回调接口*/
    public static void setCallback(CallBack callback) {
        callBack = callback;
    }
    public interface CallBack {
        //这个回调用于获取服务端返回的数据
        void Receive(String data);
        void getClientState(boolean state);
    }

    public static Socket onCreate() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                socket = new Socket();
                try {
                    socket.connect(new InetSocketAddress(ServerIP, ServerPort), 2000); // 超时连接
                    inputStream = socket.getInputStream();//输入流对象

                    while (true) {
                        //Log.e(TAG, "--------->获取值");
                        int len = inputStream.read(buf);
                        if (len == -1) {
                            toastMsg("网络异常，重连尝试");
                            break;
                        } else {
                            String Message = new String(buf, 0, len, "gb2312");
                            Log.e(TAG, "--------->Message: " + Message);
                            callBack.Receive(Message);
                        }

                        if (socket.isConnected() && !socket.isClosed()) { //第一个返回true第二个返回false时，socket处于连接状态
                            //Log.e(TAG, "--------->获取连接状态");
                            //toastMsg("服务已连接");
                            SocketClientState = true;
                            callBack.getClientState(SocketClientState);
                            //sendBeatData();
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "--------->socket重连: ");
                    /*重连*/
                    releaseSocket();
                    e.printStackTrace();
                }
            }
        }).start();

        return socket;
    }

    /**
     * 发送数据
     * @param order 发送数据
     * */
    public static void sendOrder(final String order) {
        if (socket != null && socket.isConnected()) {
            /*发送指令*/
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outputStream = socket.getOutputStream();
                        Log.e(TAG, "--------->发送信息的长度: "+order.length());
                        if (outputStream != null && order.length()>0) {
                            outputStream.write((order).getBytes());
                            outputStream.flush();
                        } else {
                            toastMsg("不能发送空值！");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        try {
                            inputStream.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }).start();
        } else {
            Log.e(TAG, "--------->socket连接错误,请重试: ");
        }
    }

    /*定时发送心跳数据包*/
    private static void sendBeatData() {
        if (timer == null) {
            timer = new Timer();
        }
        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    try {

                        outputStream = socket.getOutputStream();

                        /*JSON心跳包*/
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("ping", "01001");

                        outputStream.write((jsonObject.toString()).getBytes());
                        outputStream.flush();

                    } catch (Exception e) {
                        /*发送失败说明socket断开了或者出现了其他错误*/
                        Log.e(TAG, "--------->socket重连: ");
                        /*重连*/
                        releaseSocket();
                        e.printStackTrace();
                    }
                }
            };
        }
        timer.schedule(task, 0, 1000);
    }

    /*释放资源*/
    private static void releaseSocket() {

        if (task != null) {
            task.cancel();
            task = null;
        }
        if (timer != null) {
            timer.purge();
            timer.cancel();
            timer = null;
        }
        if (outputStream != null) {
            try {
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }
        if (inputStream != null) {
            try {
                inputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
        if (dataInputStream != null) {
            try {
                dataInputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            dataInputStream = null;
        }
        if (socket != null) {
            try {
                socket.close();

            } catch (IOException e) {
            }
            socket = null;
        }
        /*重新初始化socket*/
        if (isReConnect) {
            Log.e(TAG, "--------->ip: "+ServerIP +" port"+ServerPort);
            onCreate();
        }
    }

    /* 断开连接*/
    public static void onDestroy() {
        toastMsg("服务已断开");
        isReConnect = false;
        releaseSocket();
    }

    /**
     * 主线程显示toast
     * @param msg 要显示的数据
     * */
    private static void toastMsg(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

}
