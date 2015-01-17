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

/**
 * Created by Michael on 12/23/2014.
 */
public class RequestsFragment extends android.support.v4.app.Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "Slide -> RequestsFragment";

    private DataStore dataStore;

    public ListView requestsList;
    public Context context;
    public ArrayAdapter<Request> requestAdapter;
    public ArrayList<Request> requests;
    public BroadcastReceiver recv = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String requestJson = intent.getStringExtra("request");
            Log.i(TAG, "Received: " + requestJson);

            dataStore.insertRawRequest(requestJson);

            Request request = new Request(requestJson);
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
        LocalBroadcastManager.getInstance(context).unregisterReceiver(recv);
        super.onPause();
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(context).registerReceiver(recv,
                new IntentFilter("request-received"));
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_requests, container, false);
        context = rootView.getContext();

        dataStore = DataStore.getSingletonInstance(getActivity());

        requests = dataStore.getRequests();
        requestsList = (ListView) rootView.findViewById(R.id.requestsList);
        requestAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_list_item_1, requests);
        requestsList.setAdapter(requestAdapter);
        requestsList.setOnItemClickListener((parent, view, position, id) -> {
            Log.i(TAG, "Clicked...");
            Request req = requestAdapter.getItem(position);

            Intent requestActivityIntent = new Intent(view.getContext(), RequestActivity.class);
            requestActivityIntent.putExtra("request", req.toJson());
            Log.i(TAG, "Starting request activity...");
            startActivity(requestActivityIntent);
        });

        return rootView;
    }
}
