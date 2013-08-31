package com.banderkat.blue_pi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;
import android.support.v4.content.LocalBroadcastManager;

public class BluetoothService extends Service {

	private static final String TAG = "Blue Pi BluetoothService";
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private static String devAddr = "00:07:80:99:56:65";
	private static final String DEVICE_NAME = "ARDUINOBT";

	private final IBinder mBinder = new LocalBinder();

	public static final byte CLEAR_BYTE = (byte) 0xB7;
	public static final byte PRINT_BYTE = (byte) 0xB8;
	public static final byte M1_FWD = (byte) 0xC1;
	public static final byte M1_REV = (byte) 0xC2;
	public static final byte M2_FWD = (byte) 0xC5;
	public static final byte M2_REV = (byte) 0xC6;

	public static final byte [] STOP_DRIVING = {M1_FWD, (byte) 0, M2_FWD, (byte) 0, M1_REV, (byte) 0, M2_REV, (byte) 0, 
		CLEAR_BYTE, PRINT_BYTE, (byte) 7, (byte) 'S', (byte) 'T', (byte) 'O', (byte) 'P', (byte) 'P', (byte) 'E', (byte) 'D'};

	public static final byte [] HOWDY = {PRINT_BYTE, (byte) 5, (byte) 'H', 
		(byte) 'O', (byte) 'W', (byte) 'D', (byte) 'Y'};

	public static final byte [] CLEARSCREEN = {CLEAR_BYTE, };

	private ConnectThread deviceConnection;
	private ConnectedThread socketMgr;
	private BluetoothAdapter mBluetoothAdapter;
	private LocalBroadcastManager mBroadcast;

	private boolean mConnected = false;
	private Time lastRead = new Time(); // last time 3pi sent back voltage
	private Time now = new Time();
	private long lastReadMillis = lastRead.toMillis(false);
	private long nowMillis = now.toMillis(false);

	private boolean haveAccelSteer = false;

	// Message types sent from the handler
	public static final int MESSAGE_TOAST = 1;
	public static final int MESSAGE_VOLTAGE = 2;
	public static final int MESSAGE_NO_CONNECTION = 3;
	public static final int MESSAGE_CONNECTION_MADE = 4;



	protected void showMessage(String msg) {
		CharSequence text = msg;
		int duration = Toast.LENGTH_SHORT;

		Toast toast = Toast.makeText(getBaseContext(), text, duration);
		toast.show();
	}

	// message handler for UI
	//  TODO:  make weak reference to outer class to avoid memory leakage
	//  see:  
	// http://www.mail-archive.com/android-developers@googlegroups.com/msg31754.html
	// http://android-developers.blogspot.com/2009/01/avoiding-memory-leaks.html
	public Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MESSAGE_VOLTAGE:

				// TODO: broadcast Intent with voltage instead
				BluePiActivity.updateVoltage(msg.getData().getInt("voltage"));
				if (haveAccelSteer) {
					AccelSteerActivity.updateVoltage(msg.getData().getInt("voltage"));
				}

