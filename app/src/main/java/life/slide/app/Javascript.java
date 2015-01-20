package life.slide.app;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * Created by zeigjeder on 1/13/15.
 */
public class Javascript {
    private static final String TAG = "Slide -> Javascript";

    public static interface OnJavascriptEvalListener { public void callback(String jsResult); }

    public static Map<String, String> jsonObjectToMap(JSONObject json) throws JSONException {
        Map<String, String> ret = new HashMap<>();

        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            String value = json.getString(key);
            ret.put(key, value);
        }

        return ret;
    }

    public static JSONObject profileToJsonObject(Map<String, Set<String>> profile) throws JSONException {
        JSONObject ret = new JSONObject();

        for (String key : profile.keySet()) {
            Set<String> value = profile.get(key);
            JSONArray valueArray = new JSONArray(value);
            ret.put(key, valueArray);
        }

        return ret;
    }

    public static Map<String, Set<String>> jsonObjectToProfile(JSONObject json) throws JSONException {
        Map<String, Set<String>> ret = new HashMap<>();

        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Set<String> value = new HashSet<String>();
            JSONArray array = json.getJSONArray(key);
            for (int i = 0; i < array.length(); i++) value.add(array.getString(i));

            ret.put(key, value);
        }

        return ret;
    }

    public static void javascriptEval(WebView webView, String javascript, OnJavascriptEvalListener listener) {
        Log.i(TAG, "Evaluating: " + javascript);
        webView.evaluateJavascript(javascript, (result) -> {
            Log.i(TAG, "Response: " + result);
            listener.callback(result);
        });
    }

    public static void getResponses(WebView webView, OnJavascriptEvalListener cb) {
        javascriptEval(webView, "Forms.formFields()", cb);
    }

    public static void decrypt(WebView webView, String jsonObject, String key, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                String.format("JSON.stringify(Slide.crypto.AES.decryptData(%s, '%s'))", jsonObject, key),
                cb);
    }

    public static void encrypt(WebView webView, String jsonObject, String key, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                String.format("({fields: Slide.crypto.AES.encryptData(%s, %s), blocks:[]})", jsonObject, key),
                cb);
    }

    public static void generateSymmetricKey(WebView webView, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                "{symKey: Slide.crypto.AES.generateKey()}",
                cb);
    }

    public static void generatePemKeys(WebView webView, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                "(function() {" +
                    "var keys = forge.pki.rsa.generateKeyPair({ bits: 512, e: 0x10001 });" +
                    "var pemKeys = Slide.crypto.packKeys(keys);" +
                    "return pemKeys;" +
                "})()",
                cb);
    }

    public static void generateKeys(WebView webView, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                "forge.pki.rsa.generateKeyPair({ bits: 512, e: 0x10001 })",
                cb);
    }

    public static void decryptSymKey(WebView webView, String key, OnJavascriptEvalListener cb) {
        String privateKey = DataStore.getSingletonInstance().getPrivateKey();
        javascriptEval(webView,
                String.format("Slide.crypto.decryptStringWithPackedKey('%s', '%s')", key, privateKey),
                cb);
    }

    public static void encryptSymKey(WebView webView, String key, OnJavascriptEvalListener cb) {
        String publicKey = DataStore.getSingletonInstance().getPublicKey();
        javascriptEval(webView,
                String.format("Slide.crypto.encryptStringWithPackedKey('%s', '%s')", key, publicKey),
                cb);
    }

    public static WebView loadWebView(Activity activity, OnJavascriptEvalListener cb) throws IOException {
        WebView webView = new WebView(activity);

        initializeWebView(webView, cb);

        return webView;
    }

    public static void initializeWebView(WebView webView, OnJavascriptEvalListener cb) throws IOException {
        DataStore dataStore = DataStore.getSingletonInstance(webView.getContext());
        String jquery = dataStore.readResource(R.raw.jquery);
        String slideCrypto = dataStore.readResource(R.raw.slide);

        Javascript.javascriptEval(webView, jquery, (x) -> {
            Javascript.javascriptEval(webView, slideCrypto, cb);
        });
    }
}
