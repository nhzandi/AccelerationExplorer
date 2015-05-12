package com.kircherelectronics.accelerationexplorer.activity;

import com.kircherelectronics.accelerationexplorer.R;
import com.kircherelectronics.accelerationexplorer.activity.config.FilterConfigActivity;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfOrientation;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfQuaternion;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfRotationMatrix;
import com.kircherelectronics.accelerationexplorer.filter.ImuLinearAccelerationInterface;
import com.kircherelectronics.accelerationexplorer.filter.LowPassFilterLinearAccel;
import com.kircherelectronics.accelerationexplorer.filter.LowPassFilterSmoothing;
import com.kircherelectronics.accelerationexplorer.filter.MeanFilterSmoothing;
import com.kircherelectronics.accelerationexplorer.gauge.GaugeAcceleration;
import com.kircherelectronics.accelerationexplorer.gauge.GaugeRotation;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.view.View;
import android.view.Window;
import android.widget.TextView;

public class GaugeActivity extends Activity implements SensorEventListener
{
	private boolean meanFilterSmoothingEnabled;
	private boolean lpfSmoothingEnabled;

	private boolean lpfLinearAccelEnabled;
	private boolean androidLinearAccelEnabled;

	private boolean imuLaCfOrienationEnabled;
	private boolean imuLaCfRotationMatrixEnabled;
	private boolean imuLaCfQuaternionEnabled;

	// The acceleration, in units of meters per second, as measured by the
	// accelerometer.
	private float[] acceleration = new float[3];
	private float[] linearAcceleration = new float[3];
	private float[] magnetic = new float[3];
	private float[] rotation = new float[3];

	private GaugeAcceleration gaugeAcceleration;
	private GaugeRotation gaugeRotation;

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

	// Sensor manager to access the accelerometer
	private SensorManager sensorManager;

	// Text views for real-time output
	private TextView textViewXAxis;
	private TextView textViewYAxis;
	private TextView textViewZAxis;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.layout_gauge);

		textViewXAxis = (TextView) findViewById(R.id.value_x_axis);
		textViewYAxis = (TextView) findViewById(R.id.value_y_axis);
		textViewZAxis = (TextView) findViewById(R.id.value_z_axis);

		gaugeAcceleration = (GaugeAcceleration) findViewById(R.id.gauge_acceleration);
		gaugeRotation = (GaugeRotation) findViewById(R.id.gauge_rotation);

		meanFilterAccelSmoothing = new MeanFilterSmoothing();
		meanFilterMagneticSmoothing = new MeanFilterSmoothing();
		meanFilterRotationSmoothing = new MeanFilterSmoothing();

		lpfAccelSmoothing = new LowPassFilterSmoothing();
		lpfMagneticSmoothing = new LowPassFilterSmoothing();
		lpfRotationSmoothing = new LowPassFilterSmoothing();

		lpfLinearAcceleration = new LowPassFilterLinearAccel();

		sensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);

		handler = new Handler();

		runable = new Runnable()
		{
			@Override
			public void run()
			{
				handler.postDelayed(this, 100);

				updateAccelerationText();
				updateGauges();
			}
		};
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
		inflater.inflate(R.menu.menu_gauges, menu);
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
				linearAcceleration = lpfLinearAcceleration.addSamples(acceleration);
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
				linearAcceleration = lpfAccelSmoothing.addSamples(linearAcceleration);
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
				magnetic = meanFilterRotationSmoothing.addSamples(rotation);
			}

			if (lpfSmoothingEnabled)
			{
				magnetic = lpfRotationSmoothing.addSamples(rotation);
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
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

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
		lpfLinearAcceleration.setFilterCoefficient(getPrefLpfLinearAccelCoeff());

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

		View view = getLayoutInflater().inflate(R.layout.layout_help_gauges,
				null);

		helpDialog.setContentView(view);

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
			textViewXAxis.setText(String.format("%.2f", linearAcceleration[0]));
			textViewYAxis.setText(String.format("%.2f", linearAcceleration[1]));
			textViewZAxis.setText(String.format("%.2f", linearAcceleration[2]));
		}
	}

	private void updateGauges()
	{
		if (!lpfLinearAccelEnabled && !imuLaCfOrienationEnabled
				&& !imuLaCfRotationMatrixEnabled && !imuLaCfQuaternionEnabled
				&& !androidLinearAccelEnabled)
		{
			gaugeAcceleration.updatePoint(acceleration[0], acceleration[1],
					Color.rgb(255, 61, 0));
			gaugeRotation.updateRotation(acceleration);
		}
		else
		{
			gaugeAcceleration.updatePoint(linearAcceleration[0],
					linearAcceleration[1], Color.rgb(255, 61, 0));
			gaugeRotation.updateRotation(linearAcceleration);
		}
	}
}
