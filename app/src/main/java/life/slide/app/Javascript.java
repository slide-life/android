package life.slide.app;

import android.webkit.WebView;

/**
 * Created by zeigjeder on 1/13/15.
 */
public class Javascript {
    private static int htmloutCount = 0;

    public static interface OnJavascriptEvalListener { public void callback(String jsResult); }

    public static void javascriptEval(WebView webView, String javascript, OnJavascriptEvalListener listener) {
        htmloutCount++;
        String htmloutId = "HTMLOUT" + htmloutCount;
        webView.addJavascriptInterface(listener, htmloutId);
        webView.loadUrl("javascript:( function() { var __result = " + javascript +
                "; window." + htmloutId + ".callback(__result); } ) ()");
    }

    public static void javascriptStatements(WebView webView,
                                            String javascript,
                                            String variable,
                                            OnJavascriptEvalListener listener) {
        htmloutCount++;
        String htmloutId = "HTMLOUT" + htmloutCount;
        webView.addJavascriptInterface(listener, htmloutId);
        webView.loadUrl(String.format(
                "javascript:%s; var __result = %s; window.%s.callback(__result);", javascript, variable, htmloutId));
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
        javascriptStatements(webView, "var keys; Slide.crypto.generateKeys(function(k){keys=k;})", "keys", cb);
    }

    public static void decryptSymKey(WebView webView, String key, OnJavascriptEvalListener cb) {
        String privateKey = DataStore.getSingletonInstance().getPrivateKey();
        javascriptEval(webView,
                String.format("Slide.crypto.decryptStringWithPackedKey('%s', '%s')", key, privateKey),
                cb);
    }
}
