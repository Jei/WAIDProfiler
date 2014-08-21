/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
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

	// Service binding variables
	private ListenerServiceListener mListener = null;
	private final IBinder mBinder = new ListenerServiceBinder();

	// Content Observer variables
	private Handler handler = new Handler();
	private VehicleInstancesContentObserver evaluationsObserver = null;
	private ArrayList<VehicleInstance> evaluationList;
	private String lastMrVehicle;

	// Android managers
	private WifiManager wifiManager;
	private AudioManager audioManager;
	private BluetoothAdapter bluetoothAdapter;
	private NotificationManager notificationManager;

	SharedPreferences sharedPrefs;

	private Map<String, Bitmap> vehicleMiniaturesMap = new HashMap<String, Bitmap>();

	/**
	 * Class used to listen to changes in the VehicleInstances Content Provider
	 */
	class VehicleInstancesContentObserver extends ContentObserver {

		public VehicleInstancesContentObserver(Handler handler) {
			super(handler);
		}

		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			// Get the last evaluation from the Content Provider
			Uri uri = Uri.parse(ProfilerActivity.EVALUATIONS_URI
					+ ProfilerActivity.PATH_LAST_EVALUATION);
			Cursor cursor = getContentResolver().query(uri,
					ProfilerActivity.EvaluationColumnsProjection, null, null,
					null);
			if (cursor == null) {
				return;
			}
			// Insert the evaluation in the evaluations array
			cursor.moveToFirst();
			VehicleInstance newVehicleInstance = ProfilerActivity
					.cursorToVehicleInstance(cursor);
			cursor.close();
			evaluationList.add(newVehicleInstance);
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
				mListener.sendCurrentEvaluation(newVehicleInstance);
				mListener.sendPredictedVehicle(mrVehicle);
			}

			// Update the state of the device
			updateDeviceState(mrVehicle);

			// Show a notification of the current mode
			if (!mrVehicle.equals(lastMrVehicle)) {
				showVehicleNotification(mrVehicle);
				lastMrVehicle = mrVehicle;
			}
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

		evaluationList = new ArrayList<VehicleInstance>();

		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Persist the service state in the shared preferences
		sharedPrefs.edit().putBoolean(KEY_SERVICE_ISRUNNING, true).commit();

		// Get default managers
		wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
		audioManager = (AudioManager) this.getSystemService(AUDIO_SERVICE);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		notificationManager = (NotificationManager) this
				.getSystemService(NOTIFICATION_SERVICE);

		// Save the initial state of the device
		saveInitialState();

		// Register the content observer
		registerContentObserver();

		// Create the miniature icons for the vehicles
		for (Map.Entry<String, Bitmap> entry : ProfilerActivity.sVehiclesMap.entrySet()) {
			Bitmap vehicleIcon = entry.getValue();
			vehicleIcon = scale(vehicleIcon);
			vehicleIcon = invert(vehicleIcon);

			vehicleMiniaturesMap.put(entry.getKey(), vehicleIcon);
		}

		lastMrVehicle = "none";

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

		// Hide the notification for the vehicle mode
		hideVehicleNotification();

		Toast.makeText(getApplicationContext(),
				getText(R.string.listener_service_stopped), Toast.LENGTH_SHORT)
				.show();

		super.onDestroy();
	}

	private void registerContentObserver() {
		ContentResolver cr = getContentResolver();
		evaluationsObserver = new VehicleInstancesContentObserver(handler);
		cr.registerContentObserver(ProfilerActivity.EVALUATIONS_URI, true,
				evaluationsObserver);
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
		boolean enabled = sharedPrefs.getBoolean(vehicle
				+ ProfilesFragment.SUFFIX_PREF_ENABLED, false);
		boolean wifiState = sharedPrefs.getBoolean(vehicle
				+ ProfilesFragment.SUFFIX_PREF_WIFI, false);
		boolean bluetoothState = sharedPrefs.getBoolean(vehicle
				+ ProfilesFragment.SUFFIX_PREF_BLUETOOTH, false);
		boolean speakerphoneState = sharedPrefs.getBoolean(vehicle
				+ ProfilesFragment.SUFFIX_PREF_SPEAKERPHONE, false);

		if (enabled) {
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

	/**
	 * Show a notification for the given vehicle
	 * 
	 * @param textId
	 * @param vehicle
	 */
	private void showVehicleNotification(String vehicle) {
		// Hide the previous notification
		notificationManager.cancel(R.string.notification_mode_vehicle);

		CharSequence text = getText(R.string.notification_mode_vehicle)
				+ vehicle;
		CharSequence title = getText(R.string.listener_service_label);

		// Create a pending intent to open the activity
		Intent profilerIntent = new Intent(this, ProfilerActivity.class);
		profilerIntent.setAction(Intent.ACTION_MAIN);
		profilerIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				profilerIntent, 0);

		Bitmap vehicleIcon = vehicleMiniaturesMap.get(vehicle);

		// Build the notification
		// TODO create small icon
		@SuppressWarnings("deprecation")
		Notification notification = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher).setLargeIcon(vehicleIcon)
				.setContentText(text).setContentTitle(title)
				.setContentIntent(contentIntent).setAutoCancel(false)
				.setOngoing(true).getNotification();

		// Send the notification
		notificationManager.notify(R.string.notification_mode_vehicle,
				notification);
	}

	/**
	 * Hide the notification for the vehicle mode
	 * 
	 * @param textId
	 */
	private void hideVehicleNotification() {
		notificationManager.cancel(R.string.notification_mode_vehicle);
	}

	// XXX do i need this?
	public static Bitmap invert(Bitmap src) {
		Bitmap output = Bitmap.createBitmap(src.getWidth(), src.getHeight(),
				src.getConfig());
		int A, R, G, B;
		int pixelColor;
		int height = src.getHeight();
		int width = src.getWidth();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				pixelColor = src.getPixel(x, y);
				A = Color.alpha(pixelColor);

				R = 255 - Color.red(pixelColor);
				G = 255 - Color.green(pixelColor);
				B = 255 - Color.blue(pixelColor);

				output.setPixel(x, y, Color.argb(A, R, G, B));
			}
		}

		return output;
	}

	// XXX do i need this?
	public static Bitmap scale(Bitmap src) {
		int height = Resources.getSystem().getDimensionPixelSize(
				android.R.dimen.notification_large_icon_height);
		int width = Resources.getSystem().getDimensionPixelSize(
				android.R.dimen.notification_large_icon_width);
		Bitmap output = Bitmap.createScaledBitmap(src, height, width, false);

		return output;
	}

}
