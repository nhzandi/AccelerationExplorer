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
import com.kircherelectronics.accelerationexplorer.gauge.GaugeAccelerationHolo;
import com.kircherelectronics.accelerationexplorer.gauge.GaugeRotationHolo;
import com.kircherelectronics.accelerationexplorer.plot.DynamicLinePlot;
import com.kircherelectronics.accelerationexplorer.plot.PlotColor;

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
 * Implements an Activity that is intended to run filters on accelerometer
 * inputs and then graph the outputs. The user can select which filters should
 * be used and set key parameters for each filter.
 * 
 * Currently supports an IIR digital low-pass filter. The low-pass filters are
 * classified as recursive, or infinite response filters (IIR). The current, nth
 * sample output depends on both current and previous inputs as well as previous
 * outputs. It is essentially a weighted moving average, which comes in many
 * different flavors depending on the values for the coefficients, a and b. The
 * low-pass filter, the Wikipedia LPF, is an IIR single-pole implementation. The
 * coefficient, a (alpha), can be adjusted based on the sample period of the
 * sensor to produce the desired time constant that the filter will act on. It
 * takes a simple form of y[i] = y[i] + alpha * (x[i] - y[i]). Alpha is defined
 * as alpha = dt / (timeConstant + dt);) where the time constant is the length
 * of signals the filter should act on and dt is the sample period (1/frequency)
 * of the sensor.
 * 
 * A finite impulse response (FIR) moving average filter is also implemented.
 * This filter tends to be extremely effective at removing noise from the
 * signal, much more so than the low-pass filter.
 * 
 * @author Kaleb
 * @version %I%, %G%
 */
public class AccelerationExplorerActivity extends Activity implements
		SensorEventListener, Runnable, OnTouchListener
{
	// The size of the sample window that determines RMS Amplitude Noise
	// (standard deviation)
	private static int STD_DEV_SAMPLE_WINDOW = 20;

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

	// Log output time stamp
	private long logTime = 0;

	// Decimal formats for the UI outputs
	private DecimalFormat df;

	// Graph plot for the UI outputs
	private DynamicLinePlot dynamicPlot;

	private GaugeAccelerationHolo accelerationGauge;
	private GaugeRotationHolo rotationGauge;

	// Handler for the UI plots so everything plots smoothly
	private Handler handler;

	private Thread thread;

	// Icon to indicate logging is active
	private ImageView iconLogger;

	// Plot colors
	private PlotColor color;

	private Runnable runnable;

	// Sensor manager to access the accelerometer sensor
	private SensorManager sensorManager;

	// Acceleration plot titles
	private String plotAccelXAxisTitle = "AX";
	private String plotAccelYAxisTitle = "AY";
	private String plotAccelZAxisTitle = "AZ";

	// Output log
	private String log;

	// Acceleration UI outputs
	private TextView xAxis;
	private TextView yAxis;
	private TextView zAxis;

	/**
	 * Get the sample window size for the standard deviation.
	 * 
	 * @return Sample window size for the standard deviation.
	 */
	public static int getSampleWindow()
	{
		return STD_DEV_SAMPLE_WINDOW;
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.plot_sensor_activity);

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

		thread = new Thread(this);

		if (!run)
		{
			run = true;

			thread.start();
		}

		handler.post(runnable);

		// Register for sensor updates.
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub

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
		inflater.inflate(R.menu.settings_logger_menu, menu);
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

			// Log the data
		case R.id.menu_diagnostic:
			Intent intent = new Intent(this,
					AccelerationDiagnosticActivity.class);
			startActivity(intent);
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
		accelerationGauge = (GaugeAccelerationHolo) findViewById(R.id.gauge_acceleration);

		rotationGauge = (GaugeRotationHolo) findViewById(R.id.gauge_rotation);
	}

	/**
	 * Initialize the plots.
	 */
	private void initPlots()
	{
		View view = findViewById(R.id.ScrollView01);
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
			
			log += df.format((System.currentTimeMillis() - logTime) / 1000.0f) + ",";

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
				+ c.get(Calendar.DAY_OF_WEEK_IN_MONTH) + "-"
				+ c.get(Calendar.HOUR) + "-" + c.get(Calendar.MINUTE) + "-"
				+ c.get(Calendar.SECOND) + ".csv";

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
		accelerationGauge.updatePoint(acceleration[0]/SensorManager.GRAVITY_EARTH, acceleration[1]/SensorManager.GRAVITY_EARTH,
				Color.parseColor("#33b5e5"));
		rotationGauge.updateRotation(acceleration);
	}
}
