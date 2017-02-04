package click.dummer.oLEDBluetoothMap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class MainActivity extends Activity implements LocationListener {
    public static final String PROJECT_LINK = "https://github.com/no-go/oLEDBluetoothMap";
    public static final String FLATTR_ID = "o6wo7q";
    public String FLATTR_LINK;

    private Context ctx;
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int IWIDTH = 96;
    private static final int IHIGHT = 64;
    private static final int REQUEST_ENABLE_BT = 2;
    private int zoom = 14;
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect;
    private EditText addrField;
    private CheckBox autoCb;

    private String PROVIDER = LocationManager.GPS_PROVIDER;
    private LocationManager mLocationManager;
    private MapView map;
    private ImageView swMap;
    Canvas canvas;
    private Bitmap bitmap16b;

    private int mInterval = 30000;
    private Handler mHandler;

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                whereAmI();
            } finally {
                if (autoCb.isChecked()) mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            FLATTR_LINK = "https://flattr.com/submit/auto?fid="+FLATTR_ID+"&url="+
                    java.net.URLEncoder.encode(PROJECT_LINK, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_flattr:
                Intent intentFlattr = new Intent(Intent.ACTION_VIEW, Uri.parse(FLATTR_LINK));
                startActivity(intentFlattr);
                break;
            case R.id.action_project:
                Intent intentProj= new Intent(Intent.ACTION_VIEW, Uri.parse(PROJECT_LINK));
                startActivity(intentProj);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ctx = getApplicationContext();
        mHandler = new Handler();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        addrField = (EditText) findViewById(R.id.addrText);
        addrField.setText(
                PreferenceManager.getDefaultSharedPreferences(ctx).getString("devAddr", "")
        );

        autoCb = (CheckBox) findViewById(R.id.autoCheck);
        autoCb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (autoCb.isChecked()) mStatusChecker.run();
            }
        });


        map = (MapView) findViewById(R.id.map);
        swMap = (ImageView) findViewById(R.id.swMap);
        map.setTileSource(TileSourceFactory.MAPNIK);
        //map.setBuiltInZoomControls(true);
        //map.setMultiTouchControls(true);

        service_init();

        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {

                        String deviceAddress = addrField.getText().toString().trim();
                        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putString("devAddr",deviceAddress).apply();
                        mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
                        ((TextView) findViewById(R.id.rssival)).setText(mDevice.getName() + " - connecting");
                        mService.connect(deviceAddress);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
                swMap.setImageBitmap(getViewBitmap(map));
            }
        });

        swMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!btnConnectDisconnect.getText().equals("Connect")) {
                    swMap.setImageBitmap(getViewBitmap(map));
                    sendImg();
                }
            }
        });

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        for (String pro : mLocationManager.getAllProviders()) {
            Log.d(TAG, pro);
        }
        mLocationManager.removeUpdates(this);
    }

    public void zoomIn(View v) {
        zoom++;
        map.getController().setZoom(zoom);
        swMap.setImageBitmap(getViewBitmap(map));
    }

    public void zoomOut(View v) {
        zoom--;
        map.getController().setZoom(zoom);
        swMap.setImageBitmap(getViewBitmap(map));
    }

    public void whereAmI() {
        mLocationManager.requestLocationUpdates(PROVIDER, 0, 0, this);
    }

    public void locReq(View v) { whereAmI(); }

    public void sendImg() {
        byte[] value = new byte[2*IWIDTH*IHIGHT];
        int bCount = 0;
        byte[] dummy;
        byte r,g,b, gUpper, gLower;
        for (int y=0; y<IHIGHT; y++) {
            for (int x=0; x<IWIDTH; x++) {
                dummy = ByteBuffer.allocate(4).putInt(bitmap16b.getPixel(x, y)).array();
                r = (byte) (dummy[1] >> 3);
                g = (byte) (dummy[2] >> 2);
                b = (byte) (dummy[3] >> 3);
                gUpper = (byte) (g >> 3);
                gLower = (byte) (g << 5);
                r = (byte) (r << 3);
                value[bCount] = (byte) (r + gUpper);
                value[bCount+1] = (byte) (gLower + b);
                bCount+=2;
            }
        }
        try {
            mService.writeRXCharacteristic(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        String action;

        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();

            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        ((TextView) findViewById(R.id.rssival)).setText(mDevice.getName() + " - ready");
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        ((TextView) findViewById(R.id.rssival)).setText("Not Connected");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();

                        // try reconnect
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                                    ((TextView) findViewById(R.id.rssival)).setText("Try reconnect ...");
                                    btnConnectDisconnect.callOnClick();
                                }
                            }
                        }, 10000);
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }

            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8").trim();
                            Log.d(TAG, text);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }

            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }

        }
    };

    private Bitmap getViewBitmap(View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            Log.e(TAG, "failed getViewBitmap(" + v + ")", new RuntimeException());
            return null;
        }

        bitmap16b = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        canvas = new Canvas(bitmap16b);

        return bitmap16b;
    }

    @Override
    public void onLocationChanged(Location location) {
        IMapController mapController = map.getController();
        mapController.setZoom(zoom);
        GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        mapController.setCenter(startPoint);
        swMap.setImageBitmap(getViewBitmap(map));

        if (!btnConnectDisconnect.getText().equals("Connect") && autoCb.isChecked()) {
            sendImg();
        }
        // ende request
        mLocationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }


    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mStatusChecker);
        try {
            LocalBroadcastManager.getInstance(ctx).unregisterReceiver(UARTStatusChangeReceiver);
            unbindService(mServiceConnection);
            mService.stopSelf();
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        super.onDestroy();
    }
    @Override
    public void onResume() {
        super.onResume();
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}