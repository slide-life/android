package life.slide.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.io.InputStream;
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

    private final String INDEX = "index";
    private final String PRIVATE_KEY = "private_key";
    private final String PUBLIC_KEY = "public_key";

    private final String BLOCK_STORAGE_FILE = "blocks";
    private final String REQUEST_STORAGE_FILE = "requests";
    private final String PRIVATE_KEY_FILE = "private_key";

    public Context context;
    public SharedPreferences blocks, requests, privateKey;

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
        this.privateKey = context.getSharedPreferences(PRIVATE_KEY_FILE, Context.MODE_PRIVATE);
        initializeRequests();
        initializeBlocks();
    }

    public String readResource(int resourceId) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        byte[] reader = new byte[inputStream.available()];
        while (inputStream.read(reader) != -1);

        return new String(reader);
    }

    public void setPrivateKey(String s) {
        Editor editor = privateKey.edit();
        editor.putString(PRIVATE_KEY, s);
        editor.commit();
    }

    public void setPublicKey(String s) {
        Editor editor = privateKey.edit();
        editor.putString(PUBLIC_KEY, s);
        editor.commit();
    }

    public String getPrivateKey() {
        return privateKey.getString(PRIVATE_KEY, "");
    }

    public String getPublicKey() {
        return privateKey.getString(PUBLIC_KEY, "");
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
        if (!requests.contains(INDEX)) {
            putIndex(requests, new HashSet<>());
        }
    }

    private void initializeBlocks() {
        if (!blocks.contains(INDEX)) {
            putIndex(blocks, new HashSet<>());
        }
    }

    private Set<String> getIndex(SharedPreferences prefs) {
        return prefs.getStringSet(INDEX, new HashSet<>());
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
        editor.putStringSet(INDEX, index);
        editor.commit();
    }

    private String getOptionsName(String blockName) {
        return blockName + "$options";
    }
}
