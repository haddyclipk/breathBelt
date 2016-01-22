package com.advanpro.smartbelt;

import android.content.Context;
import com.advanpro.bluetooth.BluetoothBLE;

import java.util.*;


public class SmartBeltBLE {

    public static final UUID UUID_SMART_BELT_SERVERS = UUID.fromString("00008001-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SMART_BELT_IDSERVERS = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID SMART_BELT_DEVID_Characteristic = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    public static final UUID SMART_BELT_DEVVV_Characteristic = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb");
    public static final UUID SMART_BELT_Sensor_Characteristic = UUID.fromString("00008002-0000-1000-8000-00805f9b34fb");
    public static final UUID SMART_BELT_ACCX_Characteristic = UUID.fromString("00008003-0000-1000-8000-00805f9b34fb");
    public static final UUID SMART_BELT_ACCY_Characteristic = UUID.fromString("00008004-0000-1000-8000-00805f9b34fb");
    public static final UUID SMART_BELT_ACCZ_Characteristic = UUID.fromString("00008005-0000-1000-8000-00805f9b34fb");
    public static final UUID SMART_BELT_ACC_RANGE_Characteristic = UUID.fromString("00008006-0000-1000-8000-00805f9b34fb");
    public static final UUID AMART_BELT_BATTERY_Characteristic = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID SMART_BELT_INFPIPE_Characteristic = UUID.fromString("0000808f-0000-1000-8000-00805f9b34fb");

    private Context context;
    private BluetoothBLE.BluetoothConnect conn;
    private BluetoothBLE.DeviceInfo device;
    private Timer timer = null;
    private StatModel stat = new StatModel();
    private int inst = 0;
    private int processstep = 0;  // 处理记步次数
    private ProcessCallback cb = null;
    public String DeviceID = "";   // 当前连接的设备编号
    public String DeviceVV = "";   // 当前连接的设备编号校验码
    //new
    public String[] nodeV=new String[10];

    public double freq=0;
    public long breathCount=0;
    public double totalTime=0;
    public double avrTime=0;

    public class StatModel {
        public boolean bConnect = false;
        public long tvConnect;  // 连接起点时间
        public long tvDisconnect; // 最近一次中断时间
        public long lDisconnectCount;  // 中断总次数
        public long lRetryCount;  // 重连总次数
    }

    public static class ProcessCallback {
        public void processBreathData(BeltDataNode data) {


        }
        public void processBreathStat(ProcessModule.SmartBelt_TSBreathStats stat) {}
        public void processStepStat(ProcessModule.SmartBelt_TSStepCounterOutput stat) {}
    }

    public SmartBeltBLE(Context context) {
        this.context = context;
    }

    // 连接设备
    public boolean connect(BluetoothBLE.DeviceInfo device, ProcessCallback cb) {
        ProcessModule.SmartBelt_TMdlInitInfo info = new ProcessModule.SmartBelt_TMdlInitInfo();
        ProcessModule.InitDataProcMdl(inst, info);

        ProcessModule.SmartBelt_TSStepCounterParams stepPram = new ProcessModule.SmartBelt_TSStepCounterParams();
        ProcessModule.InitStepCounter(inst);
        ProcessModule.SetParamsToStepCounter(inst, stepPram);

        stat.lDisconnectCount = 0;
        stat.lRetryCount = 0;
        stat.tvConnect = System.currentTimeMillis();
        stat.bConnect = false;
        this.device = device;
        this.cb = cb;
        this.processstep = 0;

        conn = BluetoothBLE.Instance.connect(context, device.getBluetoothDeviceMac(), handlerBluetooth);
        if (conn != null) {
            timer = new Timer(true);
            timer.schedule(new TimerTask() {
                public void run() {
                    handler.sendEmptyMessage(1);
                }
            }, 1000, 1000); //延时1000ms后执行，1000ms执行一次
            stat.bConnect = true;
        }

        return stat.bConnect;
    }

