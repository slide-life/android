package life.slide.app;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.Set;


public class DepositActivity extends ActionBarActivity {
    public Request request;

    public TextView requestLabel;
    public WebView depositView;
    public Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);

        Intent intent = getIntent();
        request = new Request(intent.getStringExtra("deposit"));

        requestLabel = (TextView) findViewById(R.id.requestLabel);
        depositView = (WebView) findViewById(R.id.depositView);

        submitButton = (Button) findViewById(R.id.submitButton);
        submitButton.setEnabled(false);

        try {
            Javascript.initializeWebView(depositView, (result) -> {
                initializeWeb();
                submitButton.setEnabled(true);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        initializeSubmit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_deposit, menu);
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

    }

    private void initializeSubmit() {
        submitButton.setOnClickListener((view) -> {
            Javascript.decryptSymKey(depositView, request.key, (key) -> {
                try {
                    Javascript.decrypt(depositView, Javascript.profileToJsonObject(request.fields).toString(), key,
                        (decryptedData) -> {
                            try {
                                DataStore dataStore = DataStore.getSingletonInstance();

                                JSONObject decryptedFields = new JSONObject(decryptedData);
                                dataStore.applyPatch(decryptedFields);

                                Javascript.encryptPatch(depositView, decryptedData, dataStore.getSymmetricKey(),
                                        (encryptedPatch) -> {
                                            try {
                                                JSONObject encryptedPatchJson = new JSONObject(encryptedPatch);

                                                ListeningExecutorService ex = API.newExecutorService();
                                                API.postPatch(ex, this, encryptedPatchJson);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        });
    }
}
