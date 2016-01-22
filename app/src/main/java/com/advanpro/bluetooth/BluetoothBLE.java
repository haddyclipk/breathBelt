package com.advanpro.bluetooth;

import java.util.List;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 蓝牙服务
 */
public class BluetoothBLE {

	public static final int MESSAGE_GATT_CONNECTED = 1001;
	public static final int MESSAGE_GATT_DISCONNECTED = 1002;
	public static final int MESSAGE_DATA_AVAILABLE = 1003;
	public static final int MESSAGE_DEVICE_DISCOVER = 1004;
	public static final int MESSAGE_DATA_REQUEST = 1005;
	public static BluetoothBLE Instance = null;  // 唯一实例

	public static boolean init(Activity view) {
		try
		{
			Instance = new BluetoothBLE();
			// For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
			if (Instance.mBluetoothManager == null)
			{
				Instance.mBluetoothManager = (BluetoothManager)view.getSystemService(Context.BLUETOOTH_SERVICE);
				if (Instance.mBluetoothManager == null) {
					Log.e(TAG, "Unable to initialize BluetoothManager.");
					Instance = null;
					return false;
				}
			}

			Instance.mBluetoothAdapter = Instance.mBluetoothManager.getAdapter();
			if (Instance.mBluetoothAdapter == null)
			{
				Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
				Instance = null;
				return false;
			}
		}
		catch (Exception Ex)
		{
			Log.e(TAG, "打开蓝牙适配器异常.");
			Instance = null;
			return false;
		}

		if (!Instance.mBluetoothAdapter.isEnabled())
		{
			// 请求用户打开蓝牙
			final int REQUEST_ENABLE_BT = 1;// 表示蓝牙LE允许访问
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			view.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		return true;
	}

	public static class DeviceInfo {
		private String mBluetoothDeviceName = new String();
		private String mBluetoothDeviceMac = new String();
		private String mBluetoothDeviceType = new String();
		public boolean bUsed = false;

		public	DeviceInfo(String bluetoothDeviceName ,String bluetoothDeviceMac) {
			if (bluetoothDeviceName != null) this.mBluetoothDeviceName = bluetoothDeviceName;
			if (bluetoothDeviceMac != null) this.mBluetoothDeviceMac = bluetoothDeviceMac;
		}

		public String getBluetoothDeviceName(){ return mBluetoothDeviceName; }
		public void setBluetoothDeviceName(String bluetoothDeviceName) {
			if (bluetoothDeviceName != null) this.mBluetoothDeviceName = bluetoothDeviceName;
		}

		public String getBluetoothDeviceMac(){ return mBluetoothDeviceMac; }
		public void setBluetoothDeviceMac(String bluetoothDeviceMac) {
			if (bluetoothDeviceMac != null) this.mBluetoothDeviceMac = bluetoothDeviceMac;
		}

		public String getBluetoothDeviceType(){ return mBluetoothDeviceType; }
	}

	public static class DeviceData {
		public UUID service;
		public UUID characteristic;
		public byte[] value;
		public long tvMills;
		public DeviceData(UUID service, UUID characteristic, byte[] value) {
			this.service = service;
			this.characteristic = characteristic;
			this.value = value;
			this.tvMills = System.currentTimeMillis();
		}
	}

	/**
	 * 启动蓝牙搜索
	 */
	public void scanBluetoothBLE(android.os.Handler handler) {
		if (mBluetoothAdapter == null) {
			Log.w(TAG, "scanBluetoothBLE：BluetoothAdapter not initialized .");
			return;
		}
		if (!bIsStartScanBBluetooth) {
			bIsStartScanBBluetooth = true;
			ScanCallBack.handler = handler;
			mBluetoothAdapter.startLeScan(ScanCallBack);
		}
	}

	/**
	 * 停止蓝牙搜索
	 */
	public void stopBluetoothBLEScan() {
		if (bIsStartScanBBluetooth) {
			ScanCallBack.handler = null;
			mBluetoothAdapter.stopLeScan(ScanCallBack);
			bIsStartScanBBluetooth = false;
		}
	}

	public boolean isScanBluetooth() {
		return bIsStartScanBBluetooth;
	}

	private SmartDevicesScanCallBack ScanCallBack = new SmartDevicesScanCallBack();
	private class SmartDevicesScanCallBack implements BluetoothAdapter.LeScanCallback
	{
		public android.os.Handler handler;
		@Override
		public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
			DeviceInfo BDIMDevice = new DeviceInfo(device.getName(), device.getAddress());
			android.os.Message msg = new android.os.Message();
			msg.what = MESSAGE_DEVICE_DISCOVER;
			msg.obj = BDIMDevice;
			handler.sendMessage(msg);
		}
	}

