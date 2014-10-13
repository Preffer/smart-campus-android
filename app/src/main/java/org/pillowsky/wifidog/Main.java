package org.pillowsky.wifidog;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main extends Activity {
    private WifiScanReceiver wifiReciever;
    private IntentFilter scanCompleteFilter;
    float cursorX;
    float cursorY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        wifiReciever = new WifiScanReceiver();
        scanCompleteFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            ImageView bgView = (ImageView) rootView.findViewById(R.id.imageView);
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int imgW = 1270;
            int imgH = 480;
            final int windowW = metrics.widthPixels;
            final int windowH = metrics.heightPixels;
            bgView.getLayoutParams().width = windowH * imgW / imgH;
            bgView.getLayoutParams().height = windowH;
            bgView.setOnTouchListener(new View.OnTouchListener() {
                float startX, startY;
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getRawX() - v.getX();
                            startY = event.getRawY() - v.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            v.setX(event.getRawX() - startX);
                            v.setY(event.getRawY() - startY);
                            break;
                        case MotionEvent.ACTION_UP:
                            cursorX = windowW / 2 - (event.getRawX() - startX);
                            cursorY = windowH / 2 - (event.getRawY() - startY);
                            break;
                    }
                    return true;
                }
            });

            return rootView;
        }
    }

    public void makeSniff(View view) {
        Toast.makeText(getApplicationContext(), "Start scan...", Toast.LENGTH_SHORT).show();
        WifiManager manager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        manager.startScan();
        registerReceiver(wifiReciever, scanCompleteFilter);
    }

    class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            unregisterReceiver(wifiReciever);
            //Toast.makeText(getApplicationContext(), "Scan finished", Toast.LENGTH_SHORT).show();
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            JSONObject json = new JSONObject();
            JSONArray scan = new JSONArray();
            for(ScanResult result: manager.getScanResults()){
                JSONObject thisResult = new JSONObject();
                try{
                    thisResult.put("BSSID", result.BSSID);
                    thisResult.put("SSID", result.SSID);
                    thisResult.put("capabilities", result.capabilities);
                    thisResult.put("frequency", result.frequency);
                    thisResult.put("level", result.level);
                    thisResult.put("lastseen", result.timestamp / 1000);
                    scan.put(thisResult);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            try{
                json.put("scan", scan);
                json.put("timestamp", System.currentTimeMillis() / 1000);
                json.put("x", cursorX);
                json.put("y", cursorY);
                EditText zText = (EditText) findViewById(R.id.editText);
                json.put("z", Float.parseFloat(zText.getText().toString()));
                String toastString = "X = " + cursorX + " Y = " + cursorY + " Z = " +  Float.parseFloat(zText.getText().toString());
                Toast.makeText(getApplicationContext(), toastString, Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            //TextView sniffResult = (TextView) findViewById(R.id.textView);
            //sniffResult.setText(jsonString);

            new PostAsync().execute(json.toString());
        }
    }

    class PostAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            try {
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost("http://campus.pillowsky.org/query");
                StringEntity se = new StringEntity(params[0]);
                se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                httpPost.setEntity(se);

                try {
                    HttpResponse response = httpClient.execute(httpPost);
                    return EntityUtils.toString(response.getEntity());
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
        }
    }
}
