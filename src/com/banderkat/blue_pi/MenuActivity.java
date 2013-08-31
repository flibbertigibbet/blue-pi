package com.banderkat.blue_pi;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

public class MenuActivity extends Activity {

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// MenuInflater inflater = getMenuInflater();
		// inflater.inflate(R.menu.game_menu, menu);
		boolean itWorked = false;

		try {
			menu.add("controls");
			menu.add("accelerometer");
			itWorked = true;;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			itWorked = false;
		}
		return itWorked;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection

		CharSequence title = item.getTitle();
		
		if (title == "controls") {
			//showMessage("test");
			Intent myBluePiActivity = new Intent(this.getApplicationContext(), BluePiActivity.class);
			startActivity(myBluePiActivity);
			return true;
		} else if (title =="accelerometer") {
			//showMessage("help");
			Intent myAccelSteerActivity = new Intent(this.getApplicationContext(), AccelSteerActivity.class);
			startActivity(myAccelSteerActivity);
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
}
