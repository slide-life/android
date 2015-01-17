package life.slide.app;

import android.app.ActionBar;
import android.content.Intent;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;


public class PushActivity extends ActionBarActivity {
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

        webForm = (WebView) findViewById(R.id.webForm);
        webForm.getSettings().setJavaScriptEnabled(true);

        submitButton = (Button) findViewById(R.id.submitButton);

        initializeWeb();
        initializeSubmit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_push, menu);
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
                .replace("{{@slide.js}}", slideJs); //TODO: replace form template with something else

        webForm.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                initializeRequest();
            }
        });

        webForm.loadDataWithBaseURL(BASE_URL, formTemplateHtml, "text/html", "UTF-8", "");
    }

    private void initializeRequest() {
        //TODO: display request

    }

    private void initializeSubmit() {
        submitButton.setOnClickListener((view) -> {
            DataStore dataStore = DataStore.getSingletonInstance();
            dataStore.processPullRequest(request);
        });
    }
}
