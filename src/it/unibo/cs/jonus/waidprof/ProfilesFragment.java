/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

/**
 * @author jei
 * 
 */
public class ProfilesFragment extends PreferenceFragment implements
		OnSharedPreferenceChangeListener {

	public static final String KEY_PREF_HISTORY_LENGTH = "pref_history_length";
	public static final String KEY_PREF_RESTORE_STATE = "pref_restore_state";
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

		// Load the profiles preferences
		addPreferencesFromResource(R.xml.waidprof_profiles);
	}

	@Override
	public void onResume() {
		super.onResume();
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
