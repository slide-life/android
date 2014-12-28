package life.slide.app;

import android.content.Context;
import android.os.AsyncTask;
import android.sax.TextElementListener;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import com.google.common.util.concurrent.*;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.jar.Attributes;

/**
 * Created by Michael on 12/22/2014.
 */
public class SlideServices {
    private static final String TAG = "Slide -> SlideServices";

    private static final String HOSTNAME = "";
    private static final String PORT = "";
    private static final String USER_PATH = "users/";
    private static final String NEW_DEVICE_PATH = "new_device/";
    private static final String EXISTS_PATH = "exists/";
    private static final String CHANNEL_PATH = "channel/";

    private static String phoneNumber = "";

    public static String getPhoneNumber(Context context) {
        if (phoneNumber.isEmpty()) {
            TelephonyManager tMgr = (TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            return tMgr.getLine1Number();
        }

        return phoneNumber;
    }

    public static String getRootPath() {
        return "http://" + HOSTNAME + ":" + PORT + "/"; }
    public static String getUserPath(Context context) {
        return getRootPath() + USER_PATH + getPhoneNumber(context) + "/"; }
    public static String getNewDevicePath(Context context) {
        return getUserPath(context) + NEW_DEVICE_PATH; }
    public static String getExistsPath(Context context) {
        return getUserPath(context) + EXISTS_PATH; }
    public static String getChannelPath(String channelId) {
        return getRootPath() + CHANNEL_PATH + channelId; }

    public static ListeningExecutorService newExecutorService() {
        return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    }

    public static ListenableFuture<Boolean> getUserExists(
            ListeningExecutorService ex, Context context) {
        return getBooleanValue(ex, getExistsPath(context));
    }

    public static ListenableFuture<Boolean> postRegistrationId(
            ListeningExecutorService ex, Context context, String regId) {
        try {
            JSONObject ret = new JSONObject();
            ret.put("deviceType", "android");
            ret.put("key", regId);
            return postBooleanValue(ex, getNewDevicePath(context), ret);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ListenableFuture<Boolean> processRegistrationId(
            final ListeningExecutorService ex, final Context context, final String regId) {
        ListenableFuture<Boolean> userExists = getUserExists(ex, context);
        AsyncFunction<Boolean, Boolean> dependentExecution =
                new AsyncFunction<Boolean, Boolean>() {
                    @Override
                    public ListenableFuture<Boolean> apply(Boolean input) {
                        if (input) return postRegistrationId(ex, context, regId);
                        return returnListenableFuture(ex, true);
                    }
                };
        ListenableFuture<Boolean> total = Futures.transform(userExists, dependentExecution, ex);
        return total;
    }

    public static ListenableFuture<Boolean> postData(
            ListeningExecutorService ex, Map<String, String> fields, Request request) {
        JSONObject fieldsObject = new JSONObject(fields);
        ListenableFuture<Boolean> result = postBooleanValue(ex, getChannelPath(request.channelId), fieldsObject);
        return result;
    }

    public static Map<String, String> encrypt(Map<String, String> fields, String pem) {
        Map<String, String> ret = new HashMap<String, String>();

        PublicKey publicKey = publicKeyFromString(pem);
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            for (String key : fields.keySet()) {
                String item = fields.get(key);
                byte[] cipherData = cipher.doFinal(item.getBytes());
                String encodedItem = Base64.encodeToString(cipherData, Base64.DEFAULT);
                ret.put(key, encodedItem);
            }

            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static ListenableFuture<Boolean> returnListenableFuture(
            ListeningExecutorService service, final boolean value) {
        return service.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return value;
            }
        });
    }

    private static boolean booleanFromResponse(HttpResponse response) {
        StringBuilder builder = new StringBuilder();
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == 200) {
            HttpEntity entity = response.getEntity();
            try {
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) builder.append(line);
                String result = builder.toString();
                Log.i(TAG, "HTTP get result: " + result);

                JSONObject object = new JSONObject(line);
                return object.getBoolean("status");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "HTTP error, status: " + statusCode);
        }

        return false;
    }

    private static ListenableFuture<Boolean> getBooleanValue(
            ListeningExecutorService ex, final String url) {
        return ex.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                HttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                HttpResponse response = null;

                try {
                    response = client.execute(httpGet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return booleanFromResponse(response);
            }
        });
    }

    private static ListenableFuture<Boolean> postBooleanValue(
            ListeningExecutorService ex, final String url, final JSONObject data) {
        return ex.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() {
                HttpClient client = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(url);
                HttpResponse response = null;

                try {
                    ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
                    Iterator<String> keys = data.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        postParameters.add(new BasicNameValuePair(key, data.getString(key)));
                    }
                    httpPost.setEntity(new UrlEncodedFormEntity(postParameters));

                    response = client.execute(httpPost);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return booleanFromResponse(response);
            }
        });
    }

    private static PublicKey publicKeyFromString(String pem) {
        pem = new String(Base64.decode(pem.getBytes(), Base64.DEFAULT)); //get raw pem
        Log.i(TAG, "Pem obtained: " + pem);

        String finalPem = pem.
                replace("-----BEGIN PUBLIC KEY-----", "").
                replace("-----END PUBLIC KEY-----", "").
                replaceAll("\\s+", "");
        Log.i(TAG, "Base64: " + finalPem);

        byte[] decoded = Base64.decode(finalPem, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf;

        try {
            kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception nsa) {
            nsa.printStackTrace();
        }

        return null;
    }
}