	/**
	 * 连接蓝牙设备
	 * @param address Mac地址
	 */
	public BluetoothConnect connect(Context context, String address, android.os.Handler handler) {
		if (mBluetoothAdapter == null || address == null) {
			Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
			return null;
		}

		BluetoothConnect conn = new BluetoothConnect();
		conn.handler = handler;
		conn.mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(address);
		if (conn.mBluetoothDevice == null) {
			Log.w(TAG, " BluetoothDevice not found.  Unable to connect.");
			return null;
		}

		conn.mBluetoothGatt = conn.mBluetoothDevice.connectGatt(context, false, conn.siGattCallBack);
		if (conn.mBluetoothGatt == null) {
			Log.i(TAG, "Trying to create a new connection failed.");
			return null;
		}

		return conn;
	}

	public class BluetoothConnect
	{
		private BluetoothDevice mBluetoothDevice;
		private BluetoothGatt mBluetoothGatt;
		private android.os.Handler handler = null;
		private BluetoothConnect() { }

		/**
		 * Disconnects an existing connection or cancel a pending connection. The
		 * disconnection result is reported asynchronously through the
		 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
		 * callback.
		 */
		public void disconnect() {
			if (mBluetoothGatt != null) {
				mBluetoothGatt.disconnect();
				mBluetoothGatt.close();
				mBluetoothDevice = null;
				mBluetoothGatt = null;
			}
		}

		/**
		 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
		 * result is reported asynchronously through the
		 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
		 * callback. 读取蓝牙数据
		 */
		public boolean readCharacteristic(UUID service, UUID characteristic) {
			if (mBluetoothGatt == null) {
				Log.w(TAG, "BluetoothAdapter not initialized");
				return false;
			}

			BluetoothGattService Gattservice = getService(service);
			if (Gattservice == null) {
				Log.i(TAG, "BluetoothGatt Service is not include SMART BELT SERVICE");
				return false;
			}

			BluetoothGattCharacteristic c = Gattservice.getCharacteristic(characteristic);
			mBluetoothGatt.readCharacteristic(c);
			return true;
		}

		public boolean writeCharacteristic(UUID service, UUID characteristic, byte[] value) {
			if (mBluetoothGatt == null) {
				Log.w(TAG, "BluetoothAdapter not initialized");
				return false;
			}

			BluetoothGattService Gattservice = getService(service);
			if (Gattservice == null) {
				Log.i(TAG, "BluetoothGatt Service is not include SERVICE " + service.toString());
				return false;
			}

			BluetoothGattCharacteristic c = Gattservice.getCharacteristic(characteristic);
			c.setValue(value);
			mBluetoothGatt.writeCharacteristic(c);
			return true;
		}

