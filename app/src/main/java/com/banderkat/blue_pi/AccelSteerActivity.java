package com.banderkat.blue_pi;

import com.banderkat.blue_pi.BluetoothService;
import com.banderkat.blue_pi.BluetoothService.LocalBinder;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AccelSteerActivity extends MenuActivity {
	private static final String TAG = "Blue Pi AccelSteerActivity";
	
	Context context;
	private ToggleButton stopBtn, spinLeftBtn, spinRightBtn;
	private Sensor mAccelerometer;
	private SensorManager mSensorManager;
	private TextView showAccelText;
	private static TextView showVoltageText;
	private static final byte SPIN_SPEED = (byte) 30;
	
	BluetoothService mService;
	boolean mBound = false;
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.setHaveAccelSteer(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	if (mService != null) {
    			mService.setHaveAccelSteer(false);
    		}
        	
        	if (mBound) {
                unbindService(mConnection);
                mBound = false;
                mConnection = null;
            }
        }
    };
	
	@Override
	protected void onStart() {
		super.onStart();
		
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
		// Bind to BluetoothService
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mService != null) {
			mService.setHaveAccelSteer(false);
		}
		
		if (mBound) {
            unbindService(mConnection);
            mBound = false;
            mConnection = null;
        }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(mAccelListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		
		stopBtn.setChecked(false);
		spinLeftBtn.setEnabled(true);
		spinRightBtn.setEnabled(true);
		
		if (mService != null) {
			mService.setHaveAccelSteer(true);
		}
	}
	
	@Override
	protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mAccelListener);
        
        if (mService != null) {
			mService.setHaveAccelSteer(false);
		}
    }
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accel_steer);
		
		// keep screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// force landscape orientation
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		// Bind to BluetoothService
        Intent intent = new Intent(this, BluetoothService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mBound = true;

		// Capture our button from layout
	    stopBtn = (ToggleButton)findViewById(R.id.toggleButtonSTOP);
	    // Register the onClick listener with the implementation above
	    if (stopBtn != null) {
	    	stopBtn.setOnClickListener(mStopListener);
	    }
	    
	    showAccelText = (TextView)findViewById(R.id.textViewShowAccel);
	    showVoltageText = (TextView)findViewById(R.id.textViewVoltage);
	    spinLeftBtn = (ToggleButton)findViewById(R.id.ToggleButtonLeft);
	    spinRightBtn = (ToggleButton)findViewById(R.id.ToggleButtonRight);
	    
	    if (spinLeftBtn != null) {
	    	spinLeftBtn.setOnClickListener(mSpinLeftListener);
	    }
	    
	    if (spinRightBtn != null) {
	    	spinRightBtn.setOnClickListener(mSpinRightListener);
	    }
	    
	    spinLeftBtn.setEnabled(true);
		spinRightBtn.setEnabled(true);
	    
	}
	
	protected void updateAccelText(float pitch, float roll) {
		CharSequence useText = "Pitch:  " + pitch + "\nRoll:  " + roll;
		showAccelText.setText(useText);
	}
	
	protected static void updateVoltage(int int1) {
		CharSequence useText = "Battery voltage: " + int1;
		showVoltageText.setText(useText);
	}
	
	// Create an anonymous implementation of OnClickListener
	private OnClickListener mStopListener = new OnClickListener() {
	    public void onClick(View v) {
	      
	    	// send stop message
    		mService.writeStop();
    		
	    	if (stopBtn.isChecked()) {
	    		spinLeftBtn.setEnabled(false);
	    		spinRightBtn.setEnabled(false);
	    		spinLeftBtn.setChecked(false);
	    		spinRightBtn.setChecked(false);
	    	} else {
	    		spinLeftBtn.setEnabled(true);
	    		spinRightBtn.setEnabled(true);
	    	}
	    }
	};
	
	private OnClickListener mSpinLeftListener = new OnClickListener() {
	    public void onClick(View v) {
	    	if (spinLeftBtn.isChecked()) {
	    		spinRightBtn.setChecked(false);
	    		byte[] msg = {BluetoothService.M2_FWD, SPIN_SPEED, BluetoothService.M1_REV, SPIN_SPEED};
	    		mService.writeMsg(msg);
	    	} else {
	    		mService.writeStop();
	    	}
	    }
	};
	
	private OnClickListener mSpinRightListener = new OnClickListener() {
	    public void onClick(View v) {
	    	if (spinRightBtn.isChecked()) {
	    		spinLeftBtn.setChecked(false);
	    		byte[] msg = {BluetoothService.M1_FWD, SPIN_SPEED, BluetoothService.M2_REV, SPIN_SPEED};
	    		mService.writeMsg(msg);
	    	} else {
	    		mService.writeStop();
	    	}
	    }
	};
	
	private SensorEventListener mAccelListener = new SensorEventListener() {

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) { }

		@Override
		public void onSensorChanged(SensorEvent event) {
		    float pitch = event.values[1];
		    float roll = event.values[2];
		    
		    // update label with accelerometer data
		    updateAccelText(pitch, roll);
		    
		    if (stopBtn.isChecked()) {
		    	// drive
		    	setWheelsFromAccel(pitch, roll);
		    }
		}
		
	};
	
	// range map implementation from rosettacode.org
	public static double mapRange(double a1, double a2, double b1, double b2, double s){
		return b1 + ((s - a1)*(b2 - b1))/(a2 - a1);
	}

	protected void setWheelsFromAccel(float p, float r) {
		// set wheel speed based on pitch and roll
		// use roll to control speed and pitch to control turn
		int rightSpeed = 0;
		int leftSpeed = 0;
		
		boolean posPitch = false;
		boolean posRoll = false;
		
		if (p > 0) {
			posPitch = true;
		}
		
		if (r > 0) {
			posRoll = true;
		}
		
		double pitch = Math.abs(p);
		double roll = Math.abs(r);
		
		if (pitch > 10) {
			pitch = 10;
		}
		
		if (roll > 10) {
			roll = 10;
		}
		
		
		rightSpeed = (int)mapRange(0, 10, 0, 127, roll);
		leftSpeed = rightSpeed;
		
		double pitchPct = mapRange(0, 10, 0, 1, pitch);
		
		if (posPitch) {
			// steer right
			leftSpeed -= (leftSpeed * pitchPct);
		} else {
			// steer left
			rightSpeed -= (rightSpeed * pitchPct);
		}
		
		Log.d(TAG + " setWheelsFromAccel", "leftSpeed: " + leftSpeed + " rightSpeed: " + rightSpeed);
		
		if (posRoll) {
			// drive forward
			byte[] msg = {BluetoothService.M1_FWD, (byte)rightSpeed, BluetoothService.M2_FWD, (byte)leftSpeed};
    		mService.writeMsg(msg);
		} else {
			// drive in reverse
			byte[] msg = {BluetoothService.M2_REV, (byte)leftSpeed, BluetoothService.M1_REV, (byte)rightSpeed};
    		mService.writeMsg(msg);
		}
	}

}

