package click.dummer.Note2Png2Ws;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;
import de.tavendo.autobahn.WebSocketHandler;

public class MainActivity extends Activity {

    private Context ctx;
    public static final String TAG = MainActivity.class.getSimpleName();
    final WebSocketConnection mConnection = new WebSocketConnection();
    private static final int IWIDTH = 300;
    private static final int IHIGHT = 250;
    private static final int FONTSIZE = 25;
    private static final int ICONSIZE = 46;
    private EditText editUrl;
    private EditText editPort;

    private ImageView swMap;
    Bitmap bitmap;
    Canvas canvas;

    private NotificationReceiver nReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ctx = getApplicationContext();

        editUrl  = (EditText) findViewById(R.id.editUrl);
        editPort = (EditText) findViewById(R.id.editPort);
        editUrl.setText(PreferenceManager.getDefaultSharedPreferences(ctx).getString("host", "ws://192.168.1.100"));
        editPort.setText(PreferenceManager.getDefaultSharedPreferences(ctx).getString("port", "65000"));
        swMap = (ImageView) findViewById(R.id.swMap);
        ViewGroup.LayoutParams params = swMap.getLayoutParams();
        params.height = IHIGHT;
        params.width = IWIDTH;
        swMap.setLayoutParams(params);

        nReceiver = new NotificationReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("click.dummer.Note2Png2Ws.NOTIFICATION_LISTENER");
        registerReceiver(nReceiver, filter);
    }

    public void sendNote() {
        String hostUriIp = editUrl.getText().toString();
        String hostPort  = editPort.getText().toString();
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putString("host", hostUriIp).apply();
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putString("port", hostPort).apply();

        hostUriIp = hostUriIp + ":" + hostPort;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        final byte[] byteArray = stream.toByteArray();

        try {
            if (mConnection.isConnected()) mConnection.disconnect();

            mConnection.connect(hostUriIp, new WebSocketHandler() {

                @Override
                public void onOpen() {
                    Log.d(TAG, " -> ws open");
                    mConnection.sendBinaryMessage(byteArray);
                    mConnection.disconnect();
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.d(TAG, "Server says: " + payload);
                }

                @Override
                public void onClose(int code, String reason) {
                    Log.d(TAG, " -> ws close");
                }
            });

        } catch (WebSocketException e) {
            Log.e(TAG, e.toString());
        }
    }

    void createImage(String msg, String pack) {
        Drawable icon = null;
        try {
            icon = getApplicationContext().getPackageManager().getApplicationIcon(pack);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        bitmap = Bitmap.createBitmap(IWIDTH, IHIGHT, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);

        // black background
        Paint p = new Paint();
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(0, 0, 0, 0));
        canvas.drawRect(0, 0, IWIDTH, IHIGHT, p);

        // add message text
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(Color.WHITE);
        tp.setTextSize(FONTSIZE);
        int textWidth = canvas.getWidth() - FONTSIZE;

        StaticLayout textLayout = new StaticLayout(
                msg, tp, textWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f, 0.0f, false
        );
        int textHeight = textLayout.getHeight();

        textLayout.draw(canvas);

        // add icon
        icon.setBounds(0, textHeight, ICONSIZE+5, textHeight+ICONSIZE+5);
        icon.draw(canvas);
    }

    class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra("MSG") != null) {
                String msg = intent.getStringExtra("MSG").trim();
                String pack = intent.getStringExtra("pack");
                createImage(msg, pack);

                swMap.setImageBitmap(bitmap);
                sendNote();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(nReceiver);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
