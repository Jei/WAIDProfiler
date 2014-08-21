package it.unibo.cs.jonus.waidprof;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;

public class ProfilerActivity extends Activity implements ActionBar.TabListener {

	private static final String AUTHORITY = "it.unibo.cs.jonus.waidrec.evaluationsprovider";
	private static final String EVALUATIONS_PATH = "evaluations";
	private static final String VEHICLES_PATH = "vehicles";
	public static final Uri EVALUATIONS_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + EVALUATIONS_PATH);
	public static final Uri VEHICLES_URI = Uri.parse("content://" + AUTHORITY
			+ "/" + VEHICLES_PATH);
	public static final String PATH_LAST_EVALUATION = "/last";
	public static final String PATH_ALL_VEHICLES = "/all";
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_CATEGORY = "category";
	public static final String COLUMN_TIMESTAMP = "timestamp";
	public static final String COLUMN_ICON = "icon";
	public static final String COLUMN_AVGA = "avga";
	public static final String COLUMN_MINA = "mina";
	public static final String COLUMN_MAXA = "maxa";
	public static final String COLUMN_STDA = "stda";
	public static final String COLUMN_AVGG = "avgg";
	public static final String COLUMN_MING = "ming";
	public static final String COLUMN_MAXG = "maxg";
	public static final String COLUMN_STDG = "stdg";
	public static final String[] EvaluationColumnsProjection = {
			COLUMN_TIMESTAMP, COLUMN_CATEGORY, COLUMN_AVGA, COLUMN_MINA,
			COLUMN_MAXA, COLUMN_STDA, COLUMN_AVGG, COLUMN_MING, COLUMN_MAXG,
			COLUMN_STDG };
	public static final String[] VehiclesColumnsProjection = { COLUMN_CATEGORY,
			COLUMN_ICON };

	public static HashMap<String, Bitmap> sVehiclesMap = new HashMap<String, Bitmap>();

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_profiler);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}

		// Get the list of vehicles from the Content Provider
		Uri uri = Uri.parse(VEHICLES_URI + PATH_ALL_VEHICLES);
		Cursor cursor = getContentResolver().query(uri,
				ProfilerActivity.VehiclesColumnsProjection, null, null, null);
		if (cursor == null) {
			return;
		}
		ArrayList<VehicleItem> vehiclesList = cursorToVehicleItemArray(cursor);
		cursor.close();

		// Create hash map for the vehicles
		for (VehicleItem item : vehiclesList) {
			sVehiclesMap.put(item.getCategory(), item.getIcon());
		}
		Bitmap noneBmp = BitmapFactory.decodeResource(getResources(),
				R.drawable.none_00b0b0);
		sVehiclesMap.put("none", noneBmp);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.profiler, menu);
		return true;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			Fragment fragment = null;
			switch (position) {
			case 0:
				fragment = new ListenerFragment();
				break;
			case 1:
				fragment = new ProfilesFragment();
				break;
			default:
				fragment = new ListenerFragment();
			}

			return fragment;
		}

		@Override
		public int getCount() {
			// Show 2 total pages.
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
			case 0:
				return getString(R.string.fragment_listener_title).toUpperCase(
						l);
			case 1:
				return getString(R.string.fragment_profiles_title).toUpperCase(
						l);
			}
			return null;
		}
	}

	public static VehicleItem cursorToVehicleItem(Cursor cursor) {
		VehicleItem item = new VehicleItem();

		String category = cursor.getString(cursor
				.getColumnIndexOrThrow(COLUMN_CATEGORY));
		byte[] iconByteArray = cursor.getBlob(cursor
				.getColumnIndexOrThrow(COLUMN_ICON));
		Bitmap icon = BitmapFactory.decodeByteArray(iconByteArray, 0,
				iconByteArray.length);

		item.setCategory(category);
		item.setIcon(icon);

		return item;
	}

	public static ArrayList<VehicleItem> cursorToVehicleItemArray(Cursor cursor) {
		ArrayList<VehicleItem> itemArray = new ArrayList<VehicleItem>();

		VehicleItem item;
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			item = new VehicleItem();

			String category = cursor.getString(cursor
					.getColumnIndexOrThrow(COLUMN_CATEGORY));
			byte[] iconByteArray = cursor.getBlob(cursor
					.getColumnIndexOrThrow(COLUMN_ICON));
			Bitmap icon = BitmapFactory.decodeByteArray(iconByteArray, 0,
					iconByteArray.length);

			item.setCategory(category);
			item.setIcon(icon);

			itemArray.add(item);

			cursor.moveToNext();
		}

		return itemArray;
	}

	public static VehicleInstance cursorToVehicleInstance(Cursor cursor) {
		VehicleInstance instance = new VehicleInstance();
		MagnitudeFeatures accelFeatures = new MagnitudeFeatures();
		MagnitudeFeatures gyroFeatures = new MagnitudeFeatures();

		String category = cursor.getString(cursor
				.getColumnIndexOrThrow(COLUMN_CATEGORY));
		long timestamp = cursor.getLong(cursor
				.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
		accelFeatures.setAverage(cursor.getDouble(cursor
				.getColumnIndexOrThrow(COLUMN_AVGA)));
		accelFeatures.setMinimum(cursor.getDouble(cursor
				.getColumnIndexOrThrow(COLUMN_MINA)));
		accelFeatures.setMaximum(cursor.getDouble(cursor
				.getColumnIndexOrThrow(COLUMN_MAXA)));
		accelFeatures.setStandardDeviation(cursor.getDouble(cursor
				.getColumnIndexOrThrow(COLUMN_STDA)));
		gyroFeatures.setAverage(cursor.getDouble(cursor
				.getColumnIndexOrThrow(COLUMN_AVGG)));
		gyroFeatures.setMinimum(cursor.getDouble(cursor
				.getColumnIndexOrThrow(COLUMN_MING)));
		gyroFeatures.setMaximum(cursor.getDouble(cursor
				.getColumnIndexOrThrow(COLUMN_MAXG)));
		gyroFeatures.setStandardDeviation(cursor.getDouble(cursor
				.getColumnIndexOrThrow(COLUMN_STDG)));

		instance.setCategory(category);
		instance.setTimestamp(timestamp);
		instance.setAccelFeatures(accelFeatures);
		instance.setGyroFeatures(gyroFeatures);

		return instance;
	}

	public static ArrayList<VehicleInstance> cursorToVehicleInstanceArray(
			Cursor cursor) {
		ArrayList<VehicleInstance> instanceArray = new ArrayList<VehicleInstance>();
		String[] columns = cursor.getColumnNames();

		VehicleInstance instance;
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			instance = new VehicleInstance();
			MagnitudeFeatures accelFeatures = new MagnitudeFeatures();
			MagnitudeFeatures gyroFeatures = new MagnitudeFeatures();

			for (String column : columns) {
				if (column.equals(COLUMN_CATEGORY)) {
					instance.setCategory(cursor.getString(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_TIMESTAMP)) {
					instance.setTimestamp(cursor.getLong(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_AVGA)) {
					accelFeatures.setAverage(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_MINA)) {
					accelFeatures.setMinimum(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_MAXA)) {
					accelFeatures.setMaximum(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_STDA)) {
					accelFeatures.setStandardDeviation(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_AVGG)) {
					gyroFeatures.setAverage(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_MING)) {
					gyroFeatures.setMinimum(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_MAXG)) {
					gyroFeatures.setMaximum(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
				if (column.equals(COLUMN_STDG)) {
					gyroFeatures.setStandardDeviation(cursor.getDouble(cursor
							.getColumnIndex(column)));
				}
			}

			instance.setAccelFeatures(accelFeatures);
			instance.setGyroFeatures(gyroFeatures);
			instanceArray.add(instance);

			cursor.moveToNext();
		}

		return instanceArray;
	}

}
