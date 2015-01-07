package life.slide.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Michael on 12/22/2014.
 * Additional NOTE: since we're using string Sets, two identical requests will result in only one element
 */
public class DataStore {
    private static DataStore singletonInstance;

    private final String TAG = "Slide -> DataStore";
    private final String BLOCK_STORAGE_FILE = "blocks";
    private final String REQUEST_STORAGE_FILE = "requests";

    public Context context;
    public SharedPreferences blocks, requests;

    public static DataStore getSingletonInstance() {
        return DataStore.singletonInstance; //not safe
    }

    public static DataStore getSingletonInstance(Context context) {
        if (DataStore.singletonInstance != null) return DataStore.singletonInstance;
        DataStore.singletonInstance = new DataStore(context);
        return DataStore.singletonInstance;
    }

    public DataStore(Context context) {
        Log.i(TAG, "dataStore instance");

        this.context = context;
        this.blocks = context.getSharedPreferences(BLOCK_STORAGE_FILE, Context.MODE_PRIVATE);
        this.requests = context.getSharedPreferences(REQUEST_STORAGE_FILE, Context.MODE_PRIVATE);
        initializeRequests();
        initializeBlocks();
    }

    public void insertRequest(Request request) {
        String serializedRequest = request.toJson();
        addToIndex(requests, serializedRequest);
    }

    public void insertRawRequest(String request) {
        addToIndex(requests, request);
    }

    public void commitRequestList(ArrayList<Request> requestList) {
        Set<String> requestSet = new HashSet<>();
        for (Request r : requestList) requestSet.add(r.toJson());
        putIndex(requests, requestSet);
    }

    public ArrayList<Request> getRequests() {
        Set<String> index = getIndex(requests);
        ArrayList<Request> ret = new ArrayList<Request>();
        for (String s : index) ret.add(new Request(s));
        return ret;
    }

    public boolean inIndex(String blockName) {
        return getIndex(blocks).contains(blockName);
    }

    public Pair<String, Set<String>> getBlockOptions(String blockName) {
        String defaultOption = blocks.getString(blockName, "");
        Set<String> blockOptions = blocks.getStringSet(
                getOptionsName(blockName), new HashSet<>());
        return new Pair<>(defaultOption, blockOptions);
    }

    public boolean addOptionToBlock(String blockName, String option) {
        addToIndex(blocks, blockName);

        Set<String> options = blocks.getStringSet(getOptionsName(blockName), new HashSet<>());
        options.add(option);

        Editor editor = blocks.edit();
        editor.putString(blockName, option);
        editor.putStringSet(getOptionsName(blockName), options);
        return editor.commit();
    }

    private void initializeRequests() {
        if (!requests.contains("index")) {
            putIndex(requests, new HashSet<>());
        }
    }

    private void initializeBlocks() {
        if (!blocks.contains("index")) {
            putIndex(blocks, new HashSet<>());
        }
    }

    private Set<String> getIndex(SharedPreferences prefs) {
        return prefs.getStringSet("index", new HashSet<>());
    }

    private void addToIndex(SharedPreferences prefs, String item) {
        Set<String> currentIndex = getIndex(prefs);
        currentIndex.add(item);
        putIndex(prefs, currentIndex);
    }

    private void removeFromIndex(SharedPreferences prefs, String item) {
        Set<String> currentIndex = getIndex(prefs);
        currentIndex.remove(item);
        putIndex(prefs, currentIndex);
    }

    private void putIndex(SharedPreferences prefs, Set<String> index) {
        index = new HashSet<>(index); //necessary because of vagaries of Set and SharedPrefs
        Editor editor = prefs.edit();
        editor.putString("history", new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date()));
        editor.putStringSet("index", index);
        editor.commit();
    }

    private String getOptionsName(String blockName) {
        return blockName + "$options";
    }
}
