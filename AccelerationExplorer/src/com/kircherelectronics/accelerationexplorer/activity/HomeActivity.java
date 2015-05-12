package com.kircherelectronics.accelerationexplorer.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.kircherelectronics.accelerationexplorer.R;
import com.kircherelectronics.accelerationexplorer.filter.MeanFilterSmoothing;

public class HomeActivity extends Activity implements SensorEventListener
{
	private final static String tag = HomeActivity.class.getSimpleName();
	
	// The acceleration, in units of meters per second, as measured by the
	// accelerometer.
	private float[] acceleration = new float[3];

	// Handler for the UI plots so everything plots smoothly
	private Handler handler;
	
	private MeanFilterSmoothing meanFilter;
	
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
		
		setContentView(R.layout.layout_home);

		textViewXAxis = (TextView) findViewById(R.id.value_x_axis);
		textViewYAxis = (TextView) findViewById(R.id.value_y_axis);
		textViewZAxis = (TextView) findViewById(R.id.value_z_axis);

		initButtonDiagnostic();
		initButtonGauge();
		initButtonLogger();
		initButtonNoise();
		initButtonVector();
		
		meanFilter = new MeanFilterSmoothing();
		meanFilter.setTimeConstant(0.2f);

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
		
		sensorManager.registerListener(this, sensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_NORMAL);
		
		handler.post(runable);
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			// Get a local copy of the acceleration measurements
			System.arraycopy(event.values, 0, acceleration, 0,
					event.values.length);
			
			acceleration = meanFilter.addSamples(acceleration);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{

	}
	
	private void initButtonGauge()
	{
		Button button = (Button) this.findViewById(R.id.button_gauge_mode);
		
		 button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) 
             {
                 Intent intent = new Intent(HomeActivity.this, GaugeActivity.class);

                 startActivity(intent);
             }
         });
	}
	
	private void initButtonDiagnostic()
	{
		Button button = (Button) this.findViewById(R.id.button_diagnostic_mode);
		
		 button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) 
             {
                 Intent intent = new Intent(HomeActivity.this, DiagnosticActivity.class);

                 startActivity(intent);
             }
         });
	}
	
	private void initButtonLogger()
	{
		Button button = (Button) this.findViewById(R.id.button_logger_mode);
		
		 button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) 
             {
                 Intent intent = new Intent(HomeActivity.this, LoggerActivity.class);

                 startActivity(intent);
             }
         });
	}
	
	private void initButtonNoise()
	{
		Button button = (Button) this.findViewById(R.id.button_noise_mode);
		
		 button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) 
             {
                 Intent intent = new Intent(HomeActivity.this, NoiseActivity.class);

                 startActivity(intent);
             }
         });
	}
	
	private void initButtonVector()
	{
		Button button = (Button) this.findViewById(R.id.button_vector_mode);
		
		 button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) 
             {
                 Intent intent = new Intent(HomeActivity.this, VectorActivity.class);

                 startActivity(intent);
             }
         });
	}

	private void updateAccelerationText()
	{
		// Update the acceleration data
		textViewXAxis.setText(String.format("%.2f", acceleration[0]));
		textViewYAxis.setText(String.format("%.2f", acceleration[1]));
		textViewZAxis.setText(String.format("%.2f", acceleration[2]));
	}
}
