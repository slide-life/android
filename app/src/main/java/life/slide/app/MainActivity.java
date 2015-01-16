package life.slide.app;

import java.io.IOException;
import java.util.Locale;

import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.WebView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends ActionBarActivity implements ActionBar.TabListener {
    private static final String TAG = "Slide -> MainActivity";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String SENDER_ID = "86021269747";

    public GoogleCloudMessaging gcm;
    public String regId = "";
    public SharedPreferences prefs;

    //TODO: make sure that MainActivity responds to RequestActivity intent for removal

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

        //TODO: download all the blocks first

        initializeKeypair(() -> {
            DataStore dataStore = DataStore.getSingletonInstance(this);
            Log.i(TAG, "Got public key: " + dataStore.getPublicKey());

            prefs = getGCMPreferences();
            if (checkPlayServices()) {
                gcm = GoogleCloudMessaging.getInstance(this);
                regId = getRegistrationId();
                Log.i(TAG, "regId: " + regId);
                if (regId.isEmpty()) registerInBackground();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_requests, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        //this is where the fragments for the different tabs are set.
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return RequestsFragment.newInstance(position + 1);
                case 1: return QRFragment.newInstance(position + 1);
                case 2: return EditFragment.newInstance(position + 1);
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0: return getString(R.string.title_section1).toUpperCase(l);
                case 1: return getString(R.string.title_section2).toUpperCase(l);
                case 2: return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    private void initializeKeypair(Runnable runnable) {
        Log.i(TAG, "Initializing keypair");

        DataStore data = DataStore.getSingletonInstance(this);
        String privateKey = data.getPrivateKey();
        if (privateKey.equals("")) {
            WebView webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            this.addContentView(webView, new ViewGroup.LayoutParams(1, 1)); //to make sure javascript is working

            try {
                String baseUrl = getResources().getString(R.string.hostname);
                webView.loadDataWithBaseURL(baseUrl,
                        "<html><head></head><body></body></html>", "text/html", "utf-8", "");

                DataStore dataStore = DataStore.getSingletonInstance(this);
                String jquery = dataStore.readResource(R.raw.jquery);
                String slideCrypto = dataStore.readResource(R.raw.slide);

                Javascript.javascriptEval(webView, jquery, (x) -> {
                    Javascript.javascriptEval(webView, slideCrypto, (y) -> {
                        Javascript.generateKeys(webView, (keys) -> {
                            try {
                                JSONObject keypair = new JSONObject(keys);
                                String privKey = keypair.getJSONObject("privateKey").toString();
                                Javascript.getPublicKey(webView, privKey, (pubKey) -> {
                                    data.setPrivateKey(privKey);
                                    data.setPublicKey(pubKey);

                                    runnable.run();
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    });
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            runnable.run();
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

        Runnable task = () -> {
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
                        ListeningExecutorService service = API.newExecutorService();
                        ListenableFuture<Boolean> f = sendRegistrationIdToBackend(service);
                        Futures.addCallback(f, new FutureCallback<Boolean>() {
                            @Override
                            public void onSuccess(Boolean result) {
                                if (result) storeRegistrationId();
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                Log.i(TAG, "Failure: " + t.getMessage());
                            }
                        });
                    }
                }
            }
        };
        new Thread(task).start();
    }

    private ListenableFuture<Boolean> sendRegistrationIdToBackend(
            ListeningExecutorService service) {
        return API.processRegistrationId(service, this, regId);
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
}
