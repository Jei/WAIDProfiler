/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
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
	private Evaluation lastEvaluation = null;

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
			lastEvaluation = new Evaluation(id, timestamp, category);

			// Send the new evaluation to the listening activities.
			if (mListener != null) {
				mListener.sendCurrentEvaluation(lastEvaluation);
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

		// Get shared preferences
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		// Persist the service state in the shared preferences
		sharedPrefs.edit().putBoolean(KEY_SERVICE_ISRUNNING, true).commit();

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

}
