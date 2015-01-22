package life.slide.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.google.common.util.concurrent.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Future;

public class RequestActivity extends ActionBarActivity {
    private static final String TAG = "Slide -> RequestActivity";

    private String BASE_URL;

    private String slideJs;
    private String slideFormJs;
    private String stylesCss;
    private String jqueryJs;
    private String formTemplateHtml;

    public Request request;

    public TextView requestLabel;
    public WebView webForm;
    public Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        BASE_URL = getResources().getString(R.string.hostname);

        try {
            DataStore dataStore = DataStore.getSingletonInstance(this);
            slideJs = dataStore.readResource(R.raw.slide);
            stylesCss = dataStore.readResource(R.raw.styles);
            jqueryJs = dataStore.readResource(R.raw.jquery);
            formTemplateHtml = dataStore.readResource(R.raw.form_template);
        } catch (IOException e) {
            Log.i(TAG, "IO error on reading resource.");
            e.printStackTrace();
        }

        ActionBar ab = getActionBar();
        if (ab != null) ab.setTitle("Confirm request");

        Intent intent = getIntent();
        request = new Request(intent.getStringExtra("request"));

        requestLabel = (TextView) findViewById(R.id.requestLabel);
        requestLabel.setText(request.name);

        webForm = (WebView) findViewById(R.id.depositView);
        webForm.getSettings().setJavaScriptEnabled(true);

        submitButton = (Button) findViewById(R.id.submitButton);

        initializeWeb();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_request, menu);
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

    private void initializeWeb() {
        formTemplateHtml = formTemplateHtml
                .replace("{{@jquery.js}}", jqueryJs)
                .replace("{{@styles.css}}", stylesCss)
                .replace("{{@slide.js}}", slideJs);

        webForm.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                initializeForm();
            }
        });

        webForm.loadDataWithBaseURL(BASE_URL, formTemplateHtml, "text/html", "UTF-8", "");
    }

    private void initializeForm() {
        Javascript.decryptSymKey(webForm, request.key, (formSymKey) -> {
            DataStore dataStore = DataStore.getSingletonInstance(this);

            try {
                JSONObject stringifiedProfile = Javascript.profileToJsonObject(dataStore.sharedProfile);
                Javascript.generateForm(
                        webForm,
                        request.blocks,
                        stringifiedProfile.toString(), //need to unstringify
                        dataStore.getSymmetricKey(),
                        formSymKey,
                        (result) -> {
                            onSubmit(result);
                        }
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveFormValues(String serializedForm) {
        DataStore dataStore = DataStore.getSingletonInstance();

        try {
            JSONObject jsonFields = new JSONObject(serializedForm);
            Iterator<String> jsonKeys = jsonFields.keys();
            while (jsonKeys.hasNext()) {
                String jsonKey = jsonKeys.next();
                String jsonValue = jsonFields.getString(jsonKey);
                dataStore.setBlockOptions(jsonKey, jsonValue);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void onSubmit(String result) {
        DataStore dataStore = DataStore.getSingletonInstance();
        Activity self = this;

        try {
            JSONObject aggregateResponses = new JSONObject(result);
            JSONObject encryptedResponses = aggregateResponses.getJSONObject("encryptedResponses");
            JSONObject encryptedPatch = aggregateResponses.getJSONObject("encryptedPatch");
            JSONObject patch = aggregateResponses.getJSONObject("patch");

            dataStore.applyPatch(patch);

            JSONObject responsesWithPatch = new JSONObject();
            responsesWithPatch.put("patch", encryptedPatch);
            Iterator<String> keys = encryptedResponses.keys();
            while (keys.hasNext()) {
                String jKey = keys.next();
                responsesWithPatch.put(jKey, encryptedResponses.get(jKey));
            }

            ListeningExecutorService ex = API.newExecutorService();
            ListenableFuture<Boolean> patchResult = API.postPatchedData(ex, responsesWithPatch, request);

            Futures.addCallback(patchResult, new FutureCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean result) {
                    Intent mainIntent = new Intent(self, MainActivity.class);
                    Log.i(TAG, "Back to main activity...");
                    startActivity(mainIntent);
                }

                @Override
                public void onFailure(Throwable thrown) {
                    CharSequence toastText = thrown.getMessage();
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(self, toastText, duration);
                    toast.show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
