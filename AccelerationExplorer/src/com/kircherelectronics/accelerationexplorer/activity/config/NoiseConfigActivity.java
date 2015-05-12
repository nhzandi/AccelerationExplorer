package com.kircherelectronics.accelerationexplorer.activity.config;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.kircherelectronics.accelerationexplorer.R;

public class NoiseConfigActivity extends PreferenceActivity
{
	public static final String MEAN_FILTER_SMOOTHING_TIME_CONSTANT_KEY = "noise_mean_filter_smoothing_time_constant_preference";
	public static final String MEDIAN_FILTER_SMOOTHING_TIME_CONSTANT_KEY = "noise_median_filter_smoothing_time_constant_preference";
	public static final String LPF_SMOOTHING_TIME_CONSTANT_KEY = "noise_lpf_smoothing_time_constant_preference";
	

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preference_noise_filter);
	}
}
