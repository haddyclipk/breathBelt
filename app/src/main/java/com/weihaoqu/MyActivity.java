package com.weihaoqu;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.advanpro.bluetooth.BluetoothBLE;
import com.advanpro.smartbelt.BeltDataNode;
import com.advanpro.smartbelt.ProcessModule;
import com.advanpro.smartbelt.SmartBeltBLE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MyActivity extends Activity implements View.OnClickListener {

    Button button;
    Button record;
    Button stop;
    Button breath;
    TextView dirct;
    TextView tt;
    TextView tc;
    TextView at;
    TextView fr;
    SmartBeltBLE ble;
    ListView list;
    DeviceListAdapter deviceListAdapter;
    String strCurDevMac;

    boolean flag=false;
    File svFile;
//breath data
    long breathCount=0;
    double totalTime=0;
    double avrTime=0;
    double freq=0;


    //end breath
    SimpleDateFormat sDateFormat;
    String time, filename;
    private Timer mTimer;
    private TimerTask task;
    private int samplerate = 1000;  // 10 data per second

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(this);
        record=(Button)findViewById(R.id.button2);
        record.setOnClickListener(this);
        stop = (Button)findViewById(R.id.button3);
        stop.setOnClickListener(this);
        breath = (Button)findViewById(R.id.button4);
        breath.setOnClickListener(this);

        dirct=(TextView)findViewById(R.id.direction);
        tt=(TextView)findViewById(R.id.tt);
        tc=(TextView)findViewById(R.id.tc);
        at=(TextView)findViewById(R.id.at);
        fr=(TextView)findViewById(R.id.freq);
        deviceListAdapter = new DeviceListAdapter(this);
        list = (ListView) findViewById(R.id.listView);
        list.setAdapter(deviceListAdapter);
        sDateFormat = new SimpleDateFormat("yyyy-MM-dd-kk-mm-ss");

        BluetoothBLE.init(this);
        ble = new SmartBeltBLE(this);
    }

    public void record(){
        time = sDateFormat.format(new java.util.Date());
        filename = time + ".txt";
        Toast.makeText(this, filename, Toast.LENGTH_LONG).show();
        createFile(filename);
        timerset();
    }
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            Toast.makeText(this, "SD Ready", Toast.LENGTH_LONG).show();
            return true;
        }
        Toast.makeText(this, "No SD", Toast.LENGTH_LONG).show();
        return false;
    }
    private void createFile(String filename) {
        //File dir = new File(Environment.getExternalStoragePublicDirectory(DOWNLOAD_SERVICE));

        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/WeiHao/");
        if (!dir.exists()) {
            if(!dir.mkdirs()){
                Log.e("TravellerLog :: ", "Problem creating Image folder");}
        }
        if (isExternalStorageWritable()) {
            svFile = new File(dir, filename);
        }
        Toast.makeText(this, svFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        dirct.setText( svFile.getAbsolutePath());

    }

    private void saveData(File file, String data) {
        // TODO Auto-generated method stub
        try {
            BufferedWriter buf = new BufferedWriter(new FileWriter(file, true));
            buf.append(data);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    private void timerset() {

        task = new TimerTask() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                if(!flag){
                for (int i=0;i<10;++i){
                saveData(svFile, ble.nodeV[i]);}}
                else{mTimer.cancel();
                }
            }
        };

        mTimer = new Timer();
        mTimer.schedule(task,0,samplerate);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                if (BluetoothBLE.Instance.isScanBluetooth())
                    scanSmartDevice(false);
                else
                    scanSmartDevice(true);
                break;
            case R.id.button2:
                record();
                break;
            case R.id.button3:
               // mTimer.cancel();
                flag=true;
                //dirct.setText(String.valueOf(ble.avrTime));
                Toast.makeText(this, "timer stopped", Toast.LENGTH_LONG).show();
                break;
            case R.id.button4:
                tt.setText(String.valueOf(ble.totalTime));
                tc.setText(String.valueOf(ble.breathCount));
                at.setText(String.valueOf(ble.avrTime));
                fr.setText(String.valueOf(ble.freq));
                break;
        }
    }

    // 搜索呼吸带
    private void scanSmartDevice(boolean enable) {
        if (enable) {
            deviceListAdapter.clear();
            deviceListAdapter.notifyDataSetChanged();
            BluetoothBLE.Instance.scanBluetoothBLE(handler);
            button.setText("Stop");

            android.os.Message msg = new android.os.Message();
            msg.what = 1;
            handler.sendMessageDelayed(msg, 5000);
        } else {
            BluetoothBLE.Instance.stopBluetoothBLEScan();
            button.setText("Scan");
        }
    }

    // 处理蓝牙搜索的消息
    Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case BluetoothBLE.MESSAGE_DEVICE_DISCOVER:
                    BluetoothBLE.DeviceInfo BDIMDevice = (BluetoothBLE.DeviceInfo)msg.obj;
                    deviceListAdapter.addSmartDevice(BDIMDevice);
                    deviceListAdapter.notifyDataSetChanged();
                    break;
                case 1:
                    scanSmartDevice(false);
                    break;
            }
        }
    };

    // 设备列表Adapter
    private class DeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothBLE.DeviceInfo> smartDevices;
        private Context context;

        public DeviceListAdapter(Context context) {
            super();
            this.context = context;
            smartDevices = new ArrayList<BluetoothBLE.DeviceInfo>();
        }

        public void addSmartDevice(BluetoothBLE.DeviceInfo device) {
            boolean bExist = false;
            for (BluetoothBLE.DeviceInfo i : smartDevices) {
                if (i.getBluetoothDeviceMac().equals(device.getBluetoothDeviceMac())) {
                    bExist = true;
                    break;
                }
            }

            if (!bExist) {
                smartDevices.add(device);
            }
        }

        public BluetoothBLE.DeviceInfo getSmartDevice(int position) { return smartDevices.get(position); }

        public void clear() { smartDevices.clear(); }

        @Override
        public int getCount() { return smartDevices.size(); }

        @Override
        public Object getItem(int position) { return smartDevices.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            view = LayoutInflater.from(context).inflate(R.layout.device, null);
            RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.adapter_rl);
            TextView deviceName = (TextView) view.findViewById(R.id.deviceName);
            TextView deviceMac = (TextView) view.findViewById(R.id.deviceMac);

            final BluetoothBLE.DeviceInfo device = smartDevices.get(position);
            deviceName.setText(device.getBluetoothDeviceName());
            deviceMac.setText(device.getBluetoothDeviceMac());
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TextView status = (TextView) v.findViewById(R.id.status);
                    if (ble.isConnected() && strCurDevMac != null && strCurDevMac.equals(device.getBluetoothDeviceMac())) {
                        status.setText("wait");
                        ble.disconnect();
                    } else {
                        ble.disconnect();
                        if (ble.connect(device, processCB)) {
                            status.setText("connected");
                            strCurDevMac = device.getBluetoothDeviceMac();

                        }
                    }
                }
            });

            return view;
        }
    }

    private SmartBeltBLE.ProcessCallback processCB = new SmartBeltBLE.ProcessCallback(){
        @Override
        public void processBreathData(BeltDataNode data) {
            for(int i=0;i<10;++i){
                ble.nodeV[i]=data.beltACCXdata[i]+","+data.beltACCYdata[i]+","+data.beltACCZdata[i]+";";}
        }
        @Override
        public void processBreathStat(ProcessModule.SmartBelt_TSBreathStats stat) {

            ble.freq=stat.dAvgBrthFreq;
            ble.breathCount=stat.lTotalBrthCnt;
            ble.totalTime=stat.dTotalBrthTime;
            ble.avrTime=stat.dAvgBrthTime;
        }
        @Override
        public void processStepStat(ProcessModule.SmartBelt_TSStepCounterOutput stat) {}
    };
}
