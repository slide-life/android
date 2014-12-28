package life.slide.app;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RequestActivity extends ActionBarActivity {
    private static final String TAG = "Slide -> RequestActivity";
    private static final int LABEL_WIDTH = 500;
    private static final int ELEMENT_HEIGHT = 80;
    private static final int TEXT_SIZE = 20;

    public Request request;

    public TextView requestLabel;
    public LinearLayout form;
    public Button submitButton;
    public Map<String, View> fields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        ActionBar ab = getActionBar();
        if (ab != null) ab.setTitle("Confirm request");

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

            LinearLayout blockEntry = new LinearLayout(this);
            LayoutParams blockEntryParams = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    ELEMENT_HEIGHT
            );
            blockEntry.setLayoutParams(blockEntryParams);
            form.addView(blockEntry);

            createBlockEntry(blockEntry, block);
        }
    }

    private void createBlockEntry(LinearLayout blockEntry, BlockItem block) {
        String blockName = block.getBlockName();

        //label
        TextView blockLabel = new TextView(this);
        blockLabel.setText(blockName);
        LayoutParams labelParams = new LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
        );
        blockLabel.setGravity(Gravity.TOP);
        blockLabel.setLayoutParams(labelParams);
        blockLabel.setTextSize(TEXT_SIZE);

        blockEntry.addView(blockLabel);

        if (block.hasOptions()) {
            initializeSpinner(block, blockEntry);
        } else {
            initializeEditor(block, blockEntry);
        }
    }

    private void initializeEditor(BlockItem block, LinearLayout blockEntry) {
        String blockName = block.getBlockName();
        Log.i(TAG, "Empty block " + blockName);

        EditText editor = new EditText(this);
        editor.setTextSize(TEXT_SIZE / 2);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(editor, InputMethodManager.SHOW_IMPLICIT);

        LayoutParams editorParams = new LayoutParams(
                LayoutParams.FILL_PARENT,
                ELEMENT_HEIGHT
        );
        editor.setLayoutParams(editorParams);

        blockEntry.addView(editor);

        fields.put(blockName, editor);
    }

    private void initializeSpinner(BlockItem block, LinearLayout blockEntry) {
        String blockName = block.getBlockName();
        Log.i(TAG, "Full block " + blockName);

        Pair<String, Set<String>> options = block.getOptions();
        Spinner spinner = new Spinner(this);

        LayoutParams spinnerParams = new LayoutParams(
                LayoutParams.FILL_PARENT,
                ELEMENT_HEIGHT
        );
        spinner.setLayoutParams(spinnerParams);

        ArrayList<String> optionsList = new ArrayList<String>(options.second);
        int posOfDefault = optionsList.indexOf(options.first);
        SpinnerAdapter adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, optionsList);
        spinner.setAdapter(adapter);
        spinner.setSelection(posOfDefault);

        blockEntry.addView(spinner);

        fields.put(blockName, spinner);
    }

    private void initializeSubmit() {
        final RequestActivity context = this;

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
                        //add block to index and add default option to block
                        BlockItem block = new BlockItem(blockName);
                        block.addOption(input);

                        //set value of data
                        blockValues.put(blockName, input);
                    }
                }

                Log.i(TAG, "Data map obtained: " + blockValues);

                submitData(blockValues);
            }
        });
    }

    private void submitData(Map<String, String> blockValues) {
        //encrypt data and send to server
        final Context context = this;
        Map<String, String> encodedBlockValues = SlideServices.encrypt(blockValues, request.pubKey);
        Log.i(TAG, "Encoded values: " + encodedBlockValues.toString());

        ListeningExecutorService service = SlideServices.newExecutorService();
        ListenableFuture<Boolean> submission = SlideServices.postData(service, encodedBlockValues, request);
        Futures.addCallback(submission, new FutureCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                        /* TODO:
                         * - remove this Request from the list of open requests (or mark complete)
                         * - display some success message
                         */
                Intent requestsActivityIntent = new Intent(context, MainActivity.class);
                requestsActivityIntent.putExtra("request", request.toJson());
                Log.i(TAG, "Going back to requests activity...");
                startActivity(requestsActivityIntent);
            }

            @Override
            public void onFailure(Throwable t) {
                Log.i(TAG, "Failure of submission: " + t.getMessage());
                        /* TODO:
                         * - display toast error message on UI thread.
                         */
            }
        });
    }
}
