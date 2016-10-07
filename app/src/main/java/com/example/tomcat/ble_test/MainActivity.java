package com.example.tomcat.ble_test;

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
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private static final ParcelUuid MLC_BLE_CHAR = ParcelUuid.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid MLC_BLE_READ = ParcelUuid.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid MLC_BLE_WRITE = ParcelUuid.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid CLIENT_CHAR_CONFIG = ParcelUuid.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 1000 * 10;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private int lineCount=0;

    TextView    textView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.tvShow);
        textView.setText("");

        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            if (Build.VERSION.SDK_INT >= 21)
            {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<ScanFilter>();
            }
            scanLeDevice(true);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        //textView.setText("");
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
        {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy()
    {
        if (mGatt == null)
        {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == Activity.RESULT_CANCELED)
            {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        //textView.setText("");
        scanLeDevice(true);
        //mGatt.close();
        return super.onTouchEvent(event);
    }

    private void scanLeDevice(final boolean enable)
    {
        if (enable)
        {
            mHandler.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    if (Build.VERSION.SDK_INT < 21)
                    {
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }
                    else
                    {
                        mLEScanner.stopScan(mScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            if (Build.VERSION.SDK_INT < 21)
            {
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            }
            else
            {
                mLEScanner.startScan(filters, settings, mScanCallback);
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT < 21)
            {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
            else
            {
                mLEScanner.stopScan(mScanCallback);
            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice, result.getRssi());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            for (ScanResult sr : results)
            {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord)
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Log.i("onLeScan", device.toString());
                    connectToDevice(device, rssi);
                }
            });
        }
    };

    public void connectToDevice(BluetoothDevice device, int rssi)
    {
        if (mGatt == null)
        {
            showMesg(device.getName() + "\t " + device.getAddress() + "\t " + rssi);
            mGatt = device.connectGatt(this, false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
        }
    }

    public void showMesg(String data)
    {
        if(lineCount > 24)
        {
            textView.setText("");
            lineCount = 0;
        }
        textView.append(data + "\r\n");
        lineCount ++;
    }

    public void showMesg(byte[] data)
    {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for (byte bIndex : data)
        {
            stringBuilder.append(String.format("%02X", bIndex));
        }

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if(lineCount > 24)
                {
                    textView.setText("");
                    lineCount = 0;
                }
                //textView.append("\r\nHello UI \r\n");

                if (stringBuilder.length() > 0)
                {
                    textView.append("SZ:" + stringBuilder.length()
                            + ", data: " + stringBuilder + "\r\n");
                }
                else
                {
                    textView.setText("null, No data.\r\n");
                }

                lineCount += 3;
            }
        });

    }

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
                showMesg(characteristic.getValue());
                gatt.disconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic)
        {
            //super.onCharacteristicChanged(gatt, characteristic);
            showMesg(characteristic.getValue());
        }
    };
}
