package com.lorenzbi.portalalert;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.commonsware.cwac.loaderex.SQLiteCursorLoader;
import com.squareup.otto.Subscribe;

public class ListFragment extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
	static Double lat = null;
	static Double lng = null;
	Double fudge = null;
	private DatabaseHelper db = null;
	private ListAdapter adapter = null;
	public SQLiteCursorLoader loader = null;
	private int index = -1;
	private int top = 0;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_list, container, false);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setHasOptionsMenu(true);
		db = new DatabaseHelper(getActivity().getBaseContext());
		if (lat != null && lng != null) { // if no location wait for
											// locationevent
			getLoaderManager().initLoader(0, null, this);
		}
	}

	@Subscribe
	public void onLocationEvent(Location location) {
		lng = location.getLongitude();
		lat = location.getLatitude();
		getLoaderManager().initLoader(0, null, this);
	}

	@Subscribe
	public void onUpdateEvent(String msg) {
		Log.d("onupdateevent", "onupdateevent");
		if (adapter != null) {
			Cursor cursor = db.getNear(lng, lat);
			adapter.swapCursor(cursor);
		}
	}

	double getDouble(final SharedPreferences prefs, final String key,
			final double defaultValue) {
		return Double.longBitsToDouble(prefs.getLong(key,
				Double.doubleToLongBits(defaultValue)));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loaderId, Bundle arg1) {
		fudge = Math.pow(Math.cos(Math.toRadians(lat)), 2);
		Long now = System.currentTimeMillis();
		Log.d("now", now.toString());
		loader = new SQLiteCursorLoader(getActivity().getBaseContext(), db,
				"SELECT _id, id, imagesrc, title, message, lng, lat, expire, done, ( "
						+ lat + " - lat) * ( " + lat + "- lat) + ( " + lng
						+ "- lng) * ( " + lng + "- lng) * " + fudge
						+ " as distance " + " from alerts  WHERE expire >= "
						+ now + " AND NOT done order by distance asc", null);

		return (loader);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		this.loader = (SQLiteCursorLoader) loader;
		if (adapter == null) {
			adapter = new ListAdapter(getActivity().getApplicationContext(),
					cursor, 0);
		}
		ListView lv = (ListView) getActivity().findViewById(R.id.contentlist);
		lv.setClickable(true);
		lv.setOnItemClickListener(this);
		lv.setAdapter(adapter);
		registerForContextMenu(lv);
		Log.d("onloadfinished register", "onloadfinished register");
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
	    super.onCreateOptionsMenu(menu, inflater);
	    inflater.inflate(R.menu.menu_list, menu);
	}
	
	public void onResume() {
		super.onResume();
		if (((MainActivity)getActivity()).getUpdateNeeded()){
			((MainActivity)getActivity()).setUpdateNeeded(false);
			onUpdateEvent("update");
		}

		BusProvider.getInstance().register(this);
		// Log.d("register onresume", "register onresume");
		if (index != -1) {
			ListView lv = (ListView) getActivity().findViewById(
					R.id.contentlist);
			lv.setSelectionFromTop(index, top);
		}
	}

	public void onPause() {
		super.onPause();
		BusProvider.getInstance().unregister(this);
		try {
			ListView lv = (ListView) getActivity().findViewById(
					R.id.contentlist);

			index = lv.getFirstVisiblePosition();
			View v = lv.getChildAt(0);
			top = (v == null) ? 0 : v.getTop();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (loader != null)
			loader.reset();
		db.close();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View v, int position,
			long listId) {
		/*
		 * LinearLayout listItem = (LinearLayout) v; TextView clickedItemView =
		 * (TextView) listItem.findViewById(R.id.name); String clickedItemString
		 * = clickedItemView.getText().toString();
		 */
		DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
		String id = dbHelper.getId(listId);

		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = new DetailFragment();
		Bundle bundle = new Bundle();
		bundle.putString("id", id);
		fragment.setArguments(bundle);
		fragmentManager.beginTransaction().setCustomAnimations(R.animator.fragment_slide_left_enter,
                R.animator.fragment_slide_left_exit,
                R.animator.fragment_slide_right_enter,
                R.animator.fragment_slide_right_exit).addToBackStack(null)
				.replace(R.id.content_frame, fragment).commit();
	}
}