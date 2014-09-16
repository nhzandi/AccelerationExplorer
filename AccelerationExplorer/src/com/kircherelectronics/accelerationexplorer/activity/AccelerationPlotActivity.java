package com.kircherelectronics.accelerationexplorer.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.XYPlot;
import com.kircherelectronics.accelerationexplorer.R;
import com.kircherelectronics.accelerationexplorer.dialog.SensorSettingsDialog;
import com.kircherelectronics.accelerationexplorer.gauge.GaugeAcceleration;
import com.kircherelectronics.accelerationexplorer.gauge.GaugeRotation;
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
public class AccelerationPlotActivity extends Activity implements
		SensorEventListener, Runnable, OnTouchListener, PlotPrefCallback
{

	// Plot keys for the acceleration plot
	private final static int PLOT_ACCEL_X_AXIS_KEY = 0;
	private final static int PLOT_ACCEL_Y_AXIS_KEY = 1;
	private final static int PLOT_ACCEL_Z_AXIS_KEY = 2;

	// Indicate if the output should be logged to a .csv file
	private boolean logData = false;
	private boolean run = false;
	private boolean dataReady = false;

	// Touch to zoom constants for the dynamicPlot
	private float distance = 0;
	private float zoom = 1.2f;

	// Outputs for the acceleration and LPFs
	private float[] acceleration = new float[3];

	// The generation of the log output
	private int generation = 0;

	// Color keys for the acceleration plot
	private int plotAccelXAxisColor;
	private int plotAccelYAxisColor;
	private int plotAccelZAxisColor;

	// The size of the sample window that determines RMS Amplitude Noise
	// (standard deviation)
	private final int std_dev_sample_window = 20;

	// Log output time stamp
	private long logTime = 0;

	// Decimal formats for the UI outputs
	private DecimalFormat df;

	// Graph plot for the UI outputs
	private DynamicLinePlot dynamicPlot;

	private GaugeAcceleration accelerationGauge;
	private GaugeRotation rotationGauge;

	// Handler for the UI plots so everything plots smoothly
	private Handler handler;

	// Icon to indicate logging is active
	private ImageView iconLogger;

	// Plot colors
	private PlotColor color;

	private Runnable runnable;

	// Sensor manager to access the accelerometer sensor
	private SensorManager sensorManager;
	
	private SensorSettingsDialog sensorSettingsDialog;

	// Acceleration plot titles
	private String plotAccelXAxisTitle = "AX";
	private String plotAccelYAxisTitle = "AY";
	private String plotAccelZAxisTitle = "AZ";

	// Output log
	private String log;
	
	private String frequencySelection;

	// Acceleration UI outputs
	private TextView xAxis;
	private TextView yAxis;
	private TextView zAxis;

	private Thread thread;

	/**
	 * Get the sample window size for the standard deviation.
	 * 
	 * @return Sample window size for the standard deviation.
	 */
	public int getSampleWindow()
	{
		return std_dev_sample_window;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.acceleration_plot_activity);

		// Read in the saved prefs
		readPrefs();

		initTextOutputs();

		initIcons();

		initColor();

		initPlots();

		initGauges();

		sensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);

		handler = new Handler();

		runnable = new Runnable()
		{
			@Override
			public void run()
			{
				handler.postDelayed(this, 100);

				plotData();
				updateGauges();
				updateAccelerationText();
			}
		};
	}

	@Override
	public void onPause()
	{
		super.onPause();

		sensorManager.unregisterListener(this);

		if (logData)
		{
			writeLogToFile();
		}

		if (run && thread != null)
		{
			run = false;

			thread.interrupt();

			thread = null;
		}

		handler.removeCallbacks(runnable);
	}

	@Override
	public void onResume()
	{
		super.onResume();

		readPrefs();
		readSensorPrefs();

		thread = new Thread(this);

		if (!run)
		{
			run = true;

			thread.start();
		}

		handler.post(runnable);
		
		updateSensorDelay();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		// Get a local copy of the sensor values
		System.arraycopy(event.values, 0, acceleration, 0, event.values.length);

		dataReady = true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.settings_plot_menu, menu);
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
		case R.id.menu_log:
			startDataLog();
			return true;

			// Start the diagnostic activity
		case R.id.menu_diagnostic:
			Intent diagnosticIntent = new Intent(this,
					AccelerationDiagnosticActivity.class);
			startActivity(diagnosticIntent);
			return true;
			
			// Log the data
		case R.id.action_settings_sensor:
			showSensorSettingsDialog();
			return true;

			// Start the vector activity
		case R.id.menu_vector:
			Intent vectorIntent = new Intent(this,
					AccelerationVectorActivity.class);
			startActivity(vectorIntent);
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
		while (run && !Thread.currentThread().isInterrupted())
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
	 * Initialize the activity icons.
	 */
	private void initIcons()
	{
		// Create the logger icon
		iconLogger = (ImageView) findViewById(R.id.icon_logger);
		iconLogger.setVisibility(View.INVISIBLE);
	}

	private void initGauges()
	{
		accelerationGauge = (GaugeAcceleration) findViewById(R.id.gauge_acceleration);

		rotationGauge = (GaugeRotation) findViewById(R.id.gauge_rotation);
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
		dynamicPlot = new DynamicLinePlot(plot);
		dynamicPlot.setMaxRange(20);
		dynamicPlot.setMinRange(-20);

		addAccelerationPlot();
	}

	/**
	 * Initialize the Text View sensor outputs.
	 */
	private void initTextOutputs()
	{
		// Format the UI outputs so they look nice
		df = new DecimalFormat("#.##");

		// Create the acceleration UI outputs
		xAxis = (TextView) findViewById(R.id.value_x_axis);
		yAxis = (TextView) findViewById(R.id.value_y_axis);
		zAxis = (TextView) findViewById(R.id.value_z_axis);
	}

	/**
	 * Plot the output data in the UI.
	 */
	private void plotData()
	{
		dynamicPlot.setData(acceleration[0], PLOT_ACCEL_X_AXIS_KEY);
		dynamicPlot.setData(acceleration[1], PLOT_ACCEL_Y_AXIS_KEY);
		dynamicPlot.setData(acceleration[2], PLOT_ACCEL_Z_AXIS_KEY);

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
	private void showSensorSettingsDialog()
	{
		if (sensorSettingsDialog == null)
		{
			sensorSettingsDialog = new SensorSettingsDialog(this, this);
			sensorSettingsDialog.setCancelable(true);
			sensorSettingsDialog.setCanceledOnTouchOutside(true);
		}

		sensorSettingsDialog.show();
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

			iconLogger.setVisibility(View.VISIBLE);

			logData = true;
		}
		else
		{
			iconLogger.setVisibility(View.INVISIBLE);

			logData = false;
			writeLogToFile();
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

			log += df.format((System.currentTimeMillis() - logTime) / 1000.0f)
					+ ",";

			log += acceleration[0] + ",";
			log += acceleration[1] + ",";
			log += acceleration[2] + ",";

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
	 * Read in the current user preferences.
	 */
	private void readPrefs()
	{
		SharedPreferences prefs = this.getSharedPreferences(
				"acceleration_explorer_prefs", Activity.MODE_PRIVATE);
	}

	/**
	 * Update the acceleration sensor output Text Views.
	 */
	private void updateAccelerationText()
	{
		// Update the view with the new acceleration data
		xAxis.setText(df.format(acceleration[0]));
		yAxis.setText(df.format(acceleration[1]));
		zAxis.setText(df.format(acceleration[2]));
	}

	private void updateGauges()
	{
		accelerationGauge.updatePoint(acceleration[0]
				/ SensorManager.GRAVITY_EARTH, acceleration[1]
				/ SensorManager.GRAVITY_EARTH, Color.parseColor("#33b5e5"));
		rotationGauge.updateRotation(acceleration);
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

			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

			// Register for sensor updates.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_NORMAL);
			break;
		case 1:

			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

			// Register for sensor updates.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_GAME);
			break;
		case 2:

			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

			// Register for sensor updates.
			sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
					SensorManager.SENSOR_DELAY_FASTEST);
			break;
		}
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
}
