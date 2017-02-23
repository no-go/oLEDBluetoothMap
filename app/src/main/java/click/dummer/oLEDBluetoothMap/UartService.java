/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 * 
 * 2016 - modified some parts by Jochen Peters
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package click.dummer.oLEDBluetoothMap;

import android.app.Service;
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
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class UartService extends Service {

    private final static String TAG = UartService.class.getSimpleName();
    private static final int BYTE_LIMIT = 20;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "click.dummer.PotiAndLed.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "click.dummer.PotiAndLed.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "click.dummer.PotiAndLed.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "click.dummer.PotiAndLed.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "click.dummer.PotiAndLed.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART =
            "click.dummer.PotiAndLed.DEVICE_DOES_NOT_SUPPORT_UART";

    public UUID            CCCD;
    public UUID RX_SERVICE_UUID;
    public UUID    RX_CHAR_UUID;
    public UUID    TX_CHAR_UUID;
    public int           byteMS;

    private final static String CCCD_HM10  = "00002902-0000-1000-8000-00805f9b34fb";
    private final static String SERV_HM10  = "0000ffe0-0000-1000-8000-00805f9b34fb";
    private final static String RXUID_HM10 = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final static String TXUID_HM10 = "0000ffe1-0000-1000-8000-00805f9b34fb";
    private final static int  MS_HM10      = 40;

    private final static String CCCD_nRF  = "00002902-0000-1000-8000-00805f9b34fb";
    private final static String SERV_nRF  = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    private final static String RXUID_nRF = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    private final static String TXUID_nRF = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private final static int  MS_nRF      = 20;

    public void setNRF51822(boolean b, boolean isSlow) {
        if (b) {
            CCCD = UUID.fromString(CCCD_nRF);
            RX_SERVICE_UUID = UUID.fromString(SERV_nRF);
            RX_CHAR_UUID = UUID.fromString(RXUID_nRF);
            TX_CHAR_UUID = UUID.fromString(TXUID_nRF);
            byteMS = MS_nRF;
        } else {
            CCCD = UUID.fromString(CCCD_HM10);
            RX_SERVICE_UUID = UUID.fromString(SERV_HM10);
            RX_CHAR_UUID = UUID.fromString(RXUID_HM10);
            TX_CHAR_UUID = UUID.fromString(TXUID_HM10);
            byteMS = MS_HM10;
        }
        if (isSlow) byteMS = 180;
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt );
            	
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is handling for the notification on TX Character of NUS service
        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
        	
            Log.d(TAG, characteristic.getValue().toString() );
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        } else {}
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        UartService getService() {
            return UartService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) return false;
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) return false;
        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) return false;

        if (
                mBluetoothDeviceAddress != null &&
                address.equals(mBluetoothDeviceAddress) &&
                mBluetoothGatt != null
        ) {
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) return false;
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) return;
        mBluetoothGatt.disconnect();
       // mBluetoothGatt.close();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void enableTXNotification() {
    	BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
    	if (RxService == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
    	BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar,true);
        
        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    	
    }
    
    public void writeRXCharacteristic(byte[] val)
    {
        new AsyncTask<byte[], Void, Void>() {
            @Override
            protected Void doInBackground(byte[]... bytes) {
                byte[] value = bytes[0];
                int packs = (int) Math.ceil(
                        (float) value.length / (float) BYTE_LIMIT
                );
                int finish = BYTE_LIMIT;
                int offset;
                for (int i=0; i<packs; i++) {
                    offset = i * BYTE_LIMIT;
                    if ((offset+BYTE_LIMIT) >= value.length) {
                        finish = value.length - offset;
                    }
                    byte[] outputBytes = new byte[finish];
                    System.arraycopy(value, offset, outputBytes, 0, finish);

                    BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
                    BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
                    RxChar.setValue(outputBytes);
                    mBluetoothGatt.writeCharacteristic(RxChar);
                    try {
                        Thread.sleep(byteMS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
                return null;
            }
        }.execute(val);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        return mBluetoothGatt.getServices();
    }
}
