package com.kircherelectronics.accelerationexplorer.activity.config;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.kircherelectronics.accelerationexplorer.R;

/**
 * Created by navid on 1/2/16.
 */
public class ServerConfigActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener,OnPreferenceClickListener
{
    private static final String tag = ServerConfigActivity.class
            .getSimpleName();

    public static final String SERVER_IP_CONSTANT_KEY = "server_ip";
    public static final String SERVER_PORT_CONSTANT_KEY = "server_port";

    private EditTextPreference serverIP;
    private EditTextPreference serverPort;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference_server);


        serverIP = (EditTextPreference)findPreference(SERVER_IP_CONSTANT_KEY);
        serverPort = (EditTextPreference)findPreference(SERVER_PORT_CONSTANT_KEY);
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
    public boolean onPreferenceClick(Preference preference){
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key)
    {
//        if(key.equals(SERVER_IP_CONSTANT_KEY)){
//            Editor edit = sharedPreferences.edit();
//            String str = sharedPreferences.getString(String.valueOf(serverIP), null);
//
//            edit.putString(SERVER_IP_CONSTANT_KEY, str);
//            edit.commit();
//        }
//
//        if(key.equals(SERVER_PORT_CONSTANT_KEY)){
//            Editor edit = sharedPreferences.edit();
//            String str = sharedPreferences.getString(String.valueOf(serverPort), null);
//
//            edit.putString(SERVER_PORT_CONSTANT_KEY, str);
//            edit.commit();
//        }

    }

}
