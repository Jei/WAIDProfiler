/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * @author jei
 * 
 */
public class ProfilesFragment extends PreferenceFragment {

	public ProfilesFragment() {

	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Load the profiles preferences
		addPreferencesFromResource(R.xml.waidprof_profiles);
	}

}