    // 断开连接
    public void disconnect() {
        if (isConnected()) {
            timer.cancel();
			if (conn != null) {
				conn.disconnect();
				conn = null;
			}
            ProcessModule.ExitDataProcMdl(inst);
        }
        stat.bConnect = false;
        timer = null;
    }

    // 是否已连接
    public boolean isConnected() {
        return (timer != null);
    }

    // 数据处理
    private final android.os.Handler handlerBluetooth = new android.os.Handler() {
        private int inotifytype = 0;
        private BeltDataNode node = new BeltDataNode();
        private long lTimePrev = System.currentTimeMillis();

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case BluetoothBLE.MESSAGE_GATT_CONNECTED: {
                    inotifytype = 0;
                    conn.readCharacteristic(UUID_SMART_BELT_IDSERVERS, SMART_BELT_DEVID_Characteristic);
                } break;

                case BluetoothBLE.MESSAGE_DATA_REQUEST: {
                    switch (inotifytype) {
                        case 0:
                            conn.readCharacteristic(UUID_SMART_BELT_IDSERVERS, SMART_BELT_DEVVV_Characteristic);
                            inotifytype++;
                            break;
                        case 1:
                            conn.readCharacteristic(UUID_SMART_BELT_SERVERS, SMART_BELT_ACC_RANGE_Characteristic);
                            inotifytype++;
                            break;
                        case 2:
                            conn.setCharacteristicNotification(UUID_SMART_BELT_SERVERS, SMART_BELT_Sensor_Characteristic, true);
                            inotifytype++;
                            break;
                        case 3:
                            conn.setCharacteristicNotification(UUID_SMART_BELT_SERVERS, SMART_BELT_ACCX_Characteristic, true);
                            inotifytype++;
                            break;
                        case 4:
                            conn.setCharacteristicNotification(UUID_SMART_BELT_SERVERS, SMART_BELT_ACCY_Characteristic, true);
                            inotifytype++;
                            break;
                        case 5:
                            conn.setCharacteristicNotification(UUID_SMART_BELT_SERVERS, SMART_BELT_ACCZ_Characteristic, true);
                            inotifytype++;
                            break;
                        case 6:
                            conn.setCharacteristicNotification(UUID_SMART_BELT_SERVERS, AMART_BELT_BATTERY_Characteristic, true);
                            inotifytype++;
                            break;
                        case 7:
                            conn.setCharacteristicNotification(UUID_SMART_BELT_SERVERS, SMART_BELT_INFPIPE_Characteristic, true);
                            inotifytype++;
                            break;
                        case 8:
                            long lTimeCur = System.currentTimeMillis();
                            if (lTimeCur - lTimePrev >= 3000) {
                                lTimePrev = lTimeCur;
                                GregorianCalendar calendar = new GregorianCalendar();
                                byte year = (byte) (calendar.get(Calendar.YEAR) % 100);
                                byte month = (byte) (calendar.get(Calendar.MONTH) + 1);
                                byte day = (byte) calendar.get(Calendar.DATE);
                                byte hour = (byte) calendar.get(Calendar.HOUR_OF_DAY);
                                byte min = (byte) calendar.get(Calendar.MINUTE);
                                byte sec = (byte) calendar.get(Calendar.SECOND);
                                conn.writeCharacteristic(UUID_SMART_BELT_SERVERS, SMART_BELT_INFPIPE_Characteristic,
                                        new byte[]{0x02, 0x01, year, month, day, hour, min, sec, month, day});
                            }
                            break;
                    }
                } break;

                case BluetoothBLE.MESSAGE_DATA_AVAILABLE: {
                    try {
                        BluetoothBLE.DeviceData data = (BluetoothBLE.DeviceData)msg.obj;
                        if (data.characteristic.equals(SMART_BELT_DEVID_Characteristic)) {
                            // 设备编号
                            DeviceID = new String(data.value);
                        } else if (data.characteristic.equals(SMART_BELT_DEVVV_Characteristic)) {
                            // 校验码
                            DeviceVV = new String(data.value);
                        } else if (data.characteristic.equals(SMART_BELT_INFPIPE_Characteristic)) {
                            // 记步数据
                            if (cb != null && data.value.length == 13) {
                                Date tv = new Date(data.tvMills);
                                ProcessModule.SmartBelt_TSStepCounterPkt pkt = new ProcessModule.SmartBelt_TSStepCounterPkt(processstep++, data.value, tv);
                                int ret = ProcessModule.SendDataToStepCounter(inst, pkt);
                                if (ret == ProcessModule.ErrorCode_Success) {
                                    ret = ProcessModule.StartDataAnalyze(inst);
                                    if (ret == ProcessModule.ErrorCode_Success) {
                                        ProcessModule.SmartBelt_TSStepCounterOutput statStep = new ProcessModule.SmartBelt_TSStepCounterOutput();
                                        ret = ProcessModule.RcvDataFrmStepCounter(inst, statStep);
                                        if (ret == ProcessModule.ErrorCode_Success) {
                                            cb.processStepStat(statStep);
                                        }
                                    }
                                }
                            }
                        } else if (cb != null) {
                            if (data.characteristic.equals(SMART_BELT_Sensor_Characteristic) && data.value.length == 20) {
                                // 分析呼吸数据
                                ProcessModule.SmartBelt_RawByteData var0 = new ProcessModule.SmartBelt_RawByteData(data.value);
                                ProcessModule.SmartBelt_Setting var1 = new ProcessModule.SmartBelt_Setting(15, 14);
                                ProcessModule.SmartBelt_TSBreathWaveData var2 = new ProcessModule.SmartBelt_TSBreathWaveData();
                                int ret = ProcessModule.SendBreathDataToDataProcMdl(inst, var0, var1, var2); // 发送
                                if (ret == ProcessModule.ErrorCode_Success) {
                                    ProcessModule.SmartBelt_TMdlProcRtInfo rtInfo = new ProcessModule.SmartBelt_TMdlProcRtInfo();
                                    ret = ProcessModule.StartBreathDataProcess(inst, rtInfo);  // 处理
                                    if (ret == ProcessModule.ErrorCode_Success && rtInfo.lPerDataLen > 0) {
                                        ProcessModule.SmartBelt_TSBreathStats statBreath = new ProcessModule.SmartBelt_TSBreathStats(rtInfo.lPerDataLen);
                                        ret = ProcessModule.RcvBreathStatsFrmDataProcMdl(inst, statBreath); // 接收
                                        cb.processBreathStat(statBreath);
                                        if (ret == ProcessModule.ErrorCode_Success) {
                                            cb.processBreathStat(statBreath);
                                        }
                                    }
                                }
                            }

                            node.decode(data.characteristic, data.value);
                            if (node.isOk()) {

                                cb.processBreathData(node);
                                node = new BeltDataNode();  // reset
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } break;

                case BluetoothBLE.MESSAGE_GATT_DISCONNECTED: {
                    //ALog.i("SmartBeltView", device.getBluetoothDeviceMac() + "Disconnected from GATT server");
                    stat.lDisconnectCount++;
                    stat.bConnect = false;
                    stat.tvDisconnect = System.currentTimeMillis();
                    conn = null;
                } break;
            }
        }
    };

    // 重连处理
    private final android.os.Handler handler = new android.os.Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1: {
                    if (conn == null) {
                        //ALog.i("SmartBeltView", device.getBluetoothDeviceMac() + "retry reconnect" + device.getBluetoothDeviceMac());
                        conn = BluetoothBLE.Instance.connect(context, device.getBluetoothDeviceMac(), handlerBluetooth);
                        stat.lRetryCount++;
                    }
                    if (conn != null) {
                        stat.bConnect = true;
                    }
                } break;
            }
        }
    };
}
