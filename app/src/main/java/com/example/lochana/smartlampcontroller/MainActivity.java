package com.example.lochana.smartlampcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements BluetoothStateCallback, NavigationView.OnNavigationItemSelectedListener {

    private static final String device = "HC-05";
    private static final UUID BT_UUID = UUID.fromString("0001101-0000-1000-8000-00805F9B34FB");

    private TextView connectionState;
    private static BluetoothAdapter bluetoothAdapter;
    private static BluetoothSocket bluetoothSocket;
    private static ConnectTask connectTask;
    private static WriterThread writerThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // assign the widgets specified in the layout xml file
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        this.connectionState = (TextView) findViewById(R.id.main_state);
        Button on = (Button) findViewById(R.id.btn_on);
        on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.sendCommand("a");
            }
        });
        Button off = (Button) findViewById(R.id.btn_off);
        off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.sendCommand("a");
            }
        });

        MainActivity.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // register these receivers, we need them for setting up connections automatically
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

        // try to connect
        MainActivity.connectTask = new ConnectTask();
        MainActivity.connectTask.execute();
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_instant_controller) {
            // Handle the camera action
        } else if (id == R.id.nav_scheduler) {

        } else if (id == R.id.nav_pair) {

        } else if (id == R.id.nav_about) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Send a command to the writerThread, and the command will be processed there.
     * Note that the thread must be running, otherwise the message is not sent, nor saved.
     *
     * @param command the command we want to send
     */

    private void sendCommand(String command) {
        if (MainActivity.writerThread != null) {
            MainActivity.this.connectionState.setText("trying to send command \"" + command + "\"");
            MainActivity.writerThread.queueSend(command.getBytes());
        } else {
            MainActivity.this.connectionState.setText("could not send command \"" + command + "\" because there is no socket connection");
        }
    }

    /**
     * When the writerThread failed writing, this method is called.
     *
     * @param e the exception message
     */

    @Override
    public void onWriteFailure(final String e) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText("failure: " + e + ", reconnecting...");
                if (MainActivity.connectTask != null && connectTask.getStatus() == AsyncTask.Status.RUNNING) {
                    MainActivity.connectTask.cancel(true);
                }
                MainActivity.connectTask = new ConnectTask();
                MainActivity.connectTask.execute();
            }
        });
    }

    /**
     * When the writerThread writes successful on the socket, this method is called.
     *
     * @param command the already sent command
     */

    @Override
    public void onWriteSuccess(final String command) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText("successfully sent command: \"" + command + "\"");
            }
        });
    }

    /**
     * Closes any running writerThread, and starts a new one
     */

    private void restartWriterThread() {
        if (MainActivity.writerThread != null) {
            MainActivity.writerThread.interrupt();
            MainActivity.writerThread.setRunning(false);
            MainActivity.writerThread = null;
        }
        MainActivity.writerThread = new WriterThread(MainActivity.this, MainActivity.bluetoothSocket);
        MainActivity.writerThread.start();
    }

    /**
     * This class is the main logic for setting up a connection and opening a socket
     */

    class ConnectTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            connectionState.setText("trying to connect to " + MainActivity.device + "...");
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                // we need to enable the bluetooth first in order to make this app working
                if (!bluetoothAdapter.isEnabled()) {
                    publishProgress("bluetooth was not enabled, enabling...");
                    bluetoothAdapter.enable();
                    // turning on bluetooth takes some time
                    while (!bluetoothAdapter.isEnabled()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                    publishProgress("bluetooth turned on");
                }
                // here we are going to check if the device was paired in android, if not the user will be prompt to do so.
                String address = null;
                for (BluetoothDevice d : bluetoothAdapter.getBondedDevices()) {
                    if (MainActivity.device.equals(d.getName())) {
                        address = d.getAddress();
                        break;
                    }
                }
                if (address == null) {
                    return MainActivity.device + " was never paired. Please pair first using Android.";
                }
                // we have a mac address, now we try to open a socket
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                publishProgress("creating socket...");
                MainActivity.bluetoothSocket = device.createRfcommSocketToServiceRecord(MainActivity.BT_UUID);
                publishProgress("canceling discovery...");
                // we cancel discovery for other devices, since it will speed up the connection
                MainActivity.bluetoothAdapter.cancelDiscovery();
                publishProgress("trying to connect to " + device + " with address " + address);
                // try to connect to the bluetooth device, if unsuccessful, an exception will be thrown
                MainActivity.bluetoothSocket.connect();
                // start the writerThread
                restartWriterThread();
                return "connected, writer thread is running";
            } catch (IOException e) {
                try {
                    // try to close the socket, since we can have only one
                    MainActivity.bluetoothSocket.close();
                } catch (IOException e1) {
                    return "failure due " + e.getMessage() + ", closing socket did not work.";
                }
                return "failure due " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            connectionState.setText(s);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            connectionState.setText(values[0]);
        }
    }

    /**
     * This class will process some events which are called from android itself, we use it to
     * establish connections automatically.
     */

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (bluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF) {
                    // the bluetooth was turned off, so we stop any running connection tasks
                    if (MainActivity.connectTask != null && connectTask.getStatus() == AsyncTask.Status.RUNNING) {
                        MainActivity.connectTask.cancel(true);
                    }
                    connectionState.setText("bluetooth was turned off, restarting...");
                    // enable the bluetooth again, and wait till it is turned on
                    bluetoothAdapter.enable();
                    while (!bluetoothAdapter.isEnabled()) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                    connectionState.setText("bluetooth turned on");
                    // try to connect again with the device
                    MainActivity.connectTask = new ConnectTask();
                    MainActivity.connectTask.execute();
                }
            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                // this event is useful if the user has paired the device for the first time
                if (MainActivity.connectTask == null || MainActivity.connectTask.getStatus() == AsyncTask.Status.FINISHED) {
                    connectionState.setText("connected with bluetooth device, reconnecting...");
                    // reconnect since the app is doing nothing at this moment
                    MainActivity.connectTask = new ConnectTask();
                    MainActivity.connectTask.execute();
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                // if the connection gets lost, we have to reconnect again
                connectionState.setText("connection lost, reconnecting...");
                if (MainActivity.connectTask != null && connectTask.getStatus() == AsyncTask.Status.RUNNING) {
                    MainActivity.connectTask.cancel(true);
                }
                MainActivity.connectTask = new ConnectTask();
                MainActivity.connectTask.execute();
            }
        }
    };
}
