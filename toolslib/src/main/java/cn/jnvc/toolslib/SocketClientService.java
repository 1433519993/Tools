package cn.jnvc.toolslib;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class SocketClientService extends Service {
    private static final String TAG = "SocketClientService";
    private SocketBinder socketBinder = new SocketBinder();
    private Socket socket; /*socket*/
    private Thread connectThread; /*连接线程*/
    private String ip; /*服务器地址*/
    private String port; /*服务器端口*/
    private OutputStream outputStream; /*输出*/
    private InputStream inputStream; /*输入*/
    private String Message = null;
    byte buf[]  = new byte[4096];//缓存数组
    private Timer timer = new Timer();
    private TimerTask task;
    private int newDate = 0; // 新时间
    private int oldDate = 0; // 旧时间
    private boolean isReConnect = true;/*默认重连*/
    private Handler handler = new Handler(Looper.getMainLooper());
    private CallBack callBack;

    //client 可以通过Binder获取Service实例
    public class SocketBinder extends Binder {
        public SocketClientService getService() {
            return SocketClientService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "--------->onCreate: ");
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "--------->onBind: ");
        return socketBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "--------->onUnbind: ");
        return super.onUnbind(intent);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Log.e(TAG, "--------->onStart: ");
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "--------->onStartCommand: ");

        /*拿到传递过来的ip和端口号*/
        ip = intent.getStringExtra("ip");
        port = intent.getStringExtra("port");
        Log.e(TAG, "ip="+ip+"port"+port);

        /*初始化socket*/
        initSocket();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "--------->onDestroy: ");
        isReConnect = false;
        releaseSocket();
        super.onDestroy();
    }

    /*初始化socket*/
    private void initSocket() {
        if (socket == null && connectThread == null) {
            connectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    socket = new Socket();
                    /*超时时间为2秒*/
                    try {
                        socket.connect(new InetSocketAddress(ip, Integer.valueOf(port)), 2000);

                        /*连接成功的话  发送心跳包*/
                        if (socket.isConnected()) {
                            /*因为Toast是要运行在主线程的  这里是子线程  所以需要到主线程哪里去显示toast*/
                            toastMsg("socket已连接");
                            /*发送心跳数据*/
                            sendBeatData();
                            while (true) {
                                Log.e(TAG, "--------->Message: "+getData());
                                callBack.Receive(getData());
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            connectThread.start();
        }
    }

    /**
     * 发送数据
     * @param order 发送数据
     * */
    public void sendOrder(final String order) {
        if (socket != null && socket.isConnected()) {
            /*发送指令*/
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outputStream = socket.getOutputStream();
                        if (outputStream != null) {
                            outputStream.write((order).getBytes("gb2312"));
                            outputStream.flush();

                            oldDate = getDate(); // 保存当前旧时间
                            Log.e(TAG, "--------->oldDate: "+oldDate);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            toastMsg("socket连接错误,请重试");
        }
    }

    /**
     * 接收数据inputStream
     *
     * */
    public void getOrder(CallBack call){
        call.Receive(Message);
        Log.e(TAG, "--------->getOrder: "+Message);
    }
    private String getData() throws IOException {
        inputStream = socket.getInputStream();//输入流对象
        int len = inputStream.read(buf);
        Message = new String(buf, 0, len,"gb2312");
        Log.e(TAG, "--------->getData: "+Message);
        return Message;
    }

    /*定时发送心跳数据包*/
    private void sendBeatData() {

        if (timer == null) {
            timer = new Timer();
        }
        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    try {

                        newDate = getDate();// 获取当前新时间
                        Log.e(TAG, "--------->newDate: "+newDate);

                        if ((newDate - oldDate)>30) {
                            outputStream = socket.getOutputStream();

                            /*这里的编码方式根据你的需求去改*/
                            outputStream.write(("ping").getBytes("gb2312"));
                            outputStream.flush();
                        }
                    } catch (Exception e) {
                        /*发送失败说明socket断开了或者出现了其他错误*/
                        toastMsg("连接断开，正在重连");
                        /*重连*/
                        releaseSocket();
                        e.printStackTrace();
                    }
                }
            };
        }
        timer.schedule(task, 0, 10000);
    }

    /*释放资源*/
    private void releaseSocket() {

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
        if (socket != null) {
            try {
                socket.close();

            } catch (IOException e) {
            }
            socket = null;
        }
        if (connectThread != null) {
            connectThread = null;
        }
        if (buf != null) {
            buf = null;
        }
        /*重新初始化socket*/
        if (isReConnect) {
            initSocket();
        }
    }

    /**
     * 主线程显示toast
     * @param msg 要显示的数据
     * */
    private void toastMsg(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setCallback(CallBack callback) {
        this.callBack = callback;
    }
    //TCP
    public interface CallBack {
        //这个回调用于获取服务端返回的数据
        void Receive(String data);
    }

    private int getDate(){
        int date = 0; // 单位秒

        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY); //小时
        int minute = calendar.get(Calendar.MINUTE); //分钟
        int second = calendar.get(Calendar.SECOND); //秒
        date = hour*60+minute*60+second;

        return date;
    }

}
