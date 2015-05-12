package com.kircherelectronics.accelerationexplorer.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import com.kircherelectronics.accelerationexplorer.R;
import com.kircherelectronics.accelerationexplorer.activity.config.FilterConfigActivity;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfOrientation;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfQuaternion;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfRotationMatrix;
import com.kircherelectronics.accelerationexplorer.filter.ImuLinearAccelerationInterface;
import com.kircherelectronics.accelerationexplorer.filter.LowPassFilterLinearAccel;
import com.kircherelectronics.accelerationexplorer.filter.LowPassFilterSmoothing;
import com.kircherelectronics.accelerationexplorer.filter.MeanFilterSmoothing;
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
public class VectorActivity extends Activity implements SensorEventListener
{
	private boolean meanFilterSmoothingEnabled;
	private boolean lpfSmoothingEnabled;

	private boolean lpfLinearAccelEnabled;
	private boolean androidLinearAccelEnabled;

	private boolean imuLaCfOrienationEnabled;
	private boolean imuLaCfRotationMatrixEnabled;
	private boolean imuLaCfQuaternionEnabled;

	// Outputs for the acceleration and LPFs
	private float[] acceleration = new float[3];
	private float[] linearAcceleration = new float[3];
	private float[] magnetic = new float[3];
	private float[] rotation = new float[3];

	private AccelerationVectorView view;

	// Handler for the UI plots so everything plots smoothly
	private Handler handler;

	private MeanFilterSmoothing meanFilterAccelSmoothing;
	private MeanFilterSmoothing meanFilterMagneticSmoothing;
	private MeanFilterSmoothing meanFilterRotationSmoothing;

	private LowPassFilterSmoothing lpfAccelSmoothing;
	private LowPassFilterSmoothing lpfMagneticSmoothing;
	private LowPassFilterSmoothing lpfRotationSmoothing;

	private LowPassFilterLinearAccel lpfLinearAcceleration;

	private ImuLinearAccelerationInterface imuLinearAcceleration;

	private Runnable runable;

	// Sensor manager to access the accelerometer sensor
	private SensorManager sensorManager;

