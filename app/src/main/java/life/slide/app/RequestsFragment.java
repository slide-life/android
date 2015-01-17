package life.slide.app;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michael on 12/23/2014.
 */
public class RequestsFragment extends android.support.v4.app.Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "Slide -> RequestsFragment";

    private static final int REQUEST = 0;
    private static final int PUSH = 1;

    private static final String REQUEST_INTENT = "request-received";

    private DataStore dataStore;

    public ListView requestsList;
    public Context context;
    public ArrayAdapter<Request> requestAdapter;
    public ArrayList<Request> requests;

    public Map<String, BroadcastReceiver> receivers = new HashMap<>();
    public BroadcastReceiver requestRecv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String requestJson = intent.getStringExtra("request");
            Log.i(TAG, "Request received: " + requestJson);

            Request request = new Request(requestJson);
            dataStore.insertRequest(request);

            requests.add(request);
            requestAdapter.notifyDataSetChanged();
        }
    };

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
        for (String intent : receivers.keySet())
            LocalBroadcastManager.getInstance(context).unregisterReceiver(receivers.get(intent));
        super.onPause();
    }

    @Override
    public void onResume() {
        for (String intent : receivers.keySet())
            LocalBroadcastManager.getInstance(context).registerReceiver(
                receivers.get(intent), new IntentFilter(intent));
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_requests, container, false);
        context = rootView.getContext();

        receivers.put(REQUEST_INTENT, requestRecv);

        dataStore = DataStore.getSingletonInstance(getActivity());

        requests = dataStore.getRequests();
        requestsList = (ListView) rootView.findViewById(R.id.requestsList);
        requestAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, requests);
        requestsList.setAdapter(requestAdapter);
        requestsList.setOnItemClickListener((parent, view, position, id) -> {
            Log.i(TAG, "Clicked...");
            Request req = requestAdapter.getItem(position);

            switch (req.type) {
                case REQUEST:
                    Intent requestActivityIntent = new Intent(view.getContext(), RequestActivity.class);
                    requestActivityIntent.putExtra("request", req.toJson());
                    Log.i(TAG, "Starting request activity...");
                    startActivity(requestActivityIntent);
                case PUSH:
                    Intent pushActivityIntent = new Intent(view.getContext(), PushActivity.class);
                    pushActivityIntent.putExtra("request", req.toJson());
                    Log.i(TAG, "Starting request activity...");
                    startActivity(pushActivityIntent);
            }
        });

        return rootView;
    }
}
