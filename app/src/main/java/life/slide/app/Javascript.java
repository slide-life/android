package life.slide.app;

import android.util.Log;
import android.webkit.WebView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by zeigjeder on 1/13/15.
 */
public class Javascript {
    private static final String TAG = "Slide -> Javascript";

    public static interface OnJavascriptEvalListener { public void callback(String jsResult); }

    public static Map<String, String> decodeJsonMap(JSONObject jsonObject) throws JSONException {
        Map<String, String> ret = new HashMap<>();

        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            ret.put(key, jsonObject.getString(key));
        }

        return ret;
    }
    public static JSONObject encodeJsonMap(Map<String, String> map) throws JSONException {
        JSONObject ret = new JSONObject();

        for (String key : map.keySet()) {
            ret.put(key, map.get(key));
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

    public static void encrypt(WebView webView, String jsonObject, String key, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                String.format("({fields: Slide.crypto.AES.encryptData(%s, %s), blocks:[]})", jsonObject, key),
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
}
