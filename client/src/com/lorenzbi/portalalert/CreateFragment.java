package com.lorenzbi.portalalert;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.DialogFragment;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class CreateFragment extends DialogFragment {
	public AutoCompleteTextView autoComplete;
	public String data;
	public List<String> suggest;
	ArrayAdapter<String> aAdapter;
	public CreateFragment() {
	}
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create, container, false);
		autoComplete = (AutoCompleteTextView) view.findViewById(R.id.autoCompleteTextView);
        getDialog().setTitle("Neuer Alert");
        return view;
    }
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		suggest = new ArrayList<String>();
		 
		autoComplete.addTextChangedListener(new TextWatcher(){
 
			public void afterTextChanged(Editable editable) {
				// TODO Auto-generated method stub
 
			}
 
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				// TODO Auto-generated method stub
 
			}
 
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String title = s.toString();
				
				RequestParams params = new RequestParams();
				Location lastLocation = ((MainActivity)getActivity()).getLastLocation();
				params.put("lng", lastLocation.getLongitude()+"");
				params.put("lat", lastLocation.getLatitude()+"");
				params.put("title", title);
				if (title.length() > 1){
				HttpManager.post("search", params, new AsyncHttpResponseHandler() {
					@Override
					public void onSuccess(String json) {
						
						try {
							suggest = new ArrayList<String>();
							JSONArray array = new JSONArray(json);
							for (int i=0; i<array.length(); i++)
								suggest.add(array.getString(i));
							aAdapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),R.layout.dropdown_row,suggest);
							autoComplete.setAdapter(aAdapter);
							aAdapter.notifyDataSetChanged();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						

						Log.d("onTextChanged onSuccess", json);
					}
				});
			}
			}
        });
	}
}
