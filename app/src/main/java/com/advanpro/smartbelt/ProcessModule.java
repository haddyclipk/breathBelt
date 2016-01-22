package com.advanpro.smartbelt;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

// 算法库
public class ProcessModule {

    public static final int ErrorCode_Success = 0;
    public static final int ErrorCode_InitializationFailed = 1;  // 初始化失败
    public static final int ErrorCode_MemoryAllocationFailed = 2; // 内存分配失败
    public static final int ErrorCode_InvalidParameters = 3; // 参数无效
    public static final int ErrorCode_DataProcFailed = 4; // 数据处理失败
    public static final int ErrorCode_DataAnlysisFailed = 5; // 数据包分析失败
    public static final int ErrorCode_DifferentialFailed = 6; // 数据微分失败
    public static final int ErrorCode_WriteRwDataToBufFailed = 7; // 原始数据缓存失败
    public static final int ErrorCode_WriteDfDataToBufFailed = 8; // 微分数据缓存失败
    public static final int ErrorCode_BreathAnalysingFailed = 9; // 呼吸解析失败

    static {
        System.loadLibrary("SmartBelt_DataProcessModule");
    }

    //=============================================================
    // 呼吸数据处理
    //=============================================================

    // 初始化算法库，完成内存等资源的分配
	// inst: 实例编号，支持多设备连接时使用，范围[0,255]
    public static native int InitDataProcMdl(int inst, SmartBelt_TMdlInitInfo pSInitInfo);
    //iBufferSize = 0: Using default size to allocate Resource.

    // 将接收到的字节数据发送到算法库中
    //baDataSeries: Byte Array.
    //input: baDataArray:byte Array from Hardware which length is MMAXSENSORBYTENUM(20), iArrayLen is MMAXSENSORBYTENUM(20).
    //input: Need Breath Wave Data: pSWaveData:the Pointer of SmartBelt_WaveData.Otherwise, daBrthWave = NULL or iWaveArryLen = 0.
    //input: iDAQPrecision:[11 16];default:14.
    //input: iSmoothOrder:[1 20];default:1.
    public static native int SendBreathDataToDataProcMdl(int inst, SmartBelt_RawByteData pSRawBData, SmartBelt_Setting pSSetting, SmartBelt_TSBreathWaveData pSWaveData);

    // 启动呼吸数据处理
    public static native int StartBreathDataProcess(int inst, SmartBelt_TMdlProcRtInfo pSPrcRtInfo);

    // 读取算法库中产生的校准呼吸波形和呼吸统计数据
    public static native int RcvBreathStatsFrmDataProcMdl(int inst, SmartBelt_TSBreathStats SBrthStats);

    // 清空算法库缓存中的数据
    public static native int ResetProcMdl(int inst);

    // 退出处理并释放初始化时所申请的资源
    public static native int ExitDataProcMdl(int inst);

    //=============================================================
    // 记步数据处理
    //=============================================================

    public static native int InitStepCounter(int inst);
    public static native int ResetStepCounter(int inst);
    public static native int SetParamsToStepCounter(int inst, SmartBelt_TSStepCounterParams pSCParams);
    public static native int SendDataToStepCounter(int inst, SmartBelt_TSStepCounterPkt pSCDataPkt);
    public static native int StartDataAnalyze(int inst);
    public static native int RcvDataFrmStepCounter(int inst, SmartBelt_TSStepCounterOutput pSCOutput);

    //=============================================================
    // 呼吸数据数据结构
    //=============================================================
    public static final int MMAXSENSORBYTENUM  = 20;
    public static final int MMAXSENSORDATANUM = 10;
    public static final int MTIMELENGTH = 19;
    public static final int MMAXDATABUFFERLEN = 1000;

    public static class SmartBelt_Time
    {
        public int iYear;
        public int iMonth;  // 1~12
        public int iDay;
        public int iHour;
        public int iMinute;
        public int iSecond;
        public int iMilliSecond;

        public Date getDate() {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.set(iYear, iMonth-1, iDay, iHour, iMinute, iSecond);
            Date dt = new Date(calendar.getTimeInMillis() + iMilliSecond);
            return dt;
        }