		/**
		 * Enables or disables notification on a give characteristic.
		 * @param characteristic Characteristic to act on.
		 * @param enabled If true, enable notification. False otherwise.
		 */
		public boolean setCharacteristicNotification(UUID service, UUID characteristic, boolean enabled) {
			if (mBluetoothAdapter == null || mBluetoothGatt == null) {
				Log.w(TAG, "BluetoothAdapter  not initialized or BluetoothGatt is null");
				return false;
			}

			BluetoothGattService Gattservice = getService(service);
			if (Gattservice == null) {
				Log.i(TAG, "BluetoothGatt Service is not include SERVICE " + service.toString());
				return false;
			}

			BluetoothGattCharacteristic c = Gattservice.getCharacteristic(characteristic);
			boolean bsetsucceed = mBluetoothGatt.setCharacteristicNotification(c, enabled);
			if (bsetsucceed) {
				Log.i(TAG, "succeed to notify the Characteris " + characteristic.toString());
			} else {
				Log.w(TAG, "failed to notify the Characteris " + characteristic.toString());
			}

			BluetoothGattDescriptor descriptor = c.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			return mBluetoothGatt.writeDescriptor(descriptor);
		}

		private BluetoothGattService getService(UUID service) {
			BluetoothGattService Gattservice = null;
			List<BluetoothGattService> gattServicesgattServices = mBluetoothGatt.getServices();

			for (BluetoothGattService gattService : gattServicesgattServices) {
				if (service == null || service.equals(gattService.getUuid())) {
					Gattservice = gattService;
					break;
				}
			}

			return Gattservice;
		}

		/**
		 * 蓝牙回调函数，判别蓝牙的获取，连接。断开，并接受数据，发送广播
		 */
		private final BluetoothGattCallback siGattCallBack = new BluetoothGattCallback() {
			@Override
			public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
				// super.onConnectionStateChange(gatt, status, newState);
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					Log.i(TAG, "Connected to GATT server.");
					boolean bDisSerSuceed = mBluetoothGatt.discoverServices();  // 搜索支持的服务
					Log.i(TAG, "Attempting to start service discovery:" + bDisSerSuceed);
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					Log.i(TAG, "Disconnected from GATT server.");
					mBluetoothGatt.close();
					handler.sendEmptyMessage(MESSAGE_GATT_DISCONNECTED);
				}
			}

			@Override
			public void onServicesDiscovered(BluetoothGatt gatt, int status) {
				// super.onServicesDiscovered(gatt, status);
				handler.sendEmptyMessage(MESSAGE_GATT_CONNECTED);
			}

			/**
			 * 向蓝牙读取数据的回调函数
			 */
			@Override
			public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, int status) {
				// super.onCharacteristicRead(gatt, characteristic, status);
				if (status == BluetoothGatt.GATT_SUCCESS) {
					android.os.Message msg = new android.os.Message();
					msg.what = MESSAGE_DATA_AVAILABLE;
					msg.obj = new DeviceData(characteristic.getService().getUuid(), characteristic.getUuid(), characteristic.getValue());
					handler.sendMessage(msg);
				}
				handler.sendEmptyMessage(MESSAGE_DATA_REQUEST);
			}

			/**
			 * 接收蓝牙传送数据的回调函数
			 */
			@Override
			public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
				// super.onCharacteristicChanged(gatt, characteristic);
				android.os.Message msg = new android.os.Message();
				msg.what = MESSAGE_DATA_AVAILABLE;
				msg.obj = new DeviceData(characteristic.getService().getUuid(), characteristic.getUuid(), characteristic.getValue());
				handler.sendMessage(msg);
				handler.sendEmptyMessage(MESSAGE_DATA_REQUEST);
			}

			/**
			 * 打开通道之前一定要先添加描述信息，添加描述信息后的回调函数
			 */
			@Override
			public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
				// super.onDescriptorWrite(gatt, descriptor, status);
				handler.sendEmptyMessage(MESSAGE_DATA_REQUEST);
			}
		};
	}

	private BluetoothBLE() {}
	private static final String TAG = BluetoothBLE.class.getSimpleName();
	private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private boolean bIsStartScanBBluetooth = false;  // 标识蓝牙处于扫描状态
}
