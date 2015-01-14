package life.slide.app;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class RequestActivity extends ActionBarActivity {
    private static final String TAG = "Slide -> RequestActivity";

    private final String BASE_URL = getResources().getString(R.string.hostname);

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

        try {
            slideJs = API.readResource(this, R.raw.slide);
            slideFormJs = API.readResource(this, R.raw.slide_form);
            stylesCss = API.readResource(this, R.raw.styles);
            jqueryJs = API.readResource(this, R.raw.jquery);
            formTemplateHtml = API.readResource(this, R.raw.form_template);
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

        webForm = (WebView) findViewById(R.id.webForm);
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

    private void initializeForm() { //TODO: move javascript to Javascript class
        ArrayList<String> fieldCommands = new ArrayList<>();
        for (String blockName : request.blocks) {
            Log.i(TAG, "Processing block " + blockName);
            BlockItem block = new BlockItem(blockName);

            fieldCommands.add("Forms.selectField('" + blockName +
                    "', [" + TextUtils.join(",", block.getOptions().second) + "], true)");
        }

        String addBlocksCommand = "Forms.populateForm([" + TextUtils.join(",", fieldCommands) + "]);";
        webForm.loadUrl("javascript:" + addBlocksCommand);
    }

    private void initializeSubmit() {
        submitButton.setOnClickListener((view) -> {
            Javascript.getResponses(webForm, (serializedForm) -> {
                Javascript.decryptSymKey(webForm, request.pubKey, (key) -> {
                    Javascript.encrypt(webForm, serializedForm, key, (encryptedResponses) -> {
                        ListeningExecutorService exec = API.newExecutorService();
                        try {
                            API.postData(exec, new JSONObject(encryptedResponses), request);
                        } catch (JSONException e) {
                            Log.i(TAG, "JSON exception for json: " + encryptedResponses + "!");
                            e.printStackTrace();
                        }
                    });
                });
            });
        });
    }
}