	// Acceleration UI outputs
	private TextView textViewXAxis;
	private TextView textViewYAxis;
	private TextView textViewZAxis;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.layout_vector);

		textViewXAxis = (TextView) findViewById(R.id.value_x_axis);
		textViewYAxis = (TextView) findViewById(R.id.value_y_axis);
		textViewZAxis = (TextView) findViewById(R.id.value_z_axis);

		view = (AccelerationVectorView) findViewById(R.id.vector_acceleration);

		sensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);

		meanFilterAccelSmoothing = new MeanFilterSmoothing();
		meanFilterMagneticSmoothing = new MeanFilterSmoothing();
		meanFilterRotationSmoothing = new MeanFilterSmoothing();

		lpfAccelSmoothing = new LowPassFilterSmoothing();
		lpfMagneticSmoothing = new LowPassFilterSmoothing();
		lpfRotationSmoothing = new LowPassFilterSmoothing();

		lpfLinearAcceleration = new LowPassFilterLinearAccel();

		handler = new Handler();

		runable = new Runnable()
		{
			@Override
			public void run()
			{
				handler.postDelayed(this, 100);

				updateAccelerationText();

				if (!lpfLinearAccelEnabled && !imuLaCfOrienationEnabled
						&& !imuLaCfRotationMatrixEnabled
						&& !imuLaCfQuaternionEnabled
						&& !androidLinearAccelEnabled)
				{
					view.updatePoint(acceleration[0], acceleration[1]);
				}
				else
				{
					view.updatePoint(linearAcceleration[0],
							linearAcceleration[1]);
				}
			}
		};
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			// Get a local copy of the sensor values
			System.arraycopy(event.values, 0, acceleration, 0,
					event.values.length);

			if (meanFilterSmoothingEnabled)
			{
				acceleration = meanFilterAccelSmoothing
						.addSamples(acceleration);
			}

			if (lpfSmoothingEnabled)
			{
				acceleration = lpfAccelSmoothing.addSamples(acceleration);
			}

			if (lpfLinearAccelEnabled)
			{
				linearAcceleration = lpfLinearAcceleration
						.addSamples(acceleration);
			}

			if (imuLaCfOrienationEnabled || imuLaCfRotationMatrixEnabled
					|| imuLaCfQuaternionEnabled)
			{
				imuLinearAcceleration.setAcceleration(acceleration);
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
		{
			// Get a local copy of the sensor values
			System.arraycopy(event.values, 0, linearAcceleration, 0,
					event.values.length);

			if (meanFilterSmoothingEnabled)
			{
				linearAcceleration = meanFilterAccelSmoothing
						.addSamples(linearAcceleration);
			}

			if (lpfSmoothingEnabled)
			{
				linearAcceleration = lpfAccelSmoothing
						.addSamples(linearAcceleration);
			}
		}

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{

			// Get a local copy of the sensor values
			System.arraycopy(event.values, 0, magnetic, 0, event.values.length);

			if (meanFilterSmoothingEnabled)
			{
				magnetic = meanFilterMagneticSmoothing.addSamples(magnetic);
			}

			if (lpfSmoothingEnabled)
			{
				magnetic = lpfMagneticSmoothing.addSamples(magnetic);
			}

			if (imuLaCfOrienationEnabled || imuLaCfRotationMatrixEnabled
					|| imuLaCfQuaternionEnabled)
			{
				imuLinearAcceleration.setMagnetic(magnetic);
			}

		}

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
		{
			// Get a local copy of the sensor values
			System.arraycopy(event.values, 0, rotation, 0, event.values.length);

			if (meanFilterSmoothingEnabled)
			{
				rotation = meanFilterRotationSmoothing.addSamples(rotation);
			}

			if (lpfSmoothingEnabled)
			{
				rotation = lpfRotationSmoothing.addSamples(rotation);
			}

			if (imuLaCfOrienationEnabled || imuLaCfRotationMatrixEnabled
					|| imuLaCfQuaternionEnabled)
			{
				imuLinearAcceleration.setGyroscope(rotation, System.nanoTime());

				linearAcceleration = imuLinearAcceleration
						.getLinearAcceleration();
			}
		}
	}

	@Override
	public void onPause()
	{
		super.onPause();

		sensorManager.unregisterListener(this);

		handler.removeCallbacks(runable);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		getFilterPrefs();
		registerSensors();

		handler.post(runable);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_vector, menu);
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
		case R.id.action_settings_sensor:
			Intent intent = new Intent(this, FilterConfigActivity.class);
			startActivity(intent);
			return true;

			// Log the data
		case R.id.menu_settings_help:
			showHelpDialog();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void getFilterPrefs()
	{
		meanFilterSmoothingEnabled = getPrefMeanFilterSmoothingEnabled();

		meanFilterAccelSmoothing
				.setTimeConstant(getPrefMeanFilterSmoothingTimeConstant());
		meanFilterMagneticSmoothing
				.setTimeConstant(getPrefMeanFilterSmoothingTimeConstant());
		meanFilterRotationSmoothing
				.setTimeConstant(getPrefMeanFilterSmoothingTimeConstant());

		lpfSmoothingEnabled = getPrefLpfSmoothingEnabled();

		lpfAccelSmoothing.setTimeConstant(getPrefLpfSmoothingTimeConstant());
		lpfMagneticSmoothing.setTimeConstant(getPrefLpfSmoothingTimeConstant());
		lpfRotationSmoothing.setTimeConstant(getPrefLpfSmoothingTimeConstant());

		lpfLinearAccelEnabled = getPrefLpfLinearAccelEnabled();
		lpfLinearAcceleration
				.setFilterCoefficient(getPrefLpfLinearAccelCoeff());

		imuLaCfOrienationEnabled = getPrefImuLaCfOrientationEnabled();
		imuLaCfRotationMatrixEnabled = getPrefImuLaCfRotationMatrixEnabled();
		imuLaCfQuaternionEnabled = getPrefImuLaCfQuaternionEnabled();

		if (imuLaCfOrienationEnabled)
		{
			imuLinearAcceleration = new ImuLaCfOrientation();
			imuLinearAcceleration
					.setFilterCoefficient(getPrefImuLaCfOrienationCoeff());
		}
		else if (imuLaCfRotationMatrixEnabled)
		{
			imuLinearAcceleration = new ImuLaCfRotationMatrix();
			imuLinearAcceleration
					.setFilterCoefficient(getPrefImuLaCfRotationMatrixCoeff());
		}
		else if (imuLaCfQuaternionEnabled)
		{
			imuLinearAcceleration = new ImuLaCfQuaternion();
			imuLinearAcceleration
					.setFilterCoefficient(getPrefImuLaCfQuaternionCoeff());
		}

		androidLinearAccelEnabled = getPrefAndroidLinearAccelEnabled();
	}

	private boolean getPrefAndroidLinearAccelEnabled()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return prefs.getBoolean(
				FilterConfigActivity.ANDROID_LINEAR_ACCEL_ENABLED_KEY, false);
	}

	private boolean getPrefImuLaCfOrientationEnabled()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return prefs.getBoolean(
				FilterConfigActivity.IMULACF_ORIENTATION_ENABLED_KEY, false);
	}

	private boolean getPrefImuLaCfRotationMatrixEnabled()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return prefs
				.getBoolean(
						FilterConfigActivity.IMULACF_ROTATION_MATRIX_ENABLED_KEY,
						false);
	}

	private boolean getPrefImuLaCfQuaternionEnabled()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return prefs.getBoolean(
				FilterConfigActivity.IMULACF_QUATERNION_ENABLED_KEY, false);
	}

	private boolean getPrefLpfLinearAccelEnabled()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return prefs.getBoolean(
				FilterConfigActivity.LPF_LINEAR_ACCEL_ENABLED_KEY, false);
	}

	private float getPrefLpfLinearAccelCoeff()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return Float.valueOf(prefs.getString(
				FilterConfigActivity.LPF_LINEAR_ACCEL_COEFF_KEY, "0.5"));
	}

	private float getPrefImuLaCfOrienationCoeff()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return Float.valueOf(prefs.getString(
				FilterConfigActivity.IMULACF_ORIENTATION_COEFF_KEY, "0.5"));
	}

	private float getPrefImuLaCfRotationMatrixCoeff()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return Float.valueOf(prefs.getString(
				FilterConfigActivity.IMULACF_ROTATION_MATRIX_COEFF_KEY, "0.5"));
	}

	private float getPrefImuLaCfQuaternionCoeff()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return Float.valueOf(prefs.getString(
				FilterConfigActivity.IMULACF_QUATERNION_COEFF_KEY, "0.5"));
	}

	private boolean getPrefLpfSmoothingEnabled()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return prefs.getBoolean(FilterConfigActivity.LPF_SMOOTHING_ENABLED_KEY,
				false);
	}

	private float getPrefLpfSmoothingTimeConstant()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return Float.valueOf(prefs.getString(
				FilterConfigActivity.LPF_SMOOTHING_TIME_CONSTANT_KEY, "0.5"));
	}

	private boolean getPrefMeanFilterSmoothingEnabled()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return prefs.getBoolean(
				FilterConfigActivity.MEAN_FILTER_SMOOTHING_ENABLED_KEY, false);
	}

	private float getPrefMeanFilterSmoothingTimeConstant()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());

		return Float.valueOf(prefs.getString(
				FilterConfigActivity.MEAN_FILTER_SMOOTHING_TIME_CONSTANT_KEY,
				"0.5"));
	}

	private void registerSensors()
	{
		if (!androidLinearAccelEnabled)
		{
			// Register for sensor updates.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_FASTEST);
		}
		else
		{
			// Register for sensor updates.
			sensorManager.registerListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
					SensorManager.SENSOR_DELAY_FASTEST);
		}

		if ((imuLaCfOrienationEnabled || imuLaCfRotationMatrixEnabled || imuLaCfQuaternionEnabled)
				&& !androidLinearAccelEnabled)
		{
			// Register for sensor updates.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					SensorManager.SENSOR_DELAY_FASTEST);

			// Register for sensor updates.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					SensorManager.SENSOR_DELAY_FASTEST);
		}
	}

	private void showHelpDialog()
	{
		Dialog helpDialog = new Dialog(this);
		helpDialog.setCancelable(true);
		helpDialog.setCanceledOnTouchOutside(true);

		helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		helpDialog.setContentView(getLayoutInflater().inflate(
				R.layout.layout_help_vector, null));

		helpDialog.show();
	}

	private void updateAccelerationText()
	{
		if (!lpfLinearAccelEnabled && !imuLaCfOrienationEnabled
				&& !imuLaCfRotationMatrixEnabled && !imuLaCfQuaternionEnabled
				&& !androidLinearAccelEnabled)
		{
			// Update the acceleration data
			textViewXAxis.setText(String.format("%.2f", acceleration[0]));
			textViewYAxis.setText(String.format("%.2f", acceleration[1]));
			textViewZAxis.setText(String.format("%.2f", acceleration[2]));
		}
		else
		{
			// Update the acceleration data
			textViewXAxis.setText(String.format("%.2f", linearAcceleration[0]));
			textViewYAxis.setText(String.format("%.2f", linearAcceleration[1]));
			textViewZAxis.setText(String.format("%.2f", linearAcceleration[2]));
		}
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1)
	{
		// TODO Auto-generated method stub

	}
}
