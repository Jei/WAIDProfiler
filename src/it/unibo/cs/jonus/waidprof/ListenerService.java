/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * @author jei
 * 
 */
public class ListenerService extends Service {

	public static final String KEY_SERVICE_ISRUNNING = "listener_isrunning";
	public static final String KEY_PREVIOUS_STATE_WIFI = "previous_state_wifi";
	public static final String KEY_PREVIOUS_STATE_BLUETOOTH = "previous_state_bluetooth";
	public static final String KEY_PREVIOUS_STATE_SPEAKERPHONE = "previous_state_speakerphone";

	// Constants used to read Content Provider
	private static final String AUTHORITY = "it.unibo.cs.jonus.waidrec.evaluationsprovider";
	private static final String BASE_PATH = "evaluations";
	private static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + BASE_PATH);
	private static final String LAST_EVALUATION_PATH = "/last";
	private static final String EVALUATION_COLUMN_ID = "_id";
	private static final String EVALUATION_COLUMN_CATEGORY = "category";
	private static final String EVALUATION_COLUMN_TIMESTAMP = "timestamp";

	// Service binding variables
	private ListenerServiceListener mListener = null;
	private final IBinder mBinder = new ListenerServiceBinder();

	// Content Observer variables
	private Handler handler = new Handler();
	private EvaluationsContentObserver evaluationsObserver = null;
	private ArrayList<Evaluation> evaluationList;

	// Android managers
	private WifiManager wifiManager;
	private AudioManager audioManager;
	private BluetoothAdapter bluetoothAdapter;

	SharedPreferences sharedPrefs;

	/**
	 * Class used to listen to changes in the Evaluations Content Provider
	 */
	class EvaluationsContentObserver extends ContentObserver {

		public EvaluationsContentObserver(Handler handler) {
			super(handler);
		}

		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			// Get the last evaluation from the Content Provider
			String[] projection = { EVALUATION_COLUMN_ID,
					EVALUATION_COLUMN_TIMESTAMP, EVALUATION_COLUMN_CATEGORY };
			Uri uri = Uri.parse(CONTENT_URI + LAST_EVALUATION_PATH);
			Cursor cursor = getContentResolver().query(uri, projection, null,
					null, null);
			if (cursor == null) {
				return;
			}
			cursor.moveToFirst();
			long id = cursor.getLong(cursor
					.getColumnIndexOrThrow(EVALUATION_COLUMN_ID));
			String category = cursor.getString(cursor
					.getColumnIndexOrThrow(EVALUATION_COLUMN_CATEGORY));
			long timestamp = cursor.getLong(cursor
					.getColumnIndexOrThrow(EVALUATION_COLUMN_TIMESTAMP));
			// Insert the evaluation in the evaluations array
			Evaluation newEvaluation = new Evaluation(id, timestamp, category);
			evaluationList.add(newEvaluation);
			// Trim the array if needed
			String historyLengthString = sharedPrefs.getString(
					ProfilesFragment.KEY_PREF_HISTORY_LENGTH, "10");
			int historyLength = Integer.parseInt(historyLengthString);
			while (evaluationList.size() > historyLength) {
				evaluationList.remove(0);
			}
			// Get the most recurring vehicle in the history
			String mrVehicle = getMostRecurringVehicle();

			// Send the new evaluation to the listening activities.
			if (mListener != null) {
				mListener.sendCurrentEvaluation(newEvaluation);
				mListener.sendPredictedVehicle(mrVehicle);
			}

			// Update the state of the device
			updateDeviceState(mrVehicle);
		}

	}

	/**
	 * Class used for the client Binder.
	 */
	public class ListenerServiceBinder extends Binder {
		public ListenerService getService() {
			// Return this instance of ListenerService so clients can call
			// public methods
			return ListenerService.this;
		}

		public void setListener(ListenerServiceListener listener) {
			mListener = listener;
		}
	}

	public ListenerService() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onBind(android.content.Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		evaluationList = new ArrayList<Evaluation>();

		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Persist the service state in the shared preferences
		sharedPrefs.edit().putBoolean(KEY_SERVICE_ISRUNNING, true).commit();

		// Get default managers
		wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
		audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Save the initial state of the device
		saveInitialState();

		// Register the content observer
		registerContentObserver();

		Toast.makeText(getApplicationContext(),
				getText(R.string.listener_service_started), Toast.LENGTH_SHORT)
				.show();
	}

	@Override
	public void onDestroy() {
		// Unregister the content observer
		unregisterContentObserver();

		// Restore the state of the device if the preference is set
		if (sharedPrefs.getBoolean(ProfilesFragment.KEY_PREF_RESTORE_STATE,
				true)) {
			restoreInitialState();
		}

		// Persist the service state in the shared preferences
		sharedPrefs.edit().putBoolean(KEY_SERVICE_ISRUNNING, false).commit();

		Toast.makeText(getApplicationContext(),
				getText(R.string.listener_service_stopped), Toast.LENGTH_SHORT)
				.show();

		super.onDestroy();
	}

	private void registerContentObserver() {
		ContentResolver cr = getContentResolver();
		evaluationsObserver = new EvaluationsContentObserver(handler);
		cr.registerContentObserver(CONTENT_URI, true, evaluationsObserver);
	}

	private void unregisterContentObserver() {
		ContentResolver cr = getContentResolver();
		if (evaluationsObserver != null) {
			cr.unregisterContentObserver(evaluationsObserver);
			evaluationsObserver = null;
		}
	}

	/**
	 * Changes the state of wi-fi, bluetooth and speakerphone according to the
	 * settings for the specified vehicle
	 * 
	 * @param vehicle
	 */
	private void updateDeviceState(String vehicle) {
		// Get the preferences for this vehicle
		boolean wifiState = sharedPrefs.getBoolean(vehicle
				+ ProfilesFragment.SUFFIX_PREF_WIFI, false);
		boolean bluetoothState = sharedPrefs.getBoolean(vehicle
				+ ProfilesFragment.SUFFIX_PREF_BLUETOOTH, false);
		boolean speakerphoneState = sharedPrefs.getBoolean(vehicle
				+ ProfilesFragment.SUFFIX_PREF_SPEAKERPHONE, false);

		// Update the state
		if (wifiManager != null) {
			wifiManager.setWifiEnabled(wifiState);
		}
		if (audioManager != null) {
			audioManager.setSpeakerphoneOn(speakerphoneState);
		}
		if (bluetoothAdapter != null) {
			if (bluetoothState) {
				bluetoothAdapter.enable();
			} else {
				bluetoothAdapter.disable();
			}
		}
	}

	/**
	 * Calculates the most recurring vehicle in the history
	 * 
	 * @return the most recurring vehicle
	 */
	private String getMostRecurringVehicle() {
		String mrVehicle = null;

		// Get the history length from the shared preferences
		String historyLengthString = sharedPrefs.getString(
				ProfilesFragment.KEY_PREF_HISTORY_LENGTH, "10");
		int historyLength = Integer.parseInt(historyLengthString);

		HashMap<String, Integer> vehicleMap = new HashMap<String, Integer>();
		int maxNumber = 0;
		// get mrVehicle from last evaluation, in case the history length is 0
		mrVehicle = evaluationList.get(evaluationList.size() - 1).getCategory();
		for (int i = 1; (i <= evaluationList.size()) && (i <= historyLength); i++) {
			// get the list elements starting from the most recent
			String vehicle = evaluationList.get(evaluationList.size() - i)
					.getCategory();
			// if the map doesn't contain this vehicle yet, create the key
			if (!vehicleMap.containsKey(vehicle)) {
				vehicleMap.put(vehicle, 0);
			}
			// get the number of recurrences from this vehicle and increment it
			int newCount = vehicleMap.get(vehicle) + 1;
			vehicleMap.put(vehicle, newCount);
			// check if the count for this vehicle is the new maximum
			if (newCount > maxNumber) {
				maxNumber = newCount;
				mrVehicle = vehicle;
			}
		}

		return mrVehicle;
	}

	/**
	 * Saves the state of the device prior to service run to the shared
	 * preferences
	 */
	private void saveInitialState() {
		// Wi-Fi
		if ((wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED)
				|| (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING)) {
			sharedPrefs.edit().putBoolean(KEY_PREVIOUS_STATE_WIFI, true)
					.commit();
		} else {
			sharedPrefs.edit().putBoolean(KEY_PREVIOUS_STATE_WIFI, false)
					.commit();
		}
		// Speakerphone
		sharedPrefs
				.edit()
				.putBoolean(KEY_PREVIOUS_STATE_SPEAKERPHONE,
						audioManager.isSpeakerphoneOn()).commit();
		// Bluetooth
		if (bluetoothAdapter != null) {
			sharedPrefs
					.edit()
					.putBoolean(KEY_PREVIOUS_STATE_BLUETOOTH,
							bluetoothAdapter.isEnabled()).commit();
		}
	}

	/**
	 * Loads the previous state of the device from the shared preferences
	 */
	private void restoreInitialState() {
		// Wi-Fi
		if (wifiManager != null) {
			wifiManager.setWifiEnabled(sharedPrefs.getBoolean(
					KEY_PREVIOUS_STATE_WIFI, false));
		}
		// Speakerphone
		if (audioManager != null) {
			audioManager.setSpeakerphoneOn(sharedPrefs.getBoolean(
					KEY_PREVIOUS_STATE_SPEAKERPHONE, false));
		}
		// Bluetooth
		if (bluetoothAdapter != null) {
			if (sharedPrefs.getBoolean(KEY_PREVIOUS_STATE_BLUETOOTH, false)) {
				bluetoothAdapter.enable();
			} else {
				bluetoothAdapter.disable();
			}
		}
	}
}
