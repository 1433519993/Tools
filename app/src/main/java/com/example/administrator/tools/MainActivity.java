package com.example.administrator.tools;

import android.app.Service;
import android.content.ComponentName;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import cn.jnvc.toolslib.SocketClient;
import cn.jnvc.toolslib.SocketClientService;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Handler mMainHandler;//主线程Handler
    private Button play;
    private Button stop;
    private TextView display;
    private EditText editText;
    private Button send;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainHandler = new Handler() {//实例化主线程,用于更新UI
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0x0:
                        display.setText(msg.obj.toString());
                        break;
                }
            }
        };

        init();
        SocketClient.setCallback(new SocketClient.CallBack() {
            @Override
            public void Receive(String data) {
                Message msg = new Message();
                msg.obj = data;
                msg.what = 0x0;
                mMainHandler.sendMessage(msg);
            }
        });

    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn1:
                SocketClient.Builder builder = new SocketClient.Builder(MainActivity.this);
                builder.setAddress("10.2.0.216",1234);
                SocketClient.onCreate();
                break;
            case R.id.btn2:
                SocketClient.onDestroy();
                break;
            case R.id.btn_send:
                SocketClient.sendOrder(editText.getText().toString());
                break;
        }
    }


    private void init() {
        play = (Button) findViewById(R.id.btn1);
        stop = (Button) findViewById(R.id.btn2);
        editText = (EditText) findViewById(R.id.et);
        send = (Button) findViewById(R.id.btn_send);
        display = (TextView) findViewById(R.id.tv_display);

        play.setOnClickListener(this);
        stop.setOnClickListener(this);
        send.setOnClickListener(this);
    }
}
