package com.kircherelectronics.accelerationexplorer.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.XYPlot;
import com.kircherelectronics.accelerationexplorer.R;
import com.kircherelectronics.accelerationexplorer.activity.config.FilterConfigActivity;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfOrientation;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfQuaternion;
import com.kircherelectronics.accelerationexplorer.filter.ImuLaCfRotationMatrix;
import com.kircherelectronics.accelerationexplorer.filter.ImuLinearAccelerationInterface;
import com.kircherelectronics.accelerationexplorer.filter.LowPassFilterLinearAccel;
import com.kircherelectronics.accelerationexplorer.filter.LowPassFilterSmoothing;
import com.kircherelectronics.accelerationexplorer.filter.MeanFilterSmoothing;
import com.kircherelectronics.accelerationexplorer.plot.DynamicLinePlot;
import com.kircherelectronics.accelerationexplorer.plot.PlotColor;
import com.kircherelectronics.accelerationexplorer.plot.PlotPrefCallback;
import com.kircherelectronics.accelerationexplorer.prefs.PrefUtils;

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
 * An Activity that plots the three axes outputs of the acceleration sensor in
 * real-time, as well as displays the tilt of the device and acceleration of the
 * device in two-dimensions. The acceleration sensor can be logged to an
 * external .CSV file.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public class LoggerActivity extends Activity implements SensorEventListener,
		Runnable, OnTouchListener, PlotPrefCallback
{
	private final static String tag = LoggerActivity.class.getSimpleName();

	// Plot keys for the acceleration plot
	private final static int PLOT_ACCEL_X_AXIS_KEY = 0;
	private final static int PLOT_ACCEL_Y_AXIS_KEY = 1;
	private final static int PLOT_ACCEL_Z_AXIS_KEY = 2;

	// Indicate if the output should be logged to a .csv file
	private boolean logData = false;
	private boolean dataReady = false;

	private boolean meanFilterSmoothingEnabled;
	private boolean lpfSmoothingEnabled;

	private boolean lpfLinearAccelEnabled;
	private boolean androidLinearAccelEnabled;

	private boolean imuLaCfOrienationEnabled;
	private boolean imuLaCfRotationMatrixEnabled;
	private boolean imuLaCfQuaternionEnabled;

	// Touch to zoom constants for the dynamicPlot
	private float distance = 0;
	private float zoom = 1.2f;

	// Outputs for the acceleration and LPFs
	private float[] acceleration = new float[3];
	private float[] linearAcceleration = new float[3];
	private float[] magnetic = new float[3];
	private float[] rotation = new float[3];

	// The generation of the log output
	private int generation = 0;

	// Color keys for the acceleration plot
	private int plotAccelXAxisColor;
	private int plotAccelYAxisColor;
	private int plotAccelZAxisColor;

	// Log output time stamp
	private long logTime = 0;

	// Graph plot for the UI outputs
	private DynamicLinePlot dynamicPlot;

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

	// Plot colors
	private PlotColor color;

	private Runnable runable;

	// Sensor manager to access the accelerometer sensor
	private SensorManager sensorManager;

	// Acceleration plot titles
	private String plotAccelXAxisTitle = "AX";
	private String plotAccelYAxisTitle = "AY";
	private String plotAccelZAxisTitle = "AZ";

	// Output log
	private String log;

	private String frequencySelection;

	// Acceleration UI outputs
	private TextView textViewXAxis;
	private TextView textViewYAxis;
	private TextView textViewZAxis;

	private Thread thread;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.layout_logger);

		textViewXAxis = (TextView) findViewById(R.id.value_x_axis);
		textViewYAxis = (TextView) findViewById(R.id.value_y_axis);
		textViewZAxis = (TextView) findViewById(R.id.value_z_axis);

		initColor();
		initPlots();
		initStartButton();

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

				plotData();
				updateAccelerationText();
			}
		};
	}

	@Override
	public void onPause()
	{
		super.onPause();

		sensorManager.unregisterListener(this);

		stopDataLog();

		handler.removeCallbacks(runable);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		readSensorPrefs();

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

		handler.post(runable);

		updateSensorDelay();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

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

		dataReady = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_logger, menu);
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
			startIntentSensorSettings();
			return true;

			// Start the vector activity
		case R.id.action_help:
			showHelpDialog();
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Pinch to zoom.
	 */
	@Override
	public boolean onTouch(View v, MotionEvent e)
	{
		// MotionEvent reports input details from the touch screen
		// and other input controls.
		float newDist = 0;

		switch (e.getAction())
		{

		case MotionEvent.ACTION_MOVE:

			// pinch to zoom
			if (e.getPointerCount() == 2)
			{
				if (distance == 0)
				{
					distance = fingerDist(e);
				}

				newDist = fingerDist(e);

				zoom *= distance / newDist;

				dynamicPlot.setMaxRange(zoom * Math.log(zoom));
				dynamicPlot.setMinRange(-zoom * Math.log(zoom));

				distance = newDist;
			}
		}

		return false;
	}

	/**
	 * Output and logs are run on their own thread to keep the UI from hanging
	 * and the output smooth.
	 */
	@Override
	public void run()
	{
		while (logData && !Thread.currentThread().isInterrupted())
		{
			logData();
		}

		Thread.currentThread().interrupt();
	}

	/**
	 * Create the output graph line chart.
	 */
	private void addAccelerationPlot()
	{
		addGraphPlot(plotAccelXAxisTitle, PLOT_ACCEL_X_AXIS_KEY,
				plotAccelXAxisColor);
		addGraphPlot(plotAccelYAxisTitle, PLOT_ACCEL_Y_AXIS_KEY,
				plotAccelYAxisColor);
		addGraphPlot(plotAccelZAxisTitle, PLOT_ACCEL_Z_AXIS_KEY,
				plotAccelZAxisColor);
	}

	/**
	 * Add a plot to the graph.
	 * 
	 * @param title
	 *            The name of the plot.
	 * @param key
	 *            The unique plot key
	 * @param color
	 *            The color of the plot
	 */
	private void addGraphPlot(String title, int key, int color)
	{
		dynamicPlot.addSeriesPlot(title, key, color);
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

	/**
	 * Get the distance between fingers for the touch to zoom.
	 * 
	 * @param event
	 * @return
	 */
	private final float fingerDist(MotionEvent event)
	{
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float) Math.sqrt(x * x + y * y);
	}

	/**
	 * Create the plot colors.
	 */
	private void initColor()
	{
		color = new PlotColor(this);

		plotAccelXAxisColor = color.getDarkBlue();
		plotAccelYAxisColor = color.getDarkGreen();
		plotAccelZAxisColor = color.getDarkRed();
	}

	/**
	 * Initialize the plots.
	 */
	private void initPlots()
	{
		View view = findViewById(R.id.acceleration_plot_layout);
		view.setOnTouchListener(this);

		// Create the graph plot
		XYPlot plot = (XYPlot) findViewById(R.id.plot_sensor);
		plot.setTitle("Acceleration");
		dynamicPlot = new DynamicLinePlot(plot, this);
		dynamicPlot.setMaxRange(20);
		dynamicPlot.setMinRange(-20);

		addAccelerationPlot();
	}

	private void initStartButton()
	{
		final Button button = (Button) findViewById(R.id.button_start);

		button.setOnClickListener(new View.OnClickListener()
		{
			public void onClick(View v)
			{
				if (!logData)
				{
					button.setBackgroundResource(R.drawable.stop_button_background);
					button.setText("Stop Log");

					startDataLog();

					thread = new Thread(LoggerActivity.this);

					thread.start();
				}
				else
				{
					button.setBackgroundResource(R.drawable.start_button_background);
					button.setText("Start Log");

					stopDataLog();
				}
			}
		});
	}

	/**
	 * Plot the output data in the UI.
	 */
	private void plotData()
	{
		if (!lpfLinearAccelEnabled && !imuLaCfOrienationEnabled
				&& !imuLaCfRotationMatrixEnabled && !imuLaCfQuaternionEnabled
				&& !androidLinearAccelEnabled)
		{
			dynamicPlot.setData(acceleration[0], PLOT_ACCEL_X_AXIS_KEY);
			dynamicPlot.setData(acceleration[1], PLOT_ACCEL_Y_AXIS_KEY);
			dynamicPlot.setData(acceleration[2], PLOT_ACCEL_Z_AXIS_KEY);
		}
		else
		{
			dynamicPlot.setData(linearAcceleration[0], PLOT_ACCEL_X_AXIS_KEY);
			dynamicPlot.setData(linearAcceleration[1], PLOT_ACCEL_Y_AXIS_KEY);
			dynamicPlot.setData(linearAcceleration[2], PLOT_ACCEL_Z_AXIS_KEY);
		}

		dynamicPlot.draw();
	}

	/**
	 * Remove a plot from the graph.
	 * 
	 * @param key
	 */
	private void removeGraphPlot(int key)
	{
		dynamicPlot.removeSeriesPlot(key);
	}

	/**
	 * Show a settings dialog.
	 */
	private void startIntentSensorSettings()
	{
		Intent intent = new Intent(LoggerActivity.this,
				FilterConfigActivity.class);

		startActivity(intent);
	}

	/**
	 * Begin logging data to an external .csv file.
	 */
	private void startDataLog()
	{
		if (logData == false)
		{
			generation = 0;

			CharSequence text = "Logging Data";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();

			String headers = "Generation" + ",";

			headers += "Timestamp" + ",";

			headers += this.plotAccelXAxisTitle + ",";

			headers += this.plotAccelYAxisTitle + ",";

			headers += this.plotAccelZAxisTitle + ",";

			log = headers;

			log += System.getProperty("line.separator");

			logData = true;
		}
	}

	private void stopDataLog()
	{
		if (logData)
		{
			writeLogToFile();
		}

		if (logData && thread != null)
		{
			logData = false;

			thread.interrupt();

			thread = null;
		}
	}

	/**
	 * Log output data to an external .csv file.
	 */
	private void logData()
	{
		if (logData && dataReady)
		{
			if (generation == 0)
			{
				logTime = System.currentTimeMillis();
			}

			log += generation++ + ",";

			log += String.format("%.2f",
					(System.currentTimeMillis() - logTime) / 1000.0f) + ",";

			if (!lpfLinearAccelEnabled && !imuLaCfOrienationEnabled
					&& !imuLaCfRotationMatrixEnabled
					&& !imuLaCfQuaternionEnabled && !androidLinearAccelEnabled)
			{
				log += acceleration[0] + ",";
				log += acceleration[1] + ",";
				log += acceleration[2] + ",";
			}
			else
			{
				log += linearAcceleration[0] + ",";
				log += linearAcceleration[1] + ",";
				log += linearAcceleration[2] + ",";
			}

			log += System.getProperty("line.separator");

			dataReady = false;
		}
	}

	/**
	 * Write the logged data out to a persisted file.
	 */
	private void writeLogToFile()
	{
		Calendar c = Calendar.getInstance();
		String filename = "AccelerationExplorer-" + c.get(Calendar.YEAR) + "-"
				+ (c.get(Calendar.MONTH) + 1) + "-"
				+ c.get(Calendar.DAY_OF_MONTH) + "-" + c.get(Calendar.HOUR)
				+ "-" + c.get(Calendar.MINUTE) + "-" + c.get(Calendar.SECOND)
				+ ".csv";

		File dir = new File(Environment.getExternalStorageDirectory()
				+ File.separator + "AccelerationExplorer" + File.separator
				+ "Logs");
		if (!dir.exists())
		{
			dir.mkdirs();
		}

		File file = new File(dir, filename);

		FileOutputStream fos;
		byte[] data = log.getBytes();
		try
		{
			fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();

			CharSequence text = "Log Saved";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
		catch (FileNotFoundException e)
		{
			CharSequence text = e.toString();
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(this, text, duration);
			toast.show();
		}
		catch (IOException e)
		{
			// handle exception
		}
		finally
		{
			// Update the MediaStore so we can view the file without rebooting.
			// Note that it appears that the ACTION_MEDIA_MOUNTED approach is
			// now blocked for non-system apps on Android 4.4.
			MediaScannerConnection.scanFile(this, new String[]
			{ file.getPath() }, null,
					new MediaScannerConnection.OnScanCompletedListener()
					{
						@Override
						public void onScanCompleted(final String path,
								final Uri uri)
						{

						}
					});
		}
	}

	/**
	 * Update the acceleration sensor output Text Views.
	 */
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
	public void checkPlotPrefs()
	{
		readSensorPrefs();
		updateSensorDelay();
	}

	/**
	 * Set the sensor delay based on user preferences. 0 = slow, 1 = medium, 2 =
	 * fast.
	 * 
	 * @param position
	 *            The desired sensor delay.
	 */
	private void setSensorDelay(int position)
	{
		switch (position)
		{
		case 0:

			if (!androidLinearAccelEnabled)
			{
				// Register for sensor updates.
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_NORMAL);
			}
			else
			{
				// Register for sensor updates.
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
						SensorManager.SENSOR_DELAY_NORMAL);
			}

			if ((imuLaCfOrienationEnabled || imuLaCfRotationMatrixEnabled || imuLaCfQuaternionEnabled)
					&& !androidLinearAccelEnabled)
			{

				// Register for sensor updates.
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
						SensorManager.SENSOR_DELAY_NORMAL);

				// Register for sensor updates.
				sensorManager.registerListener(this,
						sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
						SensorManager.SENSOR_DELAY_NORMAL);
			}

			break;
		case 1:

			if (!androidLinearAccelEnabled)
			{

				// Register for sensor updates.
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_GAME);
			}
			else
			{

				// Register for sensor updates.
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
						SensorManager.SENSOR_DELAY_GAME);
			}

			if ((imuLaCfOrienationEnabled || imuLaCfRotationMatrixEnabled || imuLaCfQuaternionEnabled)
					&& !androidLinearAccelEnabled)
			{

				// Register for sensor updates.
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
						SensorManager.SENSOR_DELAY_GAME);

				// Register for sensor updates.
				sensorManager.registerListener(this,
						sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
						SensorManager.SENSOR_DELAY_GAME);
			}

			break;
		case 2:

			if (!androidLinearAccelEnabled)
			{

				// Register for sensor updates.
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
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
				sensorManager.registerListener(this, sensorManager
						.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
						SensorManager.SENSOR_DELAY_FASTEST);

				// Register for sensor updates.
				sensorManager.registerListener(this,
						sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

			break;
		}
	}

	/**
	 * Read in the current user preferences.
	 */
	private void readSensorPrefs()
	{
		SharedPreferences prefs = this.getSharedPreferences(
				PrefUtils.SENSOR_PREFS, Activity.MODE_PRIVATE);

		this.frequencySelection = prefs.getString(
				PrefUtils.SENSOR_FREQUENCY_PREF,
				PrefUtils.SENSOR_FREQUENCY_FAST);
	}

	private void showHelpDialog()
	{
		Dialog helpDialog = new Dialog(this);

		helpDialog.setCancelable(true);
		helpDialog.setCanceledOnTouchOutside(true);
		helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

		View view = getLayoutInflater().inflate(R.layout.layout_help_logger,
				null);

		helpDialog.setContentView(view);

		helpDialog.show();
	}

	/**
	 * Updates the sensor delay based on the user preference. 0 = slow, 1 =
	 * medium, 2 = fast.
	 */
	private void updateSensorDelay()
	{
		if (frequencySelection.equals(PrefUtils.SENSOR_FREQUENCY_SLOW))
		{
			setSensorDelay(0);
		}

		if (frequencySelection.equals(PrefUtils.SENSOR_FREQUENCY_MEDIUM))
		{
			setSensorDelay(1);
		}

		if (frequencySelection.equals(PrefUtils.SENSOR_FREQUENCY_FAST))
		{
			setSensorDelay(2);
		}
	}
}