        public void setDate(Date date) {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(date);
            iYear = calendar.get(Calendar.YEAR);
            iMonth = (byte)calendar.get(Calendar.MONTH) + 1;
            iDay = (byte)calendar.get(Calendar.DATE);
            iHour = (byte)calendar.get(Calendar.HOUR_OF_DAY);
            iMinute = (byte)calendar.get(Calendar.MINUTE);
            iSecond = (byte)calendar.get(Calendar.SECOND);
            iMilliSecond = calendar.get(Calendar.MILLISECOND);
        }

        public long getTotalSecond() {
            long lTotal = 0;
            lTotal += iYear * 31536000;
            lTotal += iMonth * 2592000;
            lTotal += iDay * 86400;
            lTotal += iHour * 3600;
            lTotal += iMinute * 60;
            lTotal += iSecond;
            lTotal += iMilliSecond/1000;
            return lTotal;
        }
    }

    public static class SmartBelt_RawByteData
    {
        // 硬件所上传的字节数据，最多20字节
        public byte[] baRawData = new byte[MMAXSENSORBYTENUM];
        // 实际数据长度，最大20
        public int iDataLen;
        public SmartBelt_RawByteData(byte[] data) {
            System.arraycopy(data, 0, baRawData, 0, 20);
            iDataLen = 20;
        }
    }

    public static class SmartBelt_Setting
    {
        // 数据采集精度，硬件侧固定以16bit精度采集数据，算法库根据该参数做精度调整，设置范围[11,16]，默认值为12
        public int iDAQPrecision;
        // 平滑阶数，对呼吸波形数据做平滑处理，设置范围[1,20]，1不做平滑，其他值做相应阶数的平滑
        public int iSmoothOrder;
        public SmartBelt_Setting(int iDAQPrecision, int iSmoothOrder) {
            this.iDAQPrecision = iDAQPrecision;
            this.iSmoothOrder = iSmoothOrder;
        }
    }

    public static class SmartBelt_TSBreathWaveData
    {
        // 平滑之后的原始呼吸波形数据，MMAXSENSORDATANUM=10，最长10个数据点
        public double[] daBrthWave = new double[MMAXSENSORDATANUM];
        // 实际原始呼吸波形数据长度.最大为10
        public int iBrthWaveLen;
        // 呼吸波形的斜率变化率数据. MMAXSENSORDATANUM=10，最长10个数据点
        public double[] daBrthSlopeVaryRto = new double[MMAXSENSORDATANUM];
        // 实际原始呼吸斜率变化率数据长度.最大为10
        public int iSlopVrRtoLen;
    }

    public static class SmartBelt_TSBreathStats
    {
        public SmartBelt_TSBreathStats(long lPerDataLen) {
            this.daPerDataBuf = new double[(int)lPerDataLen];
            this.lPerDataLen = lPerDataLen;
        }

        // 呼吸记录序列号，从0开始
        public long lBreathSN;

        // 该呼吸在呼吸波形数据序列中的前波谷索引
        public long lPreTroughIndex;
        // 该呼吸在呼吸波形数据序列中的波峰索引
        public long lPeakIndex;
        // 该呼吸在呼吸波形数据序列中的后波谷索引
        public long lPostTroughIndex;

        /***********Current Breath Data***************/
        // 该呼吸的吸时长
        public double dInspiratoryTime;
        // 该呼吸的呼时长
        public double dExspiratoryTime;
        // 该呼吸的吸呼时长比.
        public double dRtoOfIns2Exs;
        // 该呼吸的持续时长
        public double dBrthTime;

        // 该呼吸的幅度，为容积变化百分比数值.
        public double dAmplitude;

        // 该呼吸的瞬时频率.
        public double dInstantFreq;

        // 该呼吸的起始时间
        public SmartBelt_Time SStartTime = new SmartBelt_Time();
        // 该呼吸的终止时间
        public SmartBelt_Time SEndTime = new SmartBelt_Time();
        // 该呼吸的持续时间
        public SmartBelt_Time SSpanTime = new SmartBelt_Time();

        // 该呼吸的校准波形数据，外部分配长度查看SmartBelt_TMdlProcRtInfo.lPerDataLen
        public double[] daPerDataBuf;
        // 该呼吸校准呼吸波形数据的长度
        public long lPerDataLen;

