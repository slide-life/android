package life.slide.app;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public class RequestActivity extends ActionBarActivity {
    private static final String TAG = "Slide -> RequestActivity";

    public Request request;

    public TextView requestLabel;
    public LinearLayout form;
    public Button submitButton;
    public Map<String, View> fields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        Intent intent = getIntent();
        request = new Request(intent.getStringExtra("request"));

        requestLabel = (TextView) findViewById(R.id.requestLabel);
        requestLabel.setText(request.name);

        form = (LinearLayout) findViewById(R.id.form);
        initializeForm();

        submitButton = (Button) findViewById(R.id.submitButton);
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

    private void initializeForm() {
        fields = new HashMap<String, View>();

        for (String blockName : request.blocks) {
            Log.i(TAG, "Processing block " + blockName);
            //create block
            BlockItem block = new BlockItem(blockName);
            Pair<String, Set<String>> options = block.getOptions();

            LinearLayout blockEntry = new LinearLayout(this);
            LayoutParams blockEntryParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    40
            );
            blockEntry.setLayoutParams(blockEntryParams);
            form.addView(blockEntry);

            //label
            TextView blockLabel = new TextView(this);
            blockLabel.setText(blockName);
            blockLabel.setWidth(LayoutParams.WRAP_CONTENT);
            blockLabel.setHeight(LayoutParams.WRAP_CONTENT);

            blockEntry.addView(blockLabel);

            if (options.second.isEmpty()) {
                Log.i(TAG, "Empty block " + blockName);
                EditText editor = new EditText(this);
                LayoutParams editorParams = new LayoutParams(
                        form.getWidth() - 100,
                        40
                );
                editor.setLayoutParams(editorParams);

                blockEntry.addView(editor);

                fields.put(block.getBlockName(), editor);
            } else {
                Log.i(TAG, "Full block " + blockName);
                Spinner spinner = new Spinner(this);

                LayoutParams spinnerParams = new LayoutParams(
                        form.getWidth() - 100,
                        40
                );
                spinner.setLayoutParams(spinnerParams);

                ArrayList<String> optionsList = new ArrayList<String>(options.second);
                int posOfDefault = optionsList.indexOf(options.first);
                SpinnerAdapter adapter = new ArrayAdapter<String>(
                        this, android.R.layout.simple_list_item_1, optionsList);
                spinner.setAdapter(adapter);
                spinner.setSelection(posOfDefault);

                blockEntry.addView(spinner);

                fields.put(block.getBlockName(), spinner);
            }

        }
    }

    private void initializeSubmit() {
        submitButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set any fields that were filled in and fill all fields
                Map<String, String> blockValues = new HashMap<String, String>();
                for (String blockName : fields.keySet()) {
                    View blockEditView = fields.get(blockName);
                    String input = "";
                    if (blockEditView instanceof EditText)
                        input = ((EditText) blockEditView).getText().toString();
                    else if (blockEditView instanceof Spinner)
                        input = ((Spinner) blockEditView).getSelectedItem().toString();

                    if (!input.isEmpty()) {
                        BlockItem block = new BlockItem(blockName);
                        block.addOption(input);

                        blockValues.put(blockName, input);
                    }
                }

                //encrypt data and send to server
                Map<String, String> encodedBlockValues = SlideServices.encrypt(blockValues, request.pubKey);
                Future<Boolean> submission = SlideServices.postData(encodedBlockValues, request);
                //execute submission
            }
        });
    }
}
