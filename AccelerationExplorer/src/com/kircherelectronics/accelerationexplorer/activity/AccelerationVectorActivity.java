package com.kircherelectronics.accelerationexplorer.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import com.kircherelectronics.accelerationexplorer.R;
import com.kircherelectronics.accelerationexplorer.view.AccelerationVectorView;

/*
 * Acceleration Explorer
 * Copyright (C) 2013-2014, Kaleb Kircher - Kircher Engineering, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Draws a two dimensional vector of the acceleration sensors measurements.
 * 
 * @author Kaleb
 * 
 */
public class AccelerationVectorActivity extends Activity implements
		SensorEventListener
{
	private float[] acceleration = new float[3];

	private AccelerationVectorView view;

	// Sensor manager to access the accelerometer sensor
	private SensorManager sensorManager;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.acceleration_vector_activity);

		view = (AccelerationVectorView) findViewById(R.id.vector_acceleration);

		sensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		System.arraycopy(event.values, 0, acceleration, 0, event.values.length);

		view.updatePoint(acceleration[0], acceleration[1]);
	}

	@Override
	public void onPause()
	{
		super.onPause();

		sensorManager.unregisterListener(this);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		// Register for sensor updates.
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_vector_menu, menu);
		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
		// Log the data
		case R.id.action_plot:
			Intent plotIntent = new Intent(this, AccelerationPlotActivity.class);
			startActivity(plotIntent);
			return true;

			// Log the data
		case R.id.menu_settings_help:
			showHelpDialog();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}


	private void showHelpDialog()
	{
		Dialog helpDialog = new Dialog(this);
		helpDialog.setCancelable(true);
		helpDialog.setCanceledOnTouchOutside(true);

		helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		helpDialog.setContentView(getLayoutInflater().inflate(
				R.layout.help_dialog_view, null));

		helpDialog.show();
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1)
	{
		// TODO Auto-generated method stub

	}
}