				lastRead.setToNow();  // get time of this reading
				lastReadMillis = lastRead.toMillis(false);
				break;
			case MESSAGE_TOAST:
				showMessage(msg.getData().getString("toast"));
				break;
			case MESSAGE_NO_CONNECTION:
				mConnected = false;
				showMessage(msg.getData().getString("lost"));
				stopSelf();
				break;
			case MESSAGE_CONNECTION_MADE:
				mConnected = true;
				showMessage(msg.getData().getString("found"));
				break;
			default:
				Log.e(TAG + ": handleMessage", "message type " + msg.what + " not recognized");
				break;	
			}
		}
	};

	private void messageLost() {
		Message msg = mHandler.obtainMessage(MESSAGE_NO_CONNECTION);
		Bundle bundle = new Bundle();
		bundle.putString("lost", "arduino not responding");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	
	public boolean haveAccelSteer() {
		return haveAccelSteer;
	}

	public void setHaveAccelSteer(boolean gotIt) {
		if (gotIt) {
			haveAccelSteer = true;
		} else {
			haveAccelSteer = false;
		}
	}

	public boolean isConnected() {
		return mConnected;
	}

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		BluetoothService getService() {
			// Return this instance of LocalService so clients can call public methods
			return BluetoothService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		// TODO: create Intent to broadcast
		// TODO: add BroadcastReceivers to both Activities
		mBroadcast = LocalBroadcastManager.getInstance(getApplicationContext());

		// check if bluetooth is enabled and query paired devices

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support bluetooth

			// TODO:  broadcast message
			showMessage("no bluetooth support");
			stopSelf();

		} else if (!mBluetoothAdapter.isEnabled()) {
			// TODO:  tell bound activity to request to enable bluetooth and try again
			//Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			//startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
			showMessage("need to enable bluetooth");

			// bail
			stopSelf();
		}

		// have bluetooth; query paired devices for ArduinoBT
		boolean haveDevice = false;
		String deviceAddress = "";
		String deviceName = "";
		BluetoothDevice arduinoDevice = null;
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

		// If there are paired devices
		if (pairedDevices.size() > 0) {
			// Loop through paired devices
			for (BluetoothDevice device : pairedDevices) {
				deviceAddress = device.getAddress();
				deviceName = device.getName();
				System.out.println(deviceName + "\n" + deviceAddress);
				if (deviceName.compareTo(DEVICE_NAME) == 0) {
					devAddr = deviceAddress;
					arduinoDevice = device;
					haveDevice = true;
					break;
				}
			}


			if (haveDevice) {
				showMessage("paired ArduinoBT found:\n" + devAddr);
			} else {
				// device not paired
				showMessage("device not paired yet");
			}

		}

		// TODO:  track connection status
		// connect to device (will attempt to pair if not paired already)
		if ( arduinoDevice != null) {
			// sleep until bluetooth is enabled
			while (!mBluetoothAdapter.isEnabled()){
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Log.e(TAG + ": interrupted waiting for bluetooth to be enabled", e.toString());
				}
			}
			deviceConnection = new ConnectThread(arduinoDevice);
			deviceConnection.start();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (deviceConnection != null) {
			if (deviceConnection.isAlive()) {
				deviceConnection.cancel();
			}
			deviceConnection = null;
		}

		if (socketMgr != null) {
			if (socketMgr.isAlive()) {
				try {
					// send final stop message if still have connection
					socketMgr.write(STOP_DRIVING);
					socketMgr.cancel();
				} catch (Exception e) {
					Log.e(TAG + ": onDestroy()", e.toString()); 
				}
			}
			socketMgr = null;
		}
	}

	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;

		public ConnectThread(BluetoothDevice device) {
			// Use a temporary object that is later assigned to mmSocket,
			// because mmSocket is final
			BluetoothSocket tmp = null;

			// Get a BluetoothSocket to connect with the given BluetoothDevice
			try {
				// MY_UUID is the app's UUID string, also used by the server code
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) { Log.e(TAG + ": ConnectThread creating socket", e.toString()); }
			mmSocket = tmp;
		}

		public void run() {
			// Cancel discovery because it will slow down the connection
			mBluetoothAdapter.cancelDiscovery();

			try {
				// Connect the device through the socket. This will block
				// until it succeeds or throws an exception
				System.out.println("attempting connection...");
				mmSocket.connect();
			} catch (IOException connectException) {
				// Unable to connect; close the socket and get out
				Log.e(TAG + ": ConnectThread connecting to socket", connectException.toString());
				try {
					mmSocket.close();
					messageLost();
				} catch (IOException e) { Log.e(TAG + ": ConnectThread catching failed connect()", e.toString()); }
				
				messageLost();
				//stopSelf(); // bail
				return;
			}
			Log.d("ConnectThread", "connection succceeded");

			// Do work to manage the connection (in a separate thread)
			manageConnectedSocket(mmSocket);
		}

		/** Will cancel an in-progress connection, and close the socket */
		public void cancel() {
			try {
				Log.d(TAG, "closing connection");
				mmSocket.close();
			} catch (IOException e) { Log.e(TAG + ": ConnectThread.cancel()", e.toString()); }
		}
	}

	public void writeStop() {
		try {
			socketMgr.write(STOP_DRIVING);
		} catch (Exception e) {
			Log.e(TAG + ": writeStop", e.toString());
			messageLost();
		}
	}

	public void writeHowdy() {
		try{
			socketMgr.write(CLEARSCREEN);
			socketMgr.write(HOWDY);
		} catch (Exception e) {
			Log.e(TAG + ": writeHowdy", e.toString());
			messageLost();
		}
	}

	public void writeMsg(byte[] message) {

		try {
			socketMgr.write(message);
		} catch (Exception e) {
			Log.e(TAG + ": writeMsg", e.toString());
			messageLost();
		}
	}

	public void manageConnectedSocket(BluetoothSocket mmSocket) {
		Log.d("manageConnectedSocket", "starting socket");
		socketMgr = new ConnectedThread(mmSocket);
		socketMgr.start();

		try {
			// send a heartbeat battery voltage request
			socketMgr.write(new byte [] { (byte) 0xB1 , } );

			// check when last voltage reading received
			now.setToNow();
			nowMillis = now.toMillis(false);

			// if more than a second since when the last voltage reading was received, have no connection
			if ((nowMillis - lastReadMillis) > 1000) {
				messageLost();
			}

			Thread.sleep(500);
		} catch (InterruptedException e) { Log.e(TAG + ": manageConnectedSocket requesting voltage", e.toString()); }
	}

	private class ConnectedThread extends Thread {
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the input and output streams, using temp objects because
			// member streams are final
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) { Log.e(TAG + ": ConnectedThread getting I/O streams", e.toString()); }

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[2];  // buffer store for the stream
			int readVoltage = 0;
			int readBytes = 0; // number of bytes actually read

			// Keep listening to the InputStream until an exception occurs
			while (true) {
				try {
					// Read from the InputStream into buffer
					readBytes = mmInStream.read(buffer);

					// got something, so have a connection
					if (!mConnected) {
						mConnected = true;
						Message msg = mHandler.obtainMessage(MESSAGE_CONNECTION_MADE);
						Bundle bundle = new Bundle();
						bundle.putString("found", "arduino is now communicating");
						msg.setData(bundle);
						mHandler.sendMessage(msg);
					}

					// get battery voltage as two-byte int
					readVoltage = buffer[0] << 8 | buffer[1];

					Log.d(TAG, "3pi battery voltage: " + readVoltage); 

					Message msg = mHandler.obtainMessage(MESSAGE_VOLTAGE);
					Bundle bundle = new Bundle();
					bundle.putInt("voltage", readVoltage);
					msg.setData(bundle);
					mHandler.sendMessage(msg);

				} catch (IOException e) {
					mConnected = false;
					Log.e(TAG + ": ConnectedThread.run()", e.toString());
					break;
				}
			}
		}

		/* Call this from the main activity to send data to the remote device */
		public void write(byte[] bytes) {
			try {
				mmOutStream.write(bytes);
			} catch (IOException e) { Log.e(TAG + ": ConnectedThread.write(byte[])", e.toString()); }
		}

		/* Call this from the main activity to shutdown the connection */
		public void cancel() {
			try {
				mmInStream.close();
				mmOutStream.close();
			} catch (IOException e) { Log.e(TAG + ": ConnectedThread.cancel()", e.toString()); }
		}	
	}
}
