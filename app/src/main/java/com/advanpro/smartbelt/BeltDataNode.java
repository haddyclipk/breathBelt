package com.advanpro.smartbelt;

import java.text.DecimalFormat;
import java.util.UUID;


public class BeltDataNode {

    public final static int counter = 10;
    public static int ACC_RANGE = 0;

    public long tv = 0;  // 时间
    public int[] beltsensordata = null;  // 传感器数据,每秒10个
    public double[] beltACCXdata = null; // 三轴加速度X轴数据,每秒10个
    public double[] beltACCYdata = null; // 三轴加速度X轴数据,每秒10个
    public double[] beltACCZdata = null; // 三轴加速度X轴数据,每秒10个

    public boolean isOk() {
        if (beltsensordata != null && beltACCXdata != null && beltACCYdata != null && beltACCZdata != null)
            return true;
        return false;
    }

    public void decode(UUID uuid, byte[] data) {
        if (tv == 0) this.tv = System.currentTimeMillis();
        if (uuid.equals(SmartBeltBLE.SMART_BELT_Sensor_Characteristic))
            beltsensordata = analybeltdata(data);
        else if (uuid.equals(SmartBeltBLE.SMART_BELT_ACCX_Characteristic))
            beltACCXdata = analyACCdata(data);
        else if (uuid.equals(SmartBeltBLE.SMART_BELT_ACCY_Characteristic))
            beltACCYdata = analyACCdata(data);
        else if (uuid.equals(SmartBeltBLE.SMART_BELT_ACCZ_Characteristic))
            beltACCZdata = analyACCdata(data);
        else if (uuid.equals(SmartBeltBLE.SMART_BELT_ACC_RANGE_Characteristic))
            ACC_RANGE = data[0];
    }

    public void store(long db) {
        /*
        File file = new File(Config.AppSoreDir, strFileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                String title = "number\ttime\tSensor\tACCX\tACCY\tACCZ\n";
                FileOutputStream out = new FileOutputStream(file, true);
                out.write(title.getBytes());
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (file.exists()) {
            try {
                FileWriter fileWritter = new FileWriter(file, true);
                BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (int i = 0; i < counter; ++i, ++row) {
                    String strData = String.valueOf(row) + "\t";
                    strData += sDateFormat.format(tv) + "\t";
                    strData += String.valueOf(beltsensordata[i]) + "\t";
                    strData += String.valueOf(beltACCXdata[i]) + "\t";
                    strData += String.valueOf(beltACCYdata[i]) + "\t";
                    strData += String.valueOf(beltACCZdata[i]) + "\n";
                    bufferWritter.write(strData);
                    bufferWritter.flush();
                }
                bufferWritter.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return row;
        */
    }

    /**
     * @param BCharacteristic 从蓝牙获取过来的20个字节
     */
    private int[] analybeltdata(final byte[] BCharacteristic) {
        int[] datas = new int[counter];
        datas[0] = shortUnsignedAtOffset(BCharacteristic, 0);
        datas[1] = shortUnsignedAtOffset(BCharacteristic, 2);
        datas[2] = shortUnsignedAtOffset(BCharacteristic, 4);
        datas[3] = shortUnsignedAtOffset(BCharacteristic, 6);
        datas[4] = shortUnsignedAtOffset(BCharacteristic, 8);
        datas[5] = shortUnsignedAtOffset(BCharacteristic, 10);
        datas[6] = shortUnsignedAtOffset(BCharacteristic, 12);
        datas[7] = shortUnsignedAtOffset(BCharacteristic, 14);
        datas[8] = shortUnsignedAtOffset(BCharacteristic, 16);
        datas[9] = shortUnsignedAtOffset(BCharacteristic, 18);
        return datas;
    }

    private double[] analyACCdata(final byte[] BCharacteristic) {
        double[] datas = new double[counter];
        datas[0] = shortAtOffset(BCharacteristic, 0);
        datas[1] = shortAtOffset(BCharacteristic, 2);
        datas[2] = shortAtOffset(BCharacteristic, 4);
        datas[3] = shortAtOffset(BCharacteristic, 6);
        datas[4] = shortAtOffset(BCharacteristic, 8);
        datas[5] = shortAtOffset(BCharacteristic, 10);
        datas[6] = shortAtOffset(BCharacteristic, 12);
        datas[7] = shortAtOffset(BCharacteristic, 14);
        datas[8] = shortAtOffset(BCharacteristic, 16);
        datas[9] = shortAtOffset(BCharacteristic, 18);
        return datas;
    }

    /**
     * 把数据从byte型转化为无符号Interger类型，同时计算数据
     */
    private static Integer shortUnsignedAtOffset(final byte[] byteSensordata, int offset) {
        int lowerByte = byteSensordata[offset] & 0xff;
        int upperByte = byteSensordata[offset + 1] & 0xff; // Note:
        int Value = (upperByte << 8) + lowerByte; // interpret MSB
        // as unsigned.
        return Value * 15000 / (65535 - Value);
    }

    private static double shortAtOffset(final byte[] byteACCdata, int offset) {
        int lowerByte = byteACCdata[offset];
        int upperByte = byteACCdata[offset + 1]; // Note: // interpret MSB // as
        DecimalFormat df = new DecimalFormat("#.##");
        double result_value = (double) ((upperByte << 8) + lowerByte) * ACC_RANGE / (double) 32768;
        return Double.parseDouble(df.format(result_value));
    }
}
