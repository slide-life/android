package life.slide.app;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
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
        for (String blockName : request.blocks) {
            //create block
            BlockItem block = new BlockItem(blockName);
            Pair<String, Set<String>> options = block.getOptions();

            RelativeLayout blockEntry = new RelativeLayout(this);

            //label
            TextView blockLabel = new TextView(this);
            blockLabel.setWidth(LayoutParams.WRAP_CONTENT);
            blockLabel.setHeight(LayoutParams.WRAP_CONTENT);
            RelativeLayout.LayoutParams labelParams = (RelativeLayout.LayoutParams) blockLabel.getLayoutParams();
            labelParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            blockLabel.setLayoutParams(labelParams);
            blockEntry.addView(blockLabel);

            if (options.second.isEmpty()) {
                EditText editor = new EditText(this);
                editor.setWidth(blockEntry.getWidth());
                editor.setHeight(LayoutParams.WRAP_CONTENT);
                RelativeLayout.LayoutParams editorParams = (RelativeLayout.LayoutParams) editor.getLayoutParams();
                editorParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                editor.setLayoutParams(editorParams);
                blockEntry.addView(editor);

                fields.put(block.getBlockName(), editor);
            } else {
                Spinner spinner = new Spinner(this);

                RelativeLayout.LayoutParams spinnerParams = (RelativeLayout.LayoutParams) spinner.getLayoutParams();
                spinnerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
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

            form.addView(blockEntry);
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
