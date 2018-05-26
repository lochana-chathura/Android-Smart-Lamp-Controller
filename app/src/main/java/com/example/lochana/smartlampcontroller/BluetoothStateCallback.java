package com.example.lochana.smartlampcontroller;

/**
 * An easy and good implementation for the HC-05 bluetooth component
 *
 * @author Giovanni Terlingen
 */
interface BluetoothStateCallback {

    void onWriteFailure(String e);

    void onWriteSuccess(String command);
}