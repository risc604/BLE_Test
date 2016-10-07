package com.example.tomcat.ble_test;

import android.app.Service;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.Random;

public class BluetoothLeService extends Service
{
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private static final ParcelUuid MLC_BLE_CHAR = ParcelUuid.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid MLC_BLE_READ = ParcelUuid.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid MLC_BLE_WRITE = ParcelUuid.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid CLIENT_CHAR_CONFIG = ParcelUuid.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothGatt mGatt;

    public BluetoothLeService()
    {
    }

    public class LocalBinder extends Binder
    {
        BluetoothLeService getService()
        {
            Log.d(TAG, "getService() LocalBinder");
            return BluetoothLeService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        Log.d(TAG, "onBind() binding service: " + mBinder.toString());
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        //super.onCreate();
        Log.d(TAG, "onCreate() create service ...");
    }

    public int getRandomNumber()
    {
        return (new Random().nextInt(100));
        //return mGenerator.nextInt(100);
    }

    /*
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState)
            {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i("gattCallback", "STATE_CONNECTED");
                    gatt.discoverServices();
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.e("gattCallback", "STATE_DISCONNECTED");
                    break;

                default:
                    Log.e("gattCallback", "STATE_OTHER");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            List<BluetoothGattService> services = gatt.getServices();
            Log.i("onServicesDiscovered", services.toString());
            //gatt.readCharacteristic(services.get(1).getCharacteristics().get(0));
            for (BluetoothGattService gattServiceIdx : services)
            {
                Log.i("onSrvDscver", "[ " + gattServiceIdx + " ]");
                List<BluetoothGattCharacteristic> mGattCharacteristics =
                        gattServiceIdx.getCharacteristics();
                for (BluetoothGattCharacteristic mCharacteristic : mGattCharacteristics)
                {
                    Log.i("onSrvDscver", "Characteristic: " + mCharacteristic.getUuid());
                    if (MLC_BLE_READ.getUuid().equals(mCharacteristic.getUuid()))
                    {
                        //setCharacteristicNotification(gatt,mCharacteristic, true);
                        gatt.setCharacteristicNotification(mCharacteristic, true);
                        BluetoothGattDescriptor descriptor =
                                mCharacteristic.getDescriptor(CLIENT_CHAR_CONFIG.getUuid());
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status)
        {
            Log.i("onCharacteristicRead", characteristic.toString());
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                //showMesg(characteristic.getValue());
                gatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic)
        {
            //super.onCharacteristicChanged(gatt, characteristic);
            //showMesg(characteristic.getValue());
        }
    };
    */
}
