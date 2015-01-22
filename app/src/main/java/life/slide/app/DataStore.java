package life.slide.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Pair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

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
    private final String SYMMETRIC_KEY = "key";

    private final String REQUEST_STORAGE_FILE = "requests";
    private final String PRIVATE_KEY_FILE = "private_key";

    public Context context;
    public SharedPreferences requests, privateKey;
    public static Map<String, String> sharedProfile;

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
        this.requests = context.getSharedPreferences(REQUEST_STORAGE_FILE, Context.MODE_PRIVATE);
        this.privateKey = context.getSharedPreferences(PRIVATE_KEY_FILE, Context.MODE_PRIVATE);

        this.sharedProfile = new HashMap<>();

        initializeRequests();
    }

    public String readResource(int resourceId) throws IOException {
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        byte[] reader = new byte[inputStream.available()];
        while (inputStream.read(reader) != -1);

        return new String(reader);
    }

    public void storeProfile(JSONObject profile) throws JSONException {
        this.sharedProfile = Javascript.jsonObjectToProfile(profile);
    }

    public void applyPatch(Map<String, String> patch) throws JSONException {
        for (String key : patch.keySet()) {
            String value = patch.get(key);
            sharedProfile.put(key, value);
        }
    }

    public void applyPatch(JSONObject patch) throws JSONException {
        Map<String, String> patchMap = Javascript.jsonObjectToProfile(patch);
        applyPatch(patchMap);
    }

    public String getBlockOptions(String blockName) {
        if (sharedProfile.containsKey(blockName)) return sharedProfile.get(blockName);
        return "{}";
    }

    public boolean isOptionForBlock(String option, String blockName) {
        return getBlockOptions(blockName).contains(option);
    }

    public void setBlockOptions(String blockName, String options) {
        sharedProfile.put(blockName, options);
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

    public void setSymmetricKey(String s) {
        Editor editor = privateKey.edit();
        editor.putString(SYMMETRIC_KEY, s);
        editor.commit();
    }

    public String getPrivateKey() {
        return privateKey.getString(PRIVATE_KEY, "");
    }

    public String getPublicKey() {
        return privateKey.getString(PUBLIC_KEY, "");
    }

    public String getSymmetricKey() { return privateKey.getString(SYMMETRIC_KEY, ""); }

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

    private void initializeRequests() {
        if (!requests.contains(INDEX)) {
            putIndex(requests, new HashSet<>());
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
}
