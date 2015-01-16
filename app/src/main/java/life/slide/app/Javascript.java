package life.slide.app;

import android.util.Log;
import android.webkit.WebView;

/**
 * Created by zeigjeder on 1/13/15.
 */
public class Javascript {
    private static final String TAG = "Slide -> Javascript";

    public static interface OnJavascriptEvalListener { public void callback(String jsResult); }

    public static void javascriptEval(WebView webView, String javascript, OnJavascriptEvalListener listener) {
        Log.i(TAG, "Evaluating: " + javascript);
        webView.evaluateJavascript(javascript, (result) -> {
            Log.i(TAG, "Response: " + result);
            listener.callback(result);
        });
    }

    public static void getResponses(WebView webView, OnJavascriptEvalListener cb) {
        javascriptEval(webView, "Forms.serializeForm()", cb);
    }

    public static void encrypt(WebView webView, String jsonObject, String key, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                String.format("JSON.stringify({fields: Slide.crypto.AES.encryptData(%s, '%s'), blocks:[]})", jsonObject, key),
                cb);
    }

    public static void generateKeys(WebView webView, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                "forge.pki.rsa.generateKeyPair({ bits: 512, e: 0x10001 })",
                cb);
    }

    public static void getPublicKey(WebView webView, String privateKey, OnJavascriptEvalListener cb) {
        javascriptEval(webView,
                String.format("forge.pki.rsa.setPublicKey(%s.n, %s.e)", privateKey, privateKey),
                cb);
    }

    public static void decryptSymKey(WebView webView, String key, OnJavascriptEvalListener cb) {
        String privateKey = DataStore.getSingletonInstance().getPrivateKey();
        javascriptEval(webView,
                String.format("Slide.crypto.decryptStringWithPackedKey('%s', '%s')", key, privateKey),
                cb);
    }
}
