package com.example.lochana.smartlampcontroller;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * An easy and good implementation for the HC-05 bluetooth component
 *
 */
class WriterThread extends Thread {

    private LinkedBlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private MainActivity controlActivity;
    private volatile boolean isRunning = true;
    private BluetoothSocket bluetoothSocket;

    /**
     * The constructor of this thread
     *
     * @param controlActivity the activity we need to send the callbacks to
     * @param bluetoothSocket the socket where we want to write to
     */
    WriterThread(MainActivity controlActivity, BluetoothSocket bluetoothSocket) {
        this.controlActivity = controlActivity;
        this.bluetoothSocket = bluetoothSocket;
    }

    @Override
    public void run() {
        try {
            // get the output stream from the bluetooth device, so we can send data on it
            OutputStream outputStream = bluetoothSocket.getOutputStream();
            // this while is looping, but don't worry, we use a nice queue
            // if there is no data to send, the thread will be blocked
            while (isRunning()) {
                try {
                    // get data from the queue, and write it
                    byte[] data = queue.take();
                    outputStream.write(data);
                    // there are no exceptions so the writing was successful
                    this.controlActivity.onWriteSuccess(new String(data));
                } catch (InterruptedException e) {
                    // something went wrong, so notify the activity
                    this.controlActivity.onWriteFailure("thread was interrupted");
                    break;
                }
            }
        } catch (IOException e) {
            // something went wrong, so notify the activity
            this.controlActivity.onWriteFailure("thread caught IOException: " + e.getMessage());
        } finally {
            // the thread stopped, let's try to close the socket
            try {
                bluetoothSocket.close();
            } catch (IOException e1) {
                this.controlActivity.onWriteFailure("could not close socket");
            }
        }
    }

    /**
     * Used to stop the thread
     *
     * @param running set to false to stop the thread. Note that you need to create a new thread
     *                object to actually start a new one. Once stopped, you cannot run the same
     *                thread again.
     */
    void setRunning(boolean running) {
        isRunning = running;
    }

    /**
     * Get the running state
     *
     * @return the running state of the thread
     */
    private boolean isRunning() {
        return isRunning;
    }

    /**
     * Add some data to the queue, the thread will process it as soon as it can
     *
     * @param data the data to be send to the socket
     */
    void queueSend(byte[] data) {
        queue.add(data);
    }
}
