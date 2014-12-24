package life.slide.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class QRFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String REQUEST_INTENT = "request-received";

    private DataStore dataStore;

    public Context context;
    public Button scanButton;

    public static QRFragment newInstance(int sectionNumber) {
        QRFragment fragment = new QRFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public QRFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_qr, container, false);
        context = rootView.getContext();

        dataStore = DataStore.getSingletonInstance(getActivity());

        scanButton = (Button) rootView.findViewById(R.id.scanButton);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, 0);
            }
        });

        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                //String format = intent.getStringExtra("SCAN_RESULT_FORMAT");

                dataStore.insertRawRequest(contents);
                Intent received = new Intent(REQUEST_INTENT);
                received.putExtra("request", contents);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(received);
            }
        }
    }
}