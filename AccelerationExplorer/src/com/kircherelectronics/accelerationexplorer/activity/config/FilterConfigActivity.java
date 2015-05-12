package com.kircherelectronics.accelerationexplorer.activity.config;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.util.Log;

import com.kircherelectronics.accelerationexplorer.R;

public class FilterConfigActivity extends PreferenceActivity implements
		OnSharedPreferenceChangeListener, OnPreferenceClickListener
{

	private static final String tag = FilterConfigActivity.class
			.getSimpleName();

	// Preference keys for smoothing filters
	public static final String MEAN_FILTER_SMOOTHING_ENABLED_KEY = "mean_filter_smoothing_enabled_preference";
	public static final String MEAN_FILTER_SMOOTHING_TIME_CONSTANT_KEY = "mean_filter_smoothing_time_constant_preference";
	public static final String LPF_SMOOTHING_ENABLED_KEY = "lpf_smoothing_enabled_preference";
	public static final String LPF_SMOOTHING_TIME_CONSTANT_KEY = "lpf_smoothing_time_constant_preference";

	// Preference keys for linear acceleration filters
	public static final String LPF_LINEAR_ACCEL_ENABLED_KEY = "lpf_linear_accel_enabled_preference";
	public static final String LPF_LINEAR_ACCEL_COEFF_KEY = "lpf_linear_accel_coeff_preference";

	public static final String ANDROID_LINEAR_ACCEL_ENABLED_KEY = "android_linear_accel_filter_preference";

	public static final String IMULACF_ORIENTATION_ENABLED_KEY = "imulacf_orienation_enabled_preference";
	public static final String IMULACF_ORIENTATION_COEFF_KEY = "imulacf_orienation_coeff_preference";
	
	public static final String IMULACF_ROTATION_MATRIX_ENABLED_KEY = "imulacf_rotation_matrix_enabled_preference";
	public static final String IMULACF_ROTATION_MATRIX_COEFF_KEY = "imulacf_rotation_matrix_coeff_preference";
	
	public static final String IMULACF_QUATERNION_ENABLED_KEY = "imulacf_quaternion_enabled_preference";
	public static final String IMULACF_QUATERNION_COEFF_KEY = "imulacf_quaternion_coeff_preference";
	
	private SwitchPreference spLpfLinearAccel;
	private SwitchPreference spAndroidLinearAccel;
	
	private SwitchPreference spImuLaCfOrientation;
	private SwitchPreference spImuLaCfRotationMatrix;
	private SwitchPreference spImuLaCfQuaternion;

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preference_filter);
		
		spLpfLinearAccel = (SwitchPreference) findPreference(LPF_LINEAR_ACCEL_ENABLED_KEY);
		
		spAndroidLinearAccel = (SwitchPreference) findPreference(ANDROID_LINEAR_ACCEL_ENABLED_KEY);
		
		spImuLaCfOrientation = (SwitchPreference) findPreference(IMULACF_ORIENTATION_ENABLED_KEY);
		
		spImuLaCfRotationMatrix = (SwitchPreference) findPreference(IMULACF_ROTATION_MATRIX_ENABLED_KEY);
		
		spImuLaCfQuaternion = (SwitchPreference) findPreference(IMULACF_QUATERNION_ENABLED_KEY);
	
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		// Set up a listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		// Unregister the listener whenever a key changes
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public boolean onPreferenceClick(Preference preference)
	{
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key)
	{	
		if (key.equals(LPF_LINEAR_ACCEL_ENABLED_KEY))
		{
			if (sharedPreferences.getBoolean(key, false))
			{
				Editor edit = sharedPreferences.edit();

				edit.putBoolean(IMULACF_ORIENTATION_ENABLED_KEY, false);
				edit.putBoolean(IMULACF_ROTATION_MATRIX_ENABLED_KEY, false);
				edit.putBoolean(IMULACF_QUATERNION_ENABLED_KEY, false);
				edit.putBoolean(ANDROID_LINEAR_ACCEL_ENABLED_KEY, false);

				edit.apply();
				
				spImuLaCfOrientation.setChecked(false);
				spImuLaCfRotationMatrix.setChecked(false);
				spImuLaCfQuaternion.setChecked(false);
				spAndroidLinearAccel.setChecked(false);
			}
		}

		if (key.equals(IMULACF_ORIENTATION_ENABLED_KEY))
		{
			if (sharedPreferences.getBoolean(key, false))
			{
				Editor edit = sharedPreferences.edit();

				edit.putBoolean(IMULACF_ROTATION_MATRIX_ENABLED_KEY, false);
				edit.putBoolean(IMULACF_QUATERNION_ENABLED_KEY, false);
				edit.putBoolean(LPF_LINEAR_ACCEL_ENABLED_KEY, false);
				edit.putBoolean(ANDROID_LINEAR_ACCEL_ENABLED_KEY, false);
				
				edit.apply();
				
				spImuLaCfRotationMatrix.setChecked(false);
				spImuLaCfQuaternion.setChecked(false);
				spLpfLinearAccel.setChecked(false);
				spAndroidLinearAccel.setChecked(false);
			}
		}
		
		if (key.equals(IMULACF_ROTATION_MATRIX_ENABLED_KEY))
		{
			if (sharedPreferences.getBoolean(key, false))
			{
				Editor edit = sharedPreferences.edit();

				edit.putBoolean(IMULACF_ORIENTATION_ENABLED_KEY, false);
				edit.putBoolean(IMULACF_QUATERNION_ENABLED_KEY, false);
				edit.putBoolean(LPF_LINEAR_ACCEL_ENABLED_KEY, false);
				edit.putBoolean(ANDROID_LINEAR_ACCEL_ENABLED_KEY, false);
				
				edit.apply();
				
				spImuLaCfOrientation.setChecked(false);
				spImuLaCfQuaternion.setChecked(false);
				spLpfLinearAccel.setChecked(false);
				spAndroidLinearAccel.setChecked(false);
			}
		}
		
		if (key.equals(IMULACF_QUATERNION_ENABLED_KEY))
		{
			if (sharedPreferences.getBoolean(key, false))
			{
				Editor edit = sharedPreferences.edit();

				edit.putBoolean(IMULACF_ORIENTATION_ENABLED_KEY, false);
				edit.putBoolean(IMULACF_ROTATION_MATRIX_ENABLED_KEY, false);
				edit.putBoolean(LPF_LINEAR_ACCEL_ENABLED_KEY, false);
				edit.putBoolean(ANDROID_LINEAR_ACCEL_ENABLED_KEY, false);
				
				edit.apply();
				
				spImuLaCfOrientation.setChecked(false);
				spImuLaCfRotationMatrix.setChecked(false);
				spLpfLinearAccel.setChecked(false);
				spAndroidLinearAccel.setChecked(false);
			}
		}

		if (key.equals(ANDROID_LINEAR_ACCEL_ENABLED_KEY))
		{
			if (sharedPreferences.getBoolean(key, false))
			{
				Editor edit = sharedPreferences.edit();

				edit.putBoolean(LPF_LINEAR_ACCEL_ENABLED_KEY, false);
				edit.putBoolean(IMULACF_ORIENTATION_ENABLED_KEY, false);
				edit.putBoolean(IMULACF_ROTATION_MATRIX_ENABLED_KEY, false);
				edit.putBoolean(IMULACF_QUATERNION_ENABLED_KEY, false);

				edit.apply();
				
				spImuLaCfOrientation.setChecked(false);
				spImuLaCfRotationMatrix.setChecked(false);
				spImuLaCfQuaternion.setChecked(false);
				spLpfLinearAccel.setChecked(false);
			}
		}
	}
}
