/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

import java.util.HashMap;
import java.util.Map;

import it.unibo.cs.jonus.waidprof.ListenerService.ListenerServiceBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

/**
 * @author jei
 * 
 */
public class ListenerFragment extends Fragment {

	private static final int MODE_IDLE = 0;
	private static final int MODE_RUNNING = 1;

	// UI elements
	private Map<String, ImageView> vehicleViewsMap;
	private Map<String, Integer> vehicleImagesMap;
	private Button startServiceButton;
	private Button stopServiceButton;
	// UI variables
	private String currentClassification;

	// Service variables
	private boolean mBound = false;
	private ListenerService mService = null;

	private SharedPreferences sharedPrefs;

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			ListenerServiceBinder binder = (ListenerServiceBinder) service;
			mService = binder.getService();
			binder.setListener(new ListenerServiceListener() {

				@Override
				public void sendCurrentEvaluation(Evaluation evaluation) {
					// Update the UI
					updateVehicleImage(evaluation.getCategory());
				}

			});
			mBound = true;

		}
	};

	public ListenerFragment() {

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this
				.getActivity());

		// TODO Map of images
		vehicleImagesMap = new HashMap<String, Integer>();
		vehicleImagesMap.put("none", R.drawable.none);
		vehicleImagesMap.put("walking", R.drawable.walking);
		vehicleImagesMap.put("car", R.drawable.car);
		vehicleImagesMap.put("train", R.drawable.train);
		vehicleImagesMap.put("idle", R.drawable.idle);
		vehicleViewsMap = new HashMap<String, ImageView>();

		// Set initial classification
		currentClassification = "none";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_listener, container,
				false);

		// Set classifications images
		for (Map.Entry<String, Integer> entry : vehicleImagesMap.entrySet()) {
			// Set the attributes of the image
			ImageView newView = new ImageView(getActivity());
			newView.setImageResource(entry.getValue());
			newView.setMaxHeight(dpToPx(50));
			newView.setMinimumHeight(dpToPx(50));
			newView.setMaxWidth(dpToPx(175));
			newView.setMinimumWidth(dpToPx(175));
			newView.setScaleType(ScaleType.CENTER_INSIDE);
			RelativeLayout.LayoutParams newViewLayout = new RelativeLayout.LayoutParams(
					dpToPx(150), dpToPx(150));
			newViewLayout.addRule(RelativeLayout.CENTER_HORIZONTAL);
			newViewLayout.setMargins(0, dpToPx(20), 0, 0);
			newView.setVisibility(View.INVISIBLE);

			// Add the image to the root view
			RelativeLayout rootLayout = (RelativeLayout) rootView
					.findViewById(R.id.listenerTopLayout);
			rootLayout.addView(newView, newViewLayout);

			// Add the view to the map
			vehicleViewsMap.put(entry.getKey(), newView);
		}

		// Set service start/stop buttons and callbacks
		startServiceButton = (Button) rootView.findViewById(R.id.listenerStart);
		stopServiceButton = (Button) rootView.findViewById(R.id.listenerStop);
		startServiceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startListenerService(v);
			}
		});
		stopServiceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopListenerService(v);
			}
		});

		// Set none view visible
		vehicleViewsMap.get("none").setVisibility(View.VISIBLE);

		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();

		// Check ListenerService status
		boolean isServiceRunning = sharedPrefs.getBoolean(
				ListenerService.KEY_SERVICE_ISRUNNING, false);

		if (isServiceRunning && !mBound) {
			// Bind to the running service
			Intent intent = new Intent(getActivity(), ListenerService.class);
			getActivity().bindService(intent, mConnection,
					Context.BIND_AUTO_CREATE);

			setUIMode(MODE_RUNNING);
		} else {
			setUIMode(MODE_IDLE);
		}
	}

	@Override
	public void onPause() {
		// Unbind from the service
		if (mBound) {
			getActivity().unbindService(mConnection);
			mBound = false;
		}

		super.onPause();
	}

	/**
	 * Launches ListenerService and binds the fragment to it
	 * 
	 * @param view
	 */
	public void startListenerService(View view) {
		// Check ListenerService status
		boolean isServiceRunning = sharedPrefs.getBoolean(
				ListenerService.KEY_SERVICE_ISRUNNING, false);

		if (!isServiceRunning) {
			// Start the service and bind to it
			Intent intent = new Intent(getActivity(), ListenerService.class);
			getActivity().startService(intent);
			getActivity().bindService(intent, mConnection,
					Context.BIND_AUTO_CREATE);

			// Change the UI mode
			setUIMode(MODE_RUNNING);
		}
	}

	/**
	 * Unbinds from ListenerService and stops it
	 * 
	 * @param view
	 */
	public void stopListenerService(View view) {
		// Check ListenerService status
		boolean isServiceRunning = sharedPrefs.getBoolean(
				ListenerService.KEY_SERVICE_ISRUNNING, false);

		if (isServiceRunning) {
			// Unbind from the service and stop it
			getActivity().unbindService(mConnection);
			Intent intent = new Intent(getActivity(), ListenerService.class);
			getActivity().stopService(intent);

			// XXX Set the service state in the shared prefs
			sharedPrefs.edit().putBoolean(
					ListenerService.KEY_SERVICE_ISRUNNING, false);

			// Change the UI mode
			setUIMode(MODE_IDLE);

			// Show the image for "none"
			updateVehicleImage("none");
		}
	}

	/**
	 * Sets the UI elements according to the specified mode
	 * 
	 * @param mode
	 */
	private void setUIMode(int mode) {
		switch (mode) {
		case MODE_IDLE:
			startServiceButton.setEnabled(true);
			stopServiceButton.setEnabled(false);
			break;
		case MODE_RUNNING:
			startServiceButton.setEnabled(false);
			stopServiceButton.setEnabled(true);
			break;
		}
	}

	private void updateVehicleImage(String vehicle) {
		if (!currentClassification.equals(vehicle)) {
			// Hide the previous image, show the new one
			ImageView viewToHide = vehicleViewsMap.get(currentClassification);
			viewToHide.setVisibility(View.INVISIBLE);
			ImageView viewToShow = vehicleViewsMap.get(vehicle);
			viewToShow.setVisibility(View.VISIBLE);

			currentClassification = vehicle;
		}
	}

	private int dpToPx(int dp) {
		DisplayMetrics displayMetrics = getActivity().getResources()
				.getDisplayMetrics();
		int px = Math.round(dp
				* (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
		return px;
	}

}
