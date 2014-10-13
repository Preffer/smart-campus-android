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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main extends Activity {
    private WifiScanReceiver wifiReciever;

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
        registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
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
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    public void makeSniff(View view) {
        WifiManager manager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = manager.getConnectionInfo();
        manager.startScan();
        System.out.println("Start Scan");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReciever);
    }
}

class WifiScanReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        System.out.println("Scan finished");
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
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String jsonString = json.toString();
        System.out.println(jsonString);

        //TextView sniffResult = (TextView) findViewById(R.id.textView);
        //sniffResult.setText(jsonString);

        new PostAsync().execute(jsonString);
    }
}

class PostAsync extends AsyncTask<String, Void, Boolean> {

    @Override
    protected void onPreExecute () {

    }

    @Override
    protected Boolean doInBackground(String... params) {
        return postData(params[0]);
    }

    @Override
    protected void onPostExecute(Boolean result) {
        System.out.println(result);
    }

    public Boolean postData(String jsonString) {
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://campus.pillowsky.org/submit");
            StringEntity se = new StringEntity(jsonString);
            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            httpPost.setEntity(se);

            try {
                HttpResponse response = httpClient.execute(httpPost);
                System.out.println(response);

                return true;
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

}
