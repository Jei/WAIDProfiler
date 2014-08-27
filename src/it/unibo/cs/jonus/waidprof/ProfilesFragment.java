/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

/**
 * @author jei
 * 
 */
public class ProfilesFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_CATEGORY_PROFILES = "category_profiles";
	public static final String KEY_PREF_HISTORY_LENGTH = "pref_history_length";
	public static final String KEY_PREF_RESTORE_STATE = "pref_restore_state";
	public static final String KEY_TRANSITIONS_SCREEN = "transitions_screen";
	public static final String PREFIX_PROFILE = "profile_";
	public static final String SUFFIX_TRANSITIONS = "_transitions";
	public static final String SUFFIX_PREF_WIFI = "_pref_wifi";
	public static final String SUFFIX_PREF_BLUETOOTH = "_pref_bluetooth";
	public static final String SUFFIX_PREF_SPEAKERPHONE = "_pref_speakerphone";
	public static final String SUFFIX_PREF_ENABLED = "_pref_enabled";
	private static final int MAX_HISTORY_LENGTH = 50;
	private static final int MIN_HISTORY_LENGTH = 1;

	public ProfilesFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the application preferences
		addPreferencesFromResource(R.xml.waidprof_profiles);
	}

	@Override
	public void onResume() {
		super.onResume();

		// Recreate profiles screens
		PreferenceManager prefManager = getPreferenceManager();
		SharedPreferences prefs = prefManager.getSharedPreferences();
		PreferenceCategory profilesCategory = (PreferenceCategory) findPreference(KEY_CATEGORY_PROFILES);
		profilesCategory.removeAll();
		PreferenceScreen transitionsScreen = (PreferenceScreen) findPreference(KEY_TRANSITIONS_SCREEN);
		transitionsScreen.removeAll();

		// Get a set of vehicles and vehicle names
		Set<Map.Entry<String, Bitmap>> vehicleSet = ProfilerActivity.sVehiclesMap
				.entrySet();
		Set<String> vehicleNames = new HashSet<String>();
		for (Map.Entry<String, Bitmap> entry : vehicleSet) {
			vehicleNames.add(entry.getKey());
		}
		// Get the screens and transitions for all the vehicles
		for (Map.Entry<String, Bitmap> entry : vehicleSet) {
			String vehicle = entry.getKey();
			String profileKey = PREFIX_PROFILE + vehicle;
			PreferenceScreen prefScreen = prefManager
					.createPreferenceScreen(getActivity());
			prefScreen.setKey(profileKey);
			prefScreen.setTitle(vehicle);

			boolean enabled = prefs.getBoolean(vehicle + SUFFIX_PREF_ENABLED,
					false);
			boolean wifi = prefs.getBoolean(vehicle + SUFFIX_PREF_WIFI, false);
			boolean bluetooth = prefs.getBoolean(vehicle
					+ SUFFIX_PREF_BLUETOOTH, false);
			boolean speaker = prefs.getBoolean(vehicle
					+ SUFFIX_PREF_SPEAKERPHONE, false);
			Set<String> transitions = prefs.getStringSet(vehicle + SUFFIX_TRANSITIONS, new HashSet<String>());

			CheckBoxPreference enabledPref = new CheckBoxPreference(
					getActivity());
			CheckBoxPreference bluetoothPref = new CheckBoxPreference(
					getActivity());
			CheckBoxPreference speakerPref = new CheckBoxPreference(
					getActivity());
			CheckBoxPreference wifiPref = new CheckBoxPreference(getActivity());
			// Enabled preference
			enabledPref = new CheckBoxPreference(getActivity());
			enabledPref.setKey(vehicle + SUFFIX_PREF_ENABLED);
			enabledPref.setChecked(enabled);
			enabledPref.setTitle(R.string.pref_enabled);
			// Bluetooth preference
			bluetoothPref = new CheckBoxPreference(getActivity());
			bluetoothPref.setKey(vehicle + SUFFIX_PREF_BLUETOOTH);
			bluetoothPref.setChecked(bluetooth);
			bluetoothPref.setTitle(R.string.pref_bluetooth);
			// Speakerphone preference
			speakerPref = new CheckBoxPreference(getActivity());
			speakerPref.setKey(vehicle + SUFFIX_PREF_SPEAKERPHONE);
			speakerPref.setChecked(speaker);
			speakerPref.setTitle(R.string.pref_speakerphone);
			// Wifi preference
			wifiPref = new CheckBoxPreference(getActivity());
			wifiPref.setKey(vehicle + SUFFIX_PREF_WIFI);
			wifiPref.setChecked(wifi);
			wifiPref.setTitle(R.string.pref_wifi);

			// Add the preferences to the screen
			prefScreen.addPreference(enabledPref);
			prefScreen.addPreference(wifiPref);
			prefScreen.addPreference(bluetoothPref);
			prefScreen.addPreference(speakerPref);

			// Add the screen to the category
			profilesCategory.addPreference(prefScreen);
			wifiPref.setDependency(vehicle + SUFFIX_PREF_ENABLED);
			bluetoothPref.setDependency(vehicle + SUFFIX_PREF_ENABLED);
			speakerPref.setDependency(vehicle + SUFFIX_PREF_ENABLED);

			// Get transitions
			MultiSelectListPreference selectedVehicles = new MultiSelectListPreference(
					getActivity());
			selectedVehicles.setTitle(vehicle);
			selectedVehicles.setKey(vehicle + SUFFIX_TRANSITIONS);

			// Get the array of selectable vehicles
			Set<String> selectableVehicles = new HashSet<String>(vehicleNames);
			selectableVehicles.remove(vehicle);
			
			// Modify the existent values if needed
			Set<String> newTransitions = new HashSet<String>(transitions);
			newTransitions.retainAll(selectableVehicles);
			
			// Set entries and selected values
			String[] selectablesArray = selectableVehicles.toArray(new String[0]);
			selectedVehicles.setEntries(selectablesArray);
			selectedVehicles.setEntryValues(selectablesArray);
			selectedVehicles.setValues(newTransitions);
			// TODO add description
			
			// Add the transitions to the preference screen
			transitionsScreen.addPreference(selectedVehicles);
		}

		getPreferenceManager().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPause() {
		getPreferenceManager().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {

		// Validate the input for history length
		if (key.equals(KEY_PREF_HISTORY_LENGTH)) {
			// Get the changed preference
			EditTextPreference preference = (EditTextPreference) findPreference(key);
			String input = sharedPreferences.getString(key, "10");
			int value = Integer.parseInt(input);
			// Check if the input is too high or low
			if (value > MAX_HISTORY_LENGTH) {
				preference.setText("" + MAX_HISTORY_LENGTH);
				Toast.makeText(
						getActivity(),
						"" + getText(R.string.history_length_max_amount)
								+ MAX_HISTORY_LENGTH, Toast.LENGTH_SHORT)
						.show();
			} else if (value < MIN_HISTORY_LENGTH) {
				preference.setText("" + MIN_HISTORY_LENGTH);
				Toast.makeText(
						getActivity(),
						"" + getText(R.string.history_length_min_amount)
								+ MIN_HISTORY_LENGTH, Toast.LENGTH_SHORT)
						.show();
			}
		}

	}
}
