package life.slide.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;


public class RequestsActivity extends ActionBarActivity implements ActionBar.TabListener {
    private static final String TAG = "Slide -> RequestsActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String SENDER_ID = "86021269747";
    private static final String REQUEST_INTENT = "request-received";


    private static DataStore dataStore;

    public GoogleCloudMessaging gcm;
    public String regId = "";
    public SharedPreferences prefs;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
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
        setContentView(R.layout.activity_requests);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
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
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        dataStore = DataStore.getSingletonInstance(getApplicationContext());

        prefs = getGCMPreferences();
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regId = getRegistrationId();

            Log.i(TAG, "regId: " + regId);

            if (regId.isEmpty()) registerInBackground();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_requests, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
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
            switch (position) {
                case 0:
                    return RequestsFragment.newInstance(position + 1);
                case 1:
                    return QRFragment.newInstance(position + 1);
                case 2:
                    return EditFragment.newInstance(position + 1);
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    private String getRegistrationId() {
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }

        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion();
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }

        return registrationId;
    }

    private SharedPreferences getGCMPreferences() {
        return this.getSharedPreferences(getClass().getSimpleName(), Context.MODE_PRIVATE);
    }

    private int getAppVersion() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e.toString());
        }
    }

    private void registerInBackground() {
        Log.i(TAG, "Getting GCM instance...");
        gcm = (gcm != null) ? gcm : GoogleCloudMessaging.getInstance(this);

        Runnable task = new Runnable() {
            @Override
            public void run() {
                int attemptsAllowed = 5;
                int attempts = 0;
                boolean stopFetching = false;

                while (!stopFetching) {
                    attempts++;

                    try {
                        regId = gcm.register(SENDER_ID);
                        Log.i(TAG, "Device registered, regId=" + regId);
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    } catch (IOException e) {
                        Log.i(TAG, "IOException: " + e.getMessage());
                    }

                    if (!regId.isEmpty()) {
                        if (attempts > attemptsAllowed) stopFetching = true;
                        else {
                            boolean f = sendRegistrationIdToBackend();
                            if (f) storeRegistrationId();
                        }
                    }
                }
            }
        };
        new Thread(task).start();
    }

    private boolean sendRegistrationIdToBackend() {
        return true; //TODO
    }

    private void storeRegistrationId() {
        int appVersion = getAppVersion();
        Log.i(TAG, "Saving regId on v " + appVersion);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(PROPERTY_REG_ID, regId);
        edit.putInt(PROPERTY_APP_VERSION, appVersion);
        edit.commit();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class RequestsFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String TAG = "Slide -> RequestsFragment";

        public ListView requestsList;
        public Context context;
        public ArrayAdapter<Request> requestAdapter;
        public ArrayList<Request> requests;
        public BroadcastReceiver recv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Request request = new Request(intent.getStringExtra("request"));
                requests.add(request);
                requestAdapter.notifyDataSetChanged();
            }
        };

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static RequestsFragment newInstance(int sectionNumber) {
            RequestsFragment fragment = new RequestsFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public RequestsFragment() {
        }

        @Override
        public void onPause() {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(recv);
            super.onPause();
        }

        @Override
        public void onResume() {
            LocalBroadcastManager.getInstance(context).registerReceiver(recv,
                    new IntentFilter("request-received"));
            super.onResume();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_requests, container, false);

            context = rootView.getContext();

            requests = dataStore.getRequests();
            requestsList = (ListView) rootView.findViewById(R.id.requestsList);
            requestAdapter = new ArrayAdapter<Request>(context,
                    android.R.layout.simple_list_item_1, requests);
            requestsList.setAdapter(requestAdapter);
            requestsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.i(TAG, "Clicked...");
                    Request req = requestAdapter.getItem(position);

                    Intent requestActivityIntent = new Intent(view.getContext(), RequestActivity.class);
                    requestActivityIntent.putExtra("request", req.toJson());
                    Log.i(TAG, "Starting request activity...");
                    startActivity(requestActivityIntent);
                }
            });

            return rootView;
        }
    }

    public static class QRFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public Context context;
        public Button scanButton;

        public static QRFragment newInstance(int sectionNumber) {
            QRFragment fragment = new QRFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public QRFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_qr, container, false);

            context = rootView.getContext();

            scanButton = (Button) rootView.findViewById(R.id.scanButton);
            scanButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                    startActivityForResult(intent, 0);
                }
            });

            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            if (requestCode == 0) {
                if (resultCode == RESULT_OK) {
                    String contents = intent.getStringExtra("SCAN_RESULT");
                    //String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                    dataStore.insertRawRequest(contents);
                    Intent received = new Intent(REQUEST_INTENT);
                    received.putExtra("request", contents);
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(received);
                }
            }
        }
    }

    public static class EditFragment extends Fragment {
        private static final String ARG_SECTION_NUMBER = "section_number";

        public static EditFragment newInstance(int sectionNumber) {
            EditFragment fragment = new EditFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public EditFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_edit, container, false);
            return rootView;
        }
    }
}
