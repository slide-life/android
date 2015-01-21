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
            slideFormJs = dataStore.readResource(R.raw.slide_form);
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
        initializeSubmit();
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
                .replace("{{@slide-form.js}}", slideFormJs)
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
        ArrayList<String> fieldCommands = new ArrayList<>();
        for (String blockName : request.blocks) {
            Log.i(TAG, "Processing block " + blockName);
            BlockItem block = new BlockItem(blockName);
            Set<String> options = block.getOptions();
            ArrayList<String> optionsQuoted = new ArrayList<>();
            for (String o : options) optionsQuoted.add(String.format("\"%s\"", o));

            fieldCommands.add(String.format("Forms.selectField('%s', [%s], true)",
                    blockName, TextUtils.join(",", optionsQuoted)));
        }

        String addBlocksCommand = String.format("Forms.populateForm([%s]);", TextUtils.join(",", fieldCommands));
        Log.i(TAG, "JS command: " + addBlocksCommand);
        Javascript.javascriptEval(webForm, addBlocksCommand, (x) -> {});
    }

    private JSONObject saveFormValues(String serializedForm) {
        DataStore dataStore = DataStore.getSingletonInstance();
        JSONObject ret = new JSONObject();  //patch

        try {
            JSONObject jsonFields = new JSONObject(serializedForm);
            Iterator<String> jsonKeys = jsonFields.keys();
            while (jsonKeys.hasNext()) {
                String jsonKey = jsonKeys.next();
                String jsonValue = jsonFields.getString(jsonKey);
                if (! dataStore.isOptionForBlock(jsonKey, jsonValue)) {
                    JSONArray blockOptions = new JSONArray(dataStore.getBlockOptions(jsonKey));
                    blockOptions.put(jsonValue);

                    ret.put(jsonKey, Javascript.stringify(blockOptions));
                }
                dataStore.addOptionToBlock(jsonKey, jsonValue);
            }

            return ret;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void initializeSubmit() {
        DataStore dataStore = DataStore.getSingletonInstance(this);

        submitButton.setOnClickListener((view) -> {
            Javascript.getResponses(webForm, (formFields) -> {
                Log.i(TAG, "Form: " + formFields);
                JSONObject patch = saveFormValues(formFields);

                Javascript.decryptSymKey(webForm, request.key, (key) -> {
                    Log.i(TAG, "Decrypted key: " + key);
                    Javascript.encrypt(webForm, formFields, key, (encryptedResponses) -> {
                        Log.i(TAG, "Encrypted responses: " + encryptedResponses);

                        ListeningExecutorService exec = API.newExecutorService();
                        try {
                            JSONObject jsonEncryptedResponses = new JSONObject(encryptedResponses);
                            String userSymKey = dataStore.getSymmetricKey();

                            Javascript.encryptPatch(webForm, patch.toString(), userSymKey, (encryptedPatch) -> {
                                try {
                                    ListenableFuture<Boolean> dataPost = API.postData(exec,
                                            jsonEncryptedResponses,
                                            new JSONObject(encryptedPatch),
                                            request);
                                    Activity self = this;
                                    Futures.addCallback(dataPost, new FutureCallback<Boolean>() {
                                        @Override
                                        public void onSuccess(Boolean result) {
                                            //TODO: do something other than just go back
                                            Intent mainIntent = new Intent(self, MainActivity.class);
                                            Log.i(TAG, "Back to main activity...");
                                            startActivity(mainIntent);
                                        }

                                        @Override
                                        public void onFailure(Throwable thrown) {
                                            //TODO: do something better than just toast it
                                            Context context = getApplicationContext();
                                            CharSequence toastText = thrown.getMessage();
                                            int duration = Toast.LENGTH_SHORT;

                                            Toast toast = Toast.makeText(context, toastText, duration);
                                            toast.show();
                                        }
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                });
            });
        });
    }
}