        /*****************************************/
        // 所有呼吸的统计信息
        // 呼吸总计数
        public long lTotalBrthCnt;
        // 呼吸总时间
        public double dTotalBrthTime;
        // 平均呼吸时长
        public double dAvgBrthTime;
        // 总吸时长
        public double dTotalInsTime;
        // 平均吸时长
        public double dAvgInsTime;
        // 总呼时长
        public double dTotalExsTime;
        // 平均呼时长
        public double dAvgExsTime;
        // 总吸呼时长比.
        public double dTotalRtoOfIns2Exs;
        // 平均呼吸时长比
        public double dAvgRtoOfIns2Exs;
        // 平均呼吸频率
        public double dAvgBrthFreq;
        // 总幅度
        public double dTotalBrthAmp;
        // 平均呼吸幅度
        public double dAvgBrthAmp;
    }

    public static class SmartBelt_TMdlInitInfo
    {
        // 缓存大小，以数据点为单位，而非以字节单位，设置0表示默认值
        int iBufferSize;
    }

    public static class SmartBelt_TMdlProcRtInfo
    {
        // 返回算法库中校准呼吸波形的数据长度
        public long lPerDataLen;
    }

    //=============================================================
    // 记步数据结构
    //=============================================================
    public static final int MMAXSCDATAPACKETLEN = 20;
    public static final int MREALSCDATAPACKETLEN = 13;

    public static class SmartBelt_TEStepCounterStatus
    {
        static final int SCStart = 0;
        static final int SCPause = 1;
        static final int SCContinue = 2;
        static final int SCStop = 3;
    };

    public static class SmartBelt_TEStepCounterBool
    {
        static final int SCBTrue = 0;
        static final int SCBFalse = 1;
    }

    public static class SmartBelt_TSStepCounterParams
    {
        public double dParamHeight = 173;   // 身高，cm，默认值173cm
        public double dParamWeight = 65;    // 体重，kg，默认值65kg

        public double dParamWlkFactor = 0.45;        // 步行因子，默认值0.45
        public double dParamRnFactor = 0.5;          // 跑步因子，默认值0.50
        public double dParamCalorieFacotor = 1.036;  // 卡路里消耗因子，默认值1.036
    }

    public static class SmartBelt_TSStepCounterPkt
    {
        public SmartBelt_TSStepCounterPkt(int sn, byte[] data, Date tv) {
            System.arraycopy(data, 0, caDataPacket, 0, MREALSCDATAPACKETLEN);
            iDataPktLen = MREALSCDATAPACKETLEN;
            SDataTime.setDate(tv);
            eblTime = SmartBelt_TEStepCounterBool.SCBTrue;
            if (sn == 0)
                eSCStats = SmartBelt_TEStepCounterStatus.SCStart;  // 开始计步使用SCStart
            else
                eSCStats = SmartBelt_TEStepCounterStatus.SCContinue;
        }

        // Packet.
        public byte[] caDataPacket = new byte[MMAXSCDATAPACKETLEN];  // 硬件数据包
        public int iDataPktLen = MREALSCDATAPACKETLEN; // 数据包长度，固定为13 Bytes

        // Step Counter Status.
        public int eSCStats = SmartBelt_TEStepCounterStatus.SCContinue; // SmartBelt_TEStepCounterStatus
        public SmartBelt_Time SDataTime = new SmartBelt_Time();  // App提供的数据包对应时间，eblTime为SCBTrue则有效
        public int eblTime = SmartBelt_TEStepCounterBool.SCBTrue;  // SmartBelt_TEStepCounterBool
    }

    public static class SmartBelt_TSStepCounterOutput
    {
        //Time
        public SmartBelt_Time SStartTime = new SmartBelt_Time();  // 本次运动的起始时间
        public SmartBelt_Time SEndTime = new SmartBelt_Time();   // 本次运动的终止时间
        public SmartBelt_Time SDuration = new SmartBelt_Time();  // 本次运动的持续时长，与时间格式一致

        //Walking Data.
        public long lTotalWlkStps;       // 总走步数
        public double dTotalWlkDist;     // 总走步距离，单位km
        public double dTotalWlkCalorie;  // 走步消耗的卡路里，卡路里需要提供身高+体重，单位kcal

        //Running Data.
        public long lTotalRnStps;        // 总跑步数
        public double dTotalRnDist;      // 跑步总距离，单位km
        public double dTotalRnCalorie;   // 跑步消耗的卡路里，卡路里需要提供身高+体重，单位kcal

        //Statistics.
        public long lTotalStps;          // 总步数
        public double dTotalDist;        // 总距离，单位km
        public double dTotalCalorie;     // 总共消耗的卡路里，单位kcal
        public double dAvgRate;          // 平均速度，每千米花费的时间，单位s/Km
    }
}
