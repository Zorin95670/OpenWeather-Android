package com.allinthesoft.openweather.module.activity.home;

import java.util.List;
import java.util.concurrent.ExecutionException;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.allinthesoft.openweather.R;
import com.allinthesoft.openweather.common.DataActivity;
import com.allinthesoft.openweather.core.exception.AndroidException;
import com.allinthesoft.openweather.core.weather.CityData;
import com.allinthesoft.openweather.core.weather.Location;
import com.allinthesoft.openweather.core.weather.adapter.DataAdapteur;
import com.allinthesoft.openweather.core.weather.adapter.PlacesAutoCompleteAdapter;
import com.allinthesoft.openweather.module.activity.information.SimpleWeatherInformation;
import com.allinthesoft.openweather.module.listener.ChangeActivityOnItemListener;
import com.allinthesoft.openweather.module.listener.DeleteItemOnListView;
import com.allinthesoft.openweather.module.listener.OnSwipeListener;
import com.allinthesoft.openweather.module.listener.SearchCityOnKeyboardActionListener;
import com.allinthesoft.openweather.service.openweather.gps.GPSTracker;
import com.allinthesoft.openweather.service.openweather.task.JSONCityTask;
import com.allinthesoft.openweather.service.openweather.task.JSONWeatherTask;

public class HomeActivity extends DataActivity implements OnClickListener {

	private DataAdapteur adapter;
	private Dialog settings;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		ListView list = (ListView) findViewById(R.id.ah_list_city);
		adapter = new DataAdapteur(this, R.id.ah_list_city, getBaseApplication().getData());
		list.setAdapter(adapter);
		
		list.setOnItemClickListener(new ChangeActivityOnItemListener(this, SimpleWeatherInformation.class));
		list.setOnItemLongClickListener(new DeleteItemOnListView(adapter, this));
		
		AutoCompleteTextView text = ((AutoCompleteTextView) findViewById(R.id.ah_city_search));
		text.setOnKeyListener(new SearchCityOnKeyboardActionListener(text, adapter, getBaseApplication().getData()));
		initAutoComplete();
		findViewById(R.id.ah_localisation).setOnClickListener(this);
		RelativeLayout view = (RelativeLayout) findViewById(R.id.ah_content);
		view.setOnTouchListener(new OnSwipeListener() {
			
			@Override
			public void swipe() {
				if (getAction() == Action.SWIPE_RIGHT_TO_LEFT && getBaseApplication().getData().getCities().size() != 0) {
					
					Intent myIntent = new Intent(HomeActivity.this,
							SimpleWeatherInformation.class);
					myIntent.putExtra("Data", 0);
					HomeActivity.this.startActivity(myIntent);
					overridePendingTransition(R.anim.main_out, R.anim.other_in);
				}
			}
		});
		
		initCities();
		findViewById(R.id.ah_refresh).setOnClickListener(this);
		findViewById(R.id.ah_settings).setOnClickListener(this);
	}

	public void refreshData(){
		Dialog dialog = new Dialog(HomeActivity.this);
		dialog.setContentView(R.layout.dialog_loading);
		dialog.setTitle(R.string.default_wait);
		dialog.show();
		initCities();
		dialog.dismiss();
	}
	
	private void initCities() {
		List<CityData> cities = getBaseApplication().getData().getCities();
		if(cities != null){
			int len = cities.size();
			for(int i = 0 ; i < len ; i++){
				JSONWeatherTask task = new JSONWeatherTask();
				task.execute(new String[]{cities.get(i).getId()});
				try {
					CityData data = task.get();
					cities.set(i, data);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(len > 0){
				adapter.notifyDataSetChanged();
			}
		} else {
			// TODO problem
		}
	}

	private void initAutoComplete() {

		AutoCompleteTextView autoCompView = (AutoCompleteTextView) findViewById(R.id.ah_city_search);
	    autoCompView.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.list_item));
	}
	
	@Override
	protected void onStop() {
		getBaseApplication().maj();
		super.onStop();
	}

	public void changeTemperature(boolean b) {
		getBaseApplication().getData().setFahrenheit(b);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.ah_settings){
			settings = new Dialog(HomeActivity.this);
			settings.setContentView(R.layout.dialog_settings);
			settings.setTitle(R.string.settings);
			if(!getBaseApplication().getData().isFahrenheit()){
				((RadioButton)settings.findViewById(R.id.do_celcius)).setChecked(true);
				((RadioButton)settings.findViewById(R.id.do_fahrenheit)).setChecked(false);
			}
			((RadioGroup)settings.findViewById(R.id.do_group)).setOnCheckedChangeListener(new OnCheckedChangeListener() {
				
				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					if(checkedId == R.id.do_celcius){
						changeTemperature(false);
					} else {
						changeTemperature(true);					
					}
				}
			});
			((Button)settings.findViewById(R.id.do_close)).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					settings.dismiss();
				}
			});
			settings.show();
		} else if(v.getId() == R.id.ah_refresh){
			Log.i("TEST", "refresh");
			refreshData();
		} else if(v.getId() == R.id.ah_localisation){
			GPSTracker gpsTracker = new GPSTracker(HomeActivity.this);

			if (gpsTracker.canGetLocation()) {
				Location location = new Location();
				location.setLatitude(gpsTracker.latitude);
				location.setLongitude(gpsTracker.longitude);
				JSONCityTask task = new JSONCityTask(); 
				task.execute(new String[] { location.toString() });
				String city = null;
				try {
					city = task.get();
					JSONWeatherTask weatherTask = new JSONWeatherTask();
					weatherTask.execute(new String[]{city});
					try {
						CityData data = weatherTask.get();
						getBaseApplication().getData().addCurent(data);
						if(getBaseApplication().getData().isDataChanged()){
							adapter.notifyDataSetChanged();
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (AndroidException e) {
						Toast.makeText(getBaseContext(), getString(e.getMessageId()), Toast.LENGTH_LONG).show();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
