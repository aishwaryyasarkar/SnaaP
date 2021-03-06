package com.example.nilanjandaw.snaap;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends Activity {

    private EditText addressBar;
    private TextView showList;
    private BluetoothComm communicator;
    private BluetoothSocket socket = null;
    public String timestamp;
    public String mac_id;
    Fields_Details fields_details;
    JsonUploader jsonUploader;
    GPSTracker gps;
//    TimeStamp timestamp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button connect;
        Button lost_id;
        addressBar = (EditText) findViewById(R.id.address_bar);
        showList = (TextView) findViewById(R.id.show_list);
        showList.setText("");
        communicator = new BluetoothComm();
        connect = (Button) findViewById(R.id.connect);
        lost_id = (Button) findViewById(R.id.lost_id);
        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = addressBar.getText().toString();
                String deviceList = showList.getText().toString();
                showList.setText(deviceList + "\n" + addressBar.getText().toString());
                new Connect().execute(address);
            }
        });

        lost_id.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), Lost_Tag.class);
                startActivity(i);
            }
        });

        fields_details = new Fields_Details();
//        timestamp = new TimeStamp(MainActivity.this);
        fields_details.setTimestamp(getTimestamp());
        showToast(getTimestamp());
        fields_details.setMacId(getMacID());
        showToast(getMacID());
        gps = new GPSTracker(MainActivity.this);
        // check if GPS enabled
        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            fields_details.setLatitude(String.valueOf(latitude));
            fields_details.setLongitude(String.valueOf(longitude));

            // \n is for new line
            Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
        }
        jsonUploader = new JsonUploader();
        if(!jsonUploader.validate()) {
            Toast.makeText(getBaseContext(), "No Data Found!!", Toast.LENGTH_LONG).show();
        }else {
            jsonUploader.new HttpAsyncTask().execute("http://hmkcode.appspot.com/jsonservlet", fields_details.getLongitude().toString(), fields_details.getLatitude(), fields_details.getMacId(), fields_details.getTimestamp());    // call AsynTask to perform network operation on separate thread

        }

    }

    private void startCommunication() {
        new ReceiverTask().execute(socket);
        new SenderTask().execute(socket);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        communicator.close();
    }

    public class Connect extends AsyncTask<String, Void, BluetoothSocket> {

        @Override
        protected BluetoothSocket doInBackground(String... params) {

            String address = params[0];
            BluetoothSocket socket;
            socket = communicator.connect(address);
            return socket;
        }

        @Override
        protected void onPostExecute(BluetoothSocket bluetoothSocket) {
            super.onPostExecute(bluetoothSocket);
            socket = bluetoothSocket;
            if (socket != null)
                showToast("Connection Successful");
            else
                showToast("Connection Failed");
            startCommunication();
        }
    }

    public class ReceiverTask extends AsyncTask<BluetoothSocket, Void, String> {

        @Override
        protected String doInBackground(BluetoothSocket... params) {
            while (socket != null) {
                String stringReceived = communicator.receiveData(params[0]);
//                Integer stringInteger = Integer.parseInt(stringReceived);
//                stringInteger = stringInteger&00000001;
//                if (stringInteger>0){
//
//
//                }
                Log.d("receiverTask", stringReceived);
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

    public class SenderTask extends AsyncTask<BluetoothSocket, Void, String> {

        @Override
        protected String doInBackground(BluetoothSocket... params) {
            try {
                String message = "Test String";// message to be sent
                while (params[0] != null && !message.equalsIgnoreCase("")) {
                    byte msg[] = message.getBytes();
                    communicator.sendData(msg, params[0].getOutputStream());
                    //message = "";
                    Log.d("MESSGAE",message);
                    Thread.sleep(50);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return "Execution Done";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("Data Sent", s);
        }
    }

    private void showToast(String s) {
        Toast.makeText(getBaseContext(), s, Toast.LENGTH_SHORT).show();
    }

    public String getTimestamp() {
        Calendar c1 = Calendar.getInstance();
        SimpleDateFormat simpledateformat = new SimpleDateFormat("d/M/yy h:m:s a");
        timestamp = simpledateformat.format(c1.getTime());
        Log.d("Time Stamp..........", timestamp);
        return timestamp;
    }
    public String getMacID(){
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        mac_id = wInfo.getMacAddress();
        return mac_id;
    }
}
