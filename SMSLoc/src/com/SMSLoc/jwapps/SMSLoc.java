package com.SMSLoc.jwapps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

public class SMSLoc extends Activity implements OnItemClickListener,
		OnItemSelectedListener {

	private class readContacts extends AsyncTask<Void, Void, Integer> {
		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Integer doInBackground(Void... params) {

			readContactData();

			return 1;
		}

		@Override
		protected void onPostExecute(Integer result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);

			runOnUiThread(new Runnable() {

				public void run() {

					contactsProgBar.cancel();

				}
			});

		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();

			runOnUiThread(new Runnable() {

				public void run() {
					

					contactsProgBar = new ProgressDialog(SMSLoc.this);
					contactsProgBar.setCancelable(false);
					contactsProgBar
							.setMessage("Aquiring Location, Please Wait...");
					contactsProgBar
							.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					contactsProgBar.show();

				}
			});

		}

		// public void sendSMS(String toNumberValue)
	}
	
	private InterstitialAd interstitial;

	public static final String ACTION_SMS_SENT = "android.jwapps.com.SMS_SENT_ACTION";
	SmsManager sms = SmsManager.getDefault();

	// EditText txtPhoneNo;
	EditText txtMessage;
	Button Send;
	TextView textView2;

	// Initialize variables

	AutoCompleteTextView textView = null;
	private ArrayAdapter<String> adapter;

	// Store contacts values in these arraylist
	public static ArrayList<String> phoneValueArr = new ArrayList<String>();
	public static ArrayList<String> nameValueArr = new ArrayList<String>();

	ProgressBar contactsProgress;
	// TextView loadingText;
	ProgressDialog contactsProgBar;
	ProgressDialog smsProgBar;

	EditText toNumber = null;
	String toNumberValue = "";

	String strSMSLocAddress;
	String strSMSDateTime;
	static int smsCnt = 0;
	TextView mdnView;

	private LocationManager mLocationManager;
	private Handler mHandler;
	private boolean mGeocoderAvailable;
	private boolean mUseFine;
	private boolean mUseBoth;

	Button provider_both;
	Button provider_fine;
	Spinner spnComments;

	String addressStr;
	String city;
	String state;
	String zip;
	String latitude;
	String longitude;

	// Progress bar for AsyncTask
	ProgressDialog progress;
	public static final int DIALOG_DOWNLOAD_PROGRESS = 0;

	ProgressDialog mProgressBar;
	CountDownTimer mCountDownTimer;
	int i = 0;
	AdView adView;

	// UI handler codes.
	private static final int UPDATE_ADDRESS = 1;
	private static final int UPDATE_CITY = 2;
	private static final int UPDATE_STATE = 3;
	private static final int UPDATE_ZIP = 4;;

	private static final int TEN_SECONDS = 10000;
	private static final int TEN_METERS = 10;
	private static final int TWO_MINUTES = 1000 * 60 * 2;

	Location gpsLocation = null;
	Location networkLocation = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_smsloc);

		txtMessage = (EditText) findViewById(R.id.txtMessage);
		provider_both = (Button) findViewById(R.id.provider_both);
		provider_fine = (Button) findViewById(R.id.provider_fine);
		Send = (Button) findViewById(R.id.Send);
		spnComments = (Spinner) findViewById(R.id.spnComments);
		// textView2 = (TextView) findViewById(R.id.textView2);

		// Initialize AutoCompleteTextView values
		textView = (AutoCompleteTextView) findViewById(R.id.toNumber);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		provider_both.setVisibility(View.INVISIBLE);
		provider_fine.setVisibility(View.INVISIBLE);

		// Create adapter
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line,
				new ArrayList<String>());
		textView.setThreshold(1);

		// Set adapter to AutoCompleteTextView
		textView.setAdapter(adapter);
		textView.setOnItemSelectedListener(this);
		textView.setOnItemClickListener(this);

		// Read contact data and add data to ArrayAdapter
		// ArrayAdapter used by AutoCompleteTextView

		// readContactData();
		new readContacts().execute();

		/********** Button Click pass textView object ***********/
		Send.setOnClickListener(BtnAction(textView));

		// Get a reference to the LocationManager object.
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Check if the GPS setting is currently enabled on the device.
		// This verification should be done during onStart() because the system
		// calls this method
		// when the user returns to the activity, which ensures the desired
		// location provider is
		// enabled each time the activity resumes from the stopped state.
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final boolean gpsEnabled = locationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);

		mGeocoderAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD
				&& Geocoder.isPresent();

		if (!gpsEnabled) {

			gpsAlert();

		} else {

			// Handler for updating text fields on the UI like the lat/long and
			// address.
			mHandler = new Handler() {
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case UPDATE_ADDRESS:
						addressStr = ((String) msg.obj);
						break;
					case UPDATE_CITY:
						city = ((String) msg.obj);
						break;
					case UPDATE_STATE:
						state = ((String) msg.obj);
						break;
					case UPDATE_ZIP:
						zip = ((String) msg.obj);
						break;
					}
				}
			};

			// Register broadcast receivers for SMS sent and delivered intents
			registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					String message = null;
					boolean error = true;
					switch (getResultCode()) {
					case Activity.RESULT_OK:
						message = "Message sent successfully! Would you like to send another?";
						error = false;
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						message = "Error: Generic Failure. Would you like to try and send another message?";
						break;
					case SmsManager.RESULT_ERROR_NO_SERVICE:
						message = "Error: No service. Would you like to try and send another message?";
						break;
					case SmsManager.RESULT_ERROR_NULL_PDU:
						message = "Error: Null PDU. Would you like to try and send another message?";
						break;
					case SmsManager.RESULT_ERROR_RADIO_OFF:
						message = "Error: Radio off. Would you like to try and send another message?";
						break;
					}

					smsProgBar.cancel();

					// Creating alert Dialog with two Buttons

					AlertDialog.Builder alertDialog = new AlertDialog.Builder(
							SMSLoc.this);

					// Setting Dialog Title
					alertDialog.setTitle("Come Get Me!");

					// Setting Dialog Message
					alertDialog.setMessage(message);

					// Setting Icon to Dialog
					alertDialog.setIcon(R.drawable.messaging72);

					// Setting Positive "YES" Button
					alertDialog.setPositiveButton("YES",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// Write your code here to execute after
									// dialog

									textView.setText("");

								}
							});

					// Setting Positive "NO" Button
					alertDialog.setNegativeButton("NO",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									// Write your code here to execute after
									// dialog

									finish();

								}
							});

					alertDialog.setCancelable(false);
					// Showing Alert Message
					alertDialog.show();

				}
			}, new IntentFilter(ACTION_SMS_SENT));

			useFineProvider(provider_fine);
		}

		// spnComments.setVisibility(View.INVISIBLE);
		// Set up the Spinner entries
		
		List<String> lsComments = new ArrayList<String>();
		lsComments.add("Please Select one if desired");
		lsComments.add("I'm too intoxicated to drive. Please come get me at:");
		lsComments.add("I have broken down. Please come get me at:");
		lsComments.add("I am ready to leave, can you please come get me at:");
		lsComments.add("I am already here. This is the address:");

		ArrayAdapter<String> aspnComments = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, lsComments);
		aspnComments
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnComments.setAdapter(aspnComments);
		// Set up a callback for the spinner
		spnComments.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onNothingSelected(AdapterView<?> arg0) {
			}

			public void onItemSelected(AdapterView<?> parent, View v,
					int position, long id) {
				// Code that does something when the Spinner value changes

			}
		});
		
		// Prepare the Interstitial Ad
        interstitial = new InterstitialAd(SMSLoc.this);
        // Insert the Ad Unit ID
        interstitial.setAdUnitId("ca-app-pub-123456789/123456789");
 
        //Locate the Banner Ad in activity_main.xml
        AdView adView = (AdView) this.findViewById(R.id.adView);
 
        // Request for Ads
        AdRequest adRequest = new AdRequest.Builder()
 
        // Add a test device to show Test Ads
         //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
         //.addTestDevice("D4D12F9C42FED457EE80DA038A0431FB")
                .build();
 
        // Load ads into Banner Ads
        adView.loadAd(adRequest);
 
        // Load ads into Interstitial Ads
        interstitial.loadAd(adRequest);
 
        // Prepare an Interstitial Ad Listener
        interstitial.setAdListener(new AdListener() {
            public void onAdLoaded() {
                // Call displayInterstitial() function
                displayInterstitial();
            }
        });

	} // TODO end of onCreate
	
	public void displayInterstitial() {
        // If Ads are loaded, show Interstitial else show nothing.
        if (interstitial.isLoaded()) {
            interstitial.show();
        }
	}

	private OnClickListener BtnAction(final AutoCompleteTextView toNumber) {
		return new OnClickListener() {

			public void onClick(View v) {

				final String ToNumber = toNumberValue;

				if (ToNumber.length() == 0) {

					numberAlert();

				} else if (txtMessage.length() == 0) {

					messageAlert();

				} else {

					sendSMS(toNumberValue);

					runOnUiThread(new Runnable() {

						public void run() {

							smsProgBar = new ProgressDialog(SMSLoc.this);
							smsProgBar.setCancelable(false);
							smsProgBar
									.setMessage("Sending messaging, please wait...");
							smsProgBar
									.setProgressStyle(ProgressDialog.STYLE_SPINNER);
							smsProgBar.show();

						}
					});

				}

			}
		};
	}

	private void readContactData() {

		try {

			/*********** Reading Contacts Name And Number **********/

			String phoneNumber = "";
			ContentResolver cr = getBaseContext().getContentResolver();

			// Query to get contact name

			Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
					null, null, null);

			// If data data found in contacts
			if (cur.getCount() > 0) {

				Log.i("AutocompleteContacts", "Reading   contacts........");

				int k = 0;
				String name = "";

				while (cur.moveToNext()) {

					String id = cur.getString(cur
							.getColumnIndex(ContactsContract.Contacts._ID));
					name = cur
							.getString(cur
									.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

					// Check contact have phone number
					if (Integer
							.parseInt(cur.getString(cur
									.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

						// Create query to get phone number by contact id
						Cursor pCur = cr
								.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
										null,
										ContactsContract.CommonDataKinds.Phone.CONTACT_ID
												+ " = ?", new String[] { id },
										null);
						int j = 0;

						while (pCur.moveToNext()) {
							// Sometimes get multiple data
							if (j == 0) {
								// Get Phone number
								phoneNumber = ""
										+ pCur.getString(pCur
												.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

								// Add contacts names to adapter
								adapter.add(name);

								// Add ArrayList names to adapter
								phoneValueArr.add(phoneNumber.toString());
								nameValueArr.add(name.toString());

								j++;
								k++;
							}
						} // End while loop
						pCur.close();
					} // End if

				} // End while loop

			} // End Cursor value check
			cur.close();

		} catch (Exception e) {
			Log.i("AutocompleteContacts", "Exception : " + e);
		}

	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int position,
			long arg3) {
		// TODO Auto-generated method stub
		// Log.d("AutocompleteContacts", "onItemSelected() position " +
		// position);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		// TODO Auto-generated method stub

		// Get Array index value for selected name
		int i = nameValueArr.indexOf("" + arg0.getItemAtPosition(arg2));

		// If name exist in name ArrayList
		if (i >= 0) {

			// Get Phone Number
			toNumberValue = phoneValueArr.get(i);

			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

			// Show Alert
			// Toast.makeText(getBaseContext(),
			// "Position:"+arg2+" Name:"+arg0.getItemAtPosition(arg2)+" Number:"+toNumberValue,
			// Toast.LENGTH_LONG).show();

			Log.d("AutocompleteContacts",
					"Position:" + arg2 + " Name:"
							+ arg0.getItemAtPosition(arg2) + " Number:"
							+ toNumberValue);

		}

	}

	// Method to launch Settings
	private void enableLocationSettings() {
		Intent settingsIntent = new Intent(
				Settings.ACTION_LOCATION_SOURCE_SETTINGS);
		startActivity(settingsIntent);
	}

	// Stop receiving location updates whenever the Activity becomes invisible.
	@Override
	protected void onStop() {
		super.onStop();
		mLocationManager.removeUpdates(listener);
	}

	// Set up fine and/or coarse location providers depending on whether the
	// fine provider or both providers button is pressed.
	private void setup() {

		mLocationManager.removeUpdates(listener);

		// Get fine location updates only.
		if (mUseFine) {
			// Request updates from just the fine (gps) provider.
			gpsLocation = requestUpdatesFromProvider(
					LocationManager.GPS_PROVIDER, R.string.not_support_gps);
			// Update the UI immediately if a location is obtained.
			if (gpsLocation != null)
				updateUILocation(gpsLocation);

			if (gpsLocation == null) {

				mProgressBar = new ProgressDialog(SMSLoc.this);
				mProgressBar.setCancelable(false);
				mProgressBar.setMessage("Aquiring location, please wait...");
				mProgressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgressBar.setProgress(i);
				mProgressBar.show();
				mCountDownTimer = new CountDownTimer(10000, 1000) {

					@Override
					public void onTick(long millisUntilFinished) {
						Log.v("Log_tag", "Tick of Progress" + i
								+ millisUntilFinished);
						i++;
						mProgressBar.setProgress(i);

					}

					@Override
					public void onFinish() {

						mProgressBar.cancel();

						mLocationManager.removeUpdates(listener);

						gpsLocation = requestUpdatesFromProvider(
								LocationManager.GPS_PROVIDER,
								R.string.not_support_gps);

						if (gpsLocation == null) {

							mLocationManager.removeUpdates(listener);

							// Creating alert Dialog with one Button

							AlertDialog.Builder alertDialog = new AlertDialog.Builder(
									SMSLoc.this);

							// Setting Dialog Title
							alertDialog.setTitle("Unable to locate using GPS");

							// Setting Dialog Message
							alertDialog.setMessage(R.string.gps_error);

							// Setting Icon to Dialog
							alertDialog.setIcon(R.drawable.redwarningsign72);

							// Setting Positive "OK" Button
							alertDialog.setPositiveButton("OK",
									new DialogInterface.OnClickListener() {
										public void onClick(
												DialogInterface dialog,
												int which) {
											// Write your code here to execute
											// after
											// dialog

											useCoarseFineProviders(provider_both);
											;
										}
									});
							alertDialog.setCancelable(false);
							// Showing Alert Message
							alertDialog.show();

						}
					}
				};
				mCountDownTimer.start();

			}
		} else if (mUseBoth) {
			// Get coarse and fine location updates.
			// Request updates from both fine (gps) and coarse (network)
			// providers.
			gpsLocation = requestUpdatesFromProvider(
					LocationManager.GPS_PROVIDER, R.string.not_support_gps);
			networkLocation = requestUpdatesFromProvider(
					LocationManager.NETWORK_PROVIDER,
					R.string.not_support_network);

			// If both providers return last known locations, compare the two
			// and use the better
			// one to update the UI. If only one provider returns a location,
			// use it.
			if (gpsLocation != null && networkLocation != null) {
				updateUILocation(getBetterLocation(gpsLocation, networkLocation));
			} else if (gpsLocation != null) {
				updateUILocation(gpsLocation);
			} else if (networkLocation != null) {
				updateUILocation(networkLocation);
			}
		}
	}

	/**
	 * Method to register location updates with a desired location provider. If
	 * the requested provider is not available on the device, the app displays a
	 * Toast with a message referenced by a resource id.
	 * 
	 * @param provider
	 *            Name of the requested provider.
	 * @param errorResId
	 *            Resource id for the string message to be displayed if the
	 *            provider does not exist on the device.
	 * @return A previously returned {@link android.location.Location} from the
	 *         requested provider, if exists.
	 */
	private Location requestUpdatesFromProvider(final String provider,
			final int errorResId) {
		Location location = null;
		if (mLocationManager.isProviderEnabled(provider)) {
			mLocationManager.requestLocationUpdates(provider, TEN_SECONDS,
					TEN_METERS, listener);
			location = mLocationManager.getLastKnownLocation(provider);
		} else {
			Toast.makeText(this, errorResId, Toast.LENGTH_LONG).show();
		}
		return location;
	}

	// Callback method for the "fine provider" button.
	public void useFineProvider(View v) {
		Log.v("Button Pressed", "GPS Only");
		mUseFine = true;
		mUseBoth = false;
		setup();
	}

	// Callback method for the "both providers" button.
	public void useCoarseFineProviders(View v) {
		Log.v("button pressed", "this is the both button");
		mUseFine = false;
		mUseBoth = true;
		setup();
	}

	private void doReverseGeocoding(Location location) {
		// Since the geocoding API is synchronous and may take a while. You
		// don't want to lock
		// up the UI thread. Invoking reverse geocoding in an AsyncTask.
		(new ReverseGeocodingTask(this)).execute(new Location[] { location });
	}

	private void updateUILocation(Location location) {
		// We're sending the update to a handler which then updates the UI with
		// the new
		// location.
		// Message.obtain(mHandler,
		// UPDATE_LATLNG,
		// location.getLatitude() + ", " +
		// location.getLongitude()).sendToTarget();

		// Bypass reverse-geocoding only if the Geocoder service is available on
		// the device.
		if (mGeocoderAvailable)
			doReverseGeocoding(location);
	}

	private final LocationListener listener = new LocationListener() {

		public void onLocationChanged(Location location) {
			// A new location update is received. Do something useful with it.
			// Update the UI with
			// the location update.
			updateUILocation(location);
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix. Code taken from
	 * http://developer.android.com/guide/topics/location
	 * /obtaining-user-location.html
	 * 
	 * @param newLocation
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 * @return The better Location object based on recency and accuracy.
	 */

	protected Location getBetterLocation(Location newLocation,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return newLocation;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = newLocation.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved.
		if (isSignificantlyNewer) {
			return newLocation;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return currentBestLocation;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (newLocation.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(newLocation.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return newLocation;
		} else if (isNewer && !isLessAccurate) {
			return newLocation;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return newLocation;
		}
		return currentBestLocation;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	// AsyncTask encapsulating the reverse-geocoding API. Since the geocoder API
	// is blocked,
	// we do not want to invoke it from the UI thread.
	private class ReverseGeocodingTask extends AsyncTask<Location, Void, Void> {
		Context mContext;

		public ReverseGeocodingTask(Context context) {
			super();
			mContext = context;
		}

		@Override
		protected Void doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());

			Location loc = params[0];
			List<Address> addresses = null;
			try {
				addresses = geocoder.getFromLocation(loc.getLatitude(),
						loc.getLongitude(), 1);
			} catch (IOException e) {
				e.printStackTrace();
				// Update address field with the exception.
				Message.obtain(mHandler, UPDATE_ADDRESS, e.toString())
						.sendToTarget();
			}
			if (addresses != null && addresses.size() > 0) {
				Address address = addresses.get(0);
				// Format the first line of address (if available), city, and
				// country name.
				addressStr = address.getAddressLine(0);
				city = address.getLocality();
				state = address.getAdminArea();
				zip = address.getPostalCode();

				// Update address field on UI.
				Message.obtain(mHandler, UPDATE_ADDRESS, addressStr)
						.sendToTarget();
				Message.obtain(mHandler, UPDATE_CITY, city).sendToTarget();
				Message.obtain(mHandler, UPDATE_STATE, state).sendToTarget();
				Message.obtain(mHandler, UPDATE_ZIP, zip).sendToTarget();

				Log.v("address: ", addressStr);
				Log.v("city: ", city);
				Log.v("state: ", state);
				Log.v("zip: ", zip);

				runOnUiThread(new Runnable() {

					public void run() {

						txtMessage.setText("\n" + addressStr + "\n" + city
								+ ", " + state + " " + zip);

					}
				});

			}
			return null;
		}

	}

	// Get lat and long of entered address
	public void getLatandLong() {

		String specAddressStr = addressStr + " " + city + "," + " " + state
				+ " " + zip;

		// find the addresses by using getFromLocationName() method with the
		// given address

		List<Address> foundGeocode = null;

		try {

			foundGeocode = new Geocoder(SMSLoc.this, Locale.ENGLISH)
					.getFromLocationName(specAddressStr, 1);
			foundGeocode.get(0).getLatitude(); // getting latitude
			foundGeocode.get(0).getLongitude();// getting longitude

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (foundGeocode != null) {

			latitude = (String.valueOf(foundGeocode.get(0).getLatitude()));
			longitude = (String.valueOf(foundGeocode.get(0).getLongitude()));

		} else {

			latitude = "Unable to find Latitude. Please try again.";
			longitude = "Unable to find Longitude. Please try again.";

		}

	}

	/*
	 * public void onClickSend(View v) { useCoarseFineProviders(buttonSendNow);
	 * new sendSMSGPS().execute(); }
	 */

	public void sendSMS(String toNumberValue) {

		if (spnComments.getSelectedItem() == "Please Select one if desired") {

			sms.sendTextMessage(toNumberValue, null, txtMessage.getText()
					.toString(), PendingIntent.getBroadcast(this, 0,
					new Intent(ACTION_SMS_SENT), 0), null);

			Log.v("MDN: ", toNumberValue);

			Log.v("Message: ", txtMessage.getText().toString() + "\n"
					+ strSMSLocAddress);

		} else {

			sms.sendTextMessage(toNumberValue, null, spnComments
					.getSelectedItem().toString()
					+ txtMessage.getText().toString(), PendingIntent
					.getBroadcast(this, 0, new Intent(ACTION_SMS_SENT), 0),
					null);

			Log.v("MDN: ", toNumberValue);

			Log.v("Message: ", txtMessage.getText().toString() + "\n"
					+ strSMSLocAddress);
		}
	}

	// Dialog to prompt users to enter a message.
	private void messageAlert() {

		// Creating alert Dialog with one Button

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(SMSLoc.this);

		// Setting Dialog Title
		alertDialog.setTitle("Attention!");

		// Setting Dialog Message
		alertDialog
				.setMessage("Please make sure the location is showing before continuing.");

		// Setting Icon to Dialog
		alertDialog.setIcon(R.drawable.redwarningsign72);

		// Setting Positive "OK" Button
		alertDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Write your code here to execute after
						// dialog

					}
				});
		alertDialog.setCancelable(false);
		// Showing Alert Message
		alertDialog.show();

	}

	// Dialog to prompt users to enter a number.
	private void numberAlert() {

		// Creating alert Dialog with one Button

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(SMSLoc.this);

		// Setting Dialog Title
		alertDialog.setTitle("Attention!");

		// Setting Dialog Message
		alertDialog.setMessage("Please select a contact before continuing.");

		// Setting Icon to Dialog
		alertDialog.setIcon(R.drawable.redwarningsign72);

		// Setting Positive "OK" Button
		alertDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Write your code here to execute after
						// dialog

					}
				});
		alertDialog.setCancelable(false);
		// Showing Alert Message
		alertDialog.show();

	}

	// Dialog to prompt users to enable GPS on the device.
	private void gpsAlert() {

		// Creating alert Dialog with one Button

		AlertDialog.Builder alertDialog = new AlertDialog.Builder(SMSLoc.this);

		// Setting Dialog Title
		alertDialog.setTitle("Attention!");

		// Setting Dialog Message
		alertDialog
				.setMessage(Html
						.fromHtml("GPS <b>must</b> be enabled to use this application. "
								+ "This application will close and you will automatically be taken to enable GPS in settings. "
								+ "You will need to re-launch this application after enabling GPS. Please press 'OK' below to continue."));

		// Setting Icon to Dialog
		alertDialog.setIcon(R.drawable.redwarningsign72);

		// Setting Positive "OK" Button
		alertDialog.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// Write your code here to execute after
						// dialog
						Intent myIntent = new Intent(
								Settings.ACTION_LOCATION_SOURCE_SETTINGS);
						startActivityForResult(myIntent, 0);
						finish();
					}
				});
		alertDialog.setCancelable(false);
		// Showing Alert Message
		alertDialog.show();

	}

	protected void onPause() {
		super.onPause();
		if (adView != null) {
			adView.pause();
		}
		contactsProgBar.cancel();
		//finish();
	}

	protected void onResume() {
		super.onResume();
		if (adView != null) {
            adView.resume();
        }
	}

	protected void onDestroy() {
		super.onDestroy();
		if (adView != null) {
            adView.destroy();
        }
	}
}
