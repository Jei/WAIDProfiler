/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

/**
 * @author jei
 * 
 */
public class ListenerFragment extends Fragment {

	private static final int MODE_IDLE = 0;
	private static final int MODE_RUNNING = 1;

	// UI elements
	private ImageView noneView;
	private ImageView walkingView;
	private ImageView carView;
	private ImageView trainView;
	private ImageView idleView;
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
					// TODO Auto-generated method stub

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

		// Set initial classification
		currentClassification = "none";
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_listener, container,
				false);

		// Set classifications images
		noneView = (ImageView) rootView.findViewById(R.id.noneView);
		walkingView = (ImageView) rootView.findViewById(R.id.walkingView);
		carView = (ImageView) rootView.findViewById(R.id.carView);
		trainView = (ImageView) rootView.findViewById(R.id.trainView);
		idleView = (ImageView) rootView.findViewById(R.id.idleView);

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
		noneView.setVisibility(View.VISIBLE);

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

}
