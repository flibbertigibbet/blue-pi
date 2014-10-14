package com.banderkat.blue_pi;

import com.banderkat.blue_pi.BluetoothService.LocalBinder;
import com.banderkat.blue_pi.R;
import com.banderkat.blue_pi.AccelSteerActivity;
import com.banderkat.blue_pi.BluetoothService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class BluePiActivity extends MenuActivity {

	private static final String TAG = "Blue Pi BluePiActivity";
	
	private ToggleButton startStopBtn;
	private Button useAccelBtn;
	private SeekBar seekBarFWD, seekBarREV, seekBarLEFT_FWD, seekBarLEFT_REV, seekBarRIGHT_FWD, seekBarRIGHT_REV;
	private TextView lblFWD, lblREV, lblLEFT_FWD, lblLEFT_REV, lblRIGHT_FWD, lblRIGHT_REV;
	private static TextView voltageView;
	Context context;
	BluetoothService mService;
	boolean mBound = false;
	
    private static final int REQUEST_ENABLE_BT = 1;
	
	private OnClickListener mUseAccelListener = new OnClickListener() {
	    public void onClick(View v) {
	    	// start AccelSteerActivity
	    	Intent accelSteer = new Intent(getApplicationContext(), AccelSteerActivity.class);
			startActivity(accelSteer);
	    }
	};
	
	protected void onActivityResult (int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT) {
			if (resultCode != RESULT_OK) {
				// no bluetooth; exit
				Message lostMsg = mService.mHandler.obtainMessage(BluetoothService.MESSAGE_NO_CONNECTION);
                Bundle bundle = new Bundle();
                bundle.putString("lost", "bluetooth disabled");
                lostMsg.setData(bundle);
                mService.mHandler.sendMessage(lostMsg);
				this.finish();
			}
		}
	}
	
	// Create an anonymous implementation of OnClickListener
	private OnClickListener mStartStopListener = new OnClickListener() {
	    public void onClick(View v) {
	      // do something when the button is clicked
	    	try {
				if (startStopBtn.isChecked()) {
					Log.d("onClick", "checked");
					mService.writeHowdy();
					
					// hide switch button
					useAccelBtn.setVisibility(View.GONE);
					
					// show seek bars
					seekBarFWD.setVisibility(View.VISIBLE);
					seekBarREV.setVisibility(View.VISIBLE);
					seekBarLEFT_FWD.setVisibility(View.VISIBLE);
					seekBarLEFT_REV.setVisibility(View.VISIBLE);
					seekBarRIGHT_FWD.setVisibility(View.VISIBLE);
					seekBarRIGHT_REV.setVisibility(View.VISIBLE);
					
					// show labels
					lblFWD.setVisibility(View.VISIBLE);
					lblREV.setVisibility(View.VISIBLE);
					lblLEFT_FWD.setVisibility(View.VISIBLE);
					lblLEFT_REV.setVisibility(View.VISIBLE);
					lblRIGHT_FWD.setVisibility(View.VISIBLE);
					lblRIGHT_REV.setVisibility(View.VISIBLE);
					
					seekBarFWD.setEnabled(true);
					seekBarREV.setEnabled(true);
					seekBarLEFT_FWD.setEnabled(true);
					seekBarLEFT_REV.setEnabled(true);
					seekBarRIGHT_FWD.setEnabled(true);
					seekBarRIGHT_REV.setEnabled(true);
					
				} else {
					mService.writeStop();
					Log.d("onClick", "not checked");
				
					// disable seek bars when stopped
					seekBarFWD.setEnabled(false);
					seekBarREV.setEnabled(false);
					seekBarLEFT_FWD.setEnabled(false);
					seekBarLEFT_REV.setEnabled(false);
					seekBarRIGHT_FWD.setEnabled(false);
					seekBarRIGHT_REV.setEnabled(false);
					
					seekBarFWD.setProgress(0);
					seekBarREV.setProgress(0);
					seekBarRIGHT_FWD.setProgress(0);
					seekBarRIGHT_REV.setProgress(0);
					seekBarLEFT_FWD.setProgress(0);
					seekBarLEFT_REV.setProgress(0);
					
					// hide seek bars while stopped
					seekBarFWD.setVisibility(View.GONE);
					seekBarREV.setVisibility(View.GONE);
					seekBarLEFT_FWD.setVisibility(View.GONE);
					seekBarLEFT_REV.setVisibility(View.GONE);
					seekBarRIGHT_FWD.setVisibility(View.GONE);
					seekBarRIGHT_REV.setVisibility(View.GONE);
					
					// hide labels
					// show labels
					lblFWD.setVisibility(View.GONE);
					lblREV.setVisibility(View.GONE);
					lblLEFT_FWD.setVisibility(View.GONE);
					lblLEFT_REV.setVisibility(View.GONE);
					lblRIGHT_FWD.setVisibility(View.GONE);
					lblRIGHT_REV.setVisibility(View.GONE);
					
					// show switch to accelerometer button
					useAccelBtn.setVisibility(View.VISIBLE);
				}
			} catch (Exception e) { Log.e(TAG + ": onClick", e.toString()); } finally {
				if (!mBound) {
					Log.e(TAG + ": onClick", "no longer bound to BluetoothService");
	                Message msg = mService.mHandler.obtainMessage(BluetoothService.MESSAGE_NO_CONNECTION);
	                Bundle bundle = new Bundle();
	                bundle.putString("lost", "no connection to arduino");
	                msg.setData(bundle);
	                mService.mHandler.sendMessage(msg);
				}
			}
	    }
	};
	
	// Create an anonymous implementation of OnClickListener
	private OnSeekBarChangeListener mSeekBarListener = new OnSeekBarChangeListener() {
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			
			if (!startStopBtn.isChecked()) { return; } // do nothing if stopped (shouldn't get here)
			
			byte speed = (byte) progress;
			byte[] msg;
			
			if (seekBar.equals(seekBarFWD)) {
				msg = new byte [] {BluetoothService.M2_FWD, speed, BluetoothService.M1_FWD, speed};
			} else if (seekBar.equals(seekBarREV)) {
				msg = new byte [] {BluetoothService.M1_REV, speed, BluetoothService.M2_REV, speed};
			} else if (seekBar.equals(seekBarRIGHT_FWD)) {
				msg = new byte [] {BluetoothService.M2_FWD, speed};
			} else if (seekBar.equals(seekBarRIGHT_REV)) {
				msg = new byte [] {BluetoothService.M2_REV, speed};
			} else if (seekBar.equals(seekBarLEFT_FWD)) {
				msg = new byte [] {BluetoothService.M1_FWD, speed};
			} else if (seekBar.equals(seekBarLEFT_REV)) {
				msg = new byte [] {BluetoothService.M1_REV, speed};
			} else {
				Log.d("onProgressChanged", "seek bar with ID " + seekBar.getId() + " not recognized");
				msg = BluetoothService.STOP_DRIVING;
			}
			try {
				mService.writeMsg(msg);
			} catch (Exception e) {
				Log.e(TAG + ": onProgressChanged writing to socketMgr", e.toString());
			} finally {
				if (!mBound) {
					Log.d(TAG + ": onClick", "no longer bound to BluetoothService");
	                Message lostMsg = mService.mHandler.obtainMessage(BluetoothService.MESSAGE_NO_CONNECTION);
	                Bundle bundle = new Bundle();
	                bundle.putString("lost", "no connection to arduino");
	                lostMsg.setData(bundle);
	                mService.mHandler.sendMessage(lostMsg);
				}
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// set all other seek bars to zero 
			if (seekBar.equals(seekBarFWD)) {
	    		seekBarREV.setProgress(0);
	    		seekBarRIGHT_FWD.setProgress(0);
	    		seekBarRIGHT_REV.setProgress(0);
	    		seekBarLEFT_FWD.setProgress(0);
	    		seekBarLEFT_REV.setProgress(0);
			} else if (seekBar.equals(seekBarREV)) {
				seekBarFWD.setProgress(0);
	    		seekBarRIGHT_FWD.setProgress(0);
	    		seekBarRIGHT_REV.setProgress(0);
	    		seekBarLEFT_FWD.setProgress(0);
	    		seekBarLEFT_REV.setProgress(0);
			} else if (seekBar.equals(seekBarRIGHT_FWD)) {
				seekBarFWD.setProgress(0);
	    		seekBarREV.setProgress(0);
	    		seekBarRIGHT_REV.setProgress(0);
	    		seekBarLEFT_FWD.setProgress(0);
	    		seekBarLEFT_REV.setProgress(0);
			} else if (seekBar.equals(seekBarRIGHT_REV)) {
				seekBarFWD.setProgress(0);
	    		seekBarREV.setProgress(0);
	    		seekBarRIGHT_FWD.setProgress(0);
	    		seekBarLEFT_FWD.setProgress(0);
	    		seekBarLEFT_REV.setProgress(0);
			} else if (seekBar.equals(seekBarLEFT_FWD)) {
				seekBarFWD.setProgress(0);
	    		seekBarREV.setProgress(0);
	    		seekBarRIGHT_FWD.setProgress(0);
	    		seekBarRIGHT_REV.setProgress(0);
	    		seekBarLEFT_REV.setProgress(0);
			} else if (seekBar.equals(seekBarLEFT_REV)) {
				seekBarFWD.setProgress(0);
	    		seekBarREV.setProgress(0);
	    		seekBarRIGHT_FWD.setProgress(0);
	    		seekBarRIGHT_REV.setProgress(0);
	    		seekBarLEFT_FWD.setProgress(0);
			} else {
				Log.d("onStartTrackingTouch", "seek bar with ID " + seekBar.getId() + " not recognized");
			} 
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			try{
				mService.writeStop();
			} catch (Exception e) {
				Log.e(TAG + ": onStopTrackingTouch writing to socketMgr", e.toString());
			} finally {
				if (!mBound) {
					Log.d(TAG + ": onClick", "no longer bound to BluetoothService");
	                Message lostMsg = mService.mHandler.obtainMessage(BluetoothService.MESSAGE_NO_CONNECTION);
	                Bundle bundle = new Bundle();
	                bundle.putString("lost", "no connection to arduino");
	                lostMsg.setData(bundle);
	                mService.mHandler.sendMessage(lostMsg);
				}
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// Bind to BluetoothService
		if (!mBound) {
	        Intent intent = new Intent(this, BluetoothService.class);
	        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	        mBound = true;
		}
		
		// force portrait orientation
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// keep screen on
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// Capture our button from layout
	    startStopBtn = (ToggleButton)findViewById(R.id.toggleButtonSTOP);
	    // Register the onClick listener with the implementation above
	    if (startStopBtn != null) {
	    	startStopBtn.setOnClickListener(mStartStopListener);
	    }
	    
	    voltageView = (TextView)findViewById(R.id.textViewBatteryVoltage);
	    
	    seekBarFWD = (SeekBar)findViewById(R.id.seekBarFWD);
	    seekBarREV = (SeekBar)findViewById(R.id.seekBarREV);
	    seekBarRIGHT_FWD = (SeekBar)findViewById(R.id.seekBarRIGHT_FWD);
	    seekBarRIGHT_REV = (SeekBar)findViewById(R.id.seekBarRIGHT_REV);
	    seekBarLEFT_FWD = (SeekBar)findViewById(R.id.seekBarLEFT_FWD);
	    seekBarLEFT_REV = (SeekBar)findViewById(R.id.seekBarLEFT_REV);
	    
	    seekBarFWD.setOnSeekBarChangeListener(mSeekBarListener);
	    seekBarREV.setOnSeekBarChangeListener(mSeekBarListener);
	    seekBarRIGHT_FWD.setOnSeekBarChangeListener(mSeekBarListener);
	    seekBarRIGHT_REV.setOnSeekBarChangeListener(mSeekBarListener);
	    seekBarLEFT_FWD.setOnSeekBarChangeListener(mSeekBarListener);
	    seekBarLEFT_REV.setOnSeekBarChangeListener(mSeekBarListener);
	    
	    lblFWD = (TextView)findViewById(R.id.textViewFWD);
	    lblREV = (TextView)findViewById(R.id.textViewREV);
	    lblRIGHT_FWD = (TextView)findViewById(R.id.TextViewRIGHT_FWD);
	    lblRIGHT_REV = (TextView)findViewById(R.id.TextViewRIGHT_REV);
	    lblLEFT_FWD = (TextView)findViewById(R.id.TextViewLEFT_FWD);
	    lblLEFT_REV = (TextView)findViewById(R.id.TextViewLEFT_REV);
	    
	    useAccelBtn = (Button)findViewById(R.id.buttonUseAccel);
	    if (useAccelBtn != null) {
	    	useAccelBtn.setOnClickListener(mUseAccelListener);
	    }
	}

    protected void bail() {
    	
    	this.finish();
    }
    

	protected static void updateVoltage(int int1) {
		CharSequence useText = "Battery voltage: " + int1;
		voltageView.setText(useText);
	}

	@Override
	protected void onStart() {
		super.onStart();

		context = getApplicationContext(); // for toast

	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mBound) {
            unbindService(mConnection);
            mBound = false;
            mConnection = null;
        }
	}
	
	/** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalBinder binder = (LocalBinder) service;
            mService = binder.getService();
            if (mService != null) {
            	mBound = true;
            } else {
            	Log.e(TAG + "onServiceConnected", "mService is null (connection failed)");
            	Message lostMsg = mService.mHandler.obtainMessage(BluetoothService.MESSAGE_NO_CONNECTION);
                Bundle bundle = new Bundle();
                bundle.putString("lost", "bluetooth service connection failed");
                lostMsg.setData(bundle);
                mService.mHandler.sendMessage(lostMsg);
            	bail();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	if (mBound) {
                unbindService(mConnection);
                mBound = false;
                mConnection = null;
            }
        }
    };
}
