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
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
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

    private static final String HOSTNAME = "api-sandbox.slide.life";
    private static final String PORT = "";
    private static final String BLOCKS_PATH = "blocks/";
    private static final String USER_PATH = "users/";
    private static final String NEW_DEVICE_PATH = "new_device/";
    private static final String EXISTS_PATH = "exists/";
    private static final String CHANNEL_PATH = "channels/";

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
        return "http://" + HOSTNAME + (PORT.equals("") ? "" : ":" + PORT) + "/"; }
    public static String getBlocksPath() {
        return getRootPath() + BLOCKS_PATH; }
    public static String getNewUserPath() {
        return getRootPath() + USER_PATH; }
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

    public static ListenableFuture<ArrayList<BlockItem>> getBlocks(
            final ListeningExecutorService ex, Context context) {
        ListenableFuture<HttpResponse> blockResponse = getRequest(ex, getBlocksPath());

        AsyncFunction<HttpResponse, ArrayList<BlockItem>> getBlockItems =
                new AsyncFunction<HttpResponse, ArrayList<BlockItem>>() {
                    @Override
                    public ListenableFuture<ArrayList<BlockItem>> apply(HttpResponse input) throws Exception {
                        //get json
                        JSONArray array = jsonArrayFromResponse(input);
                        //create blocks through json
                        ArrayList<BlockItem> blocks = new ArrayList<BlockItem>();
                        for (int i = 0; i < array.length(); i++) {
                            blocks.add(new BlockItem(array.getJSONObject(i)));
                        }

                        return returnListenableFuture(ex, blocks);
                    }
                };
        ListenableFuture<ArrayList<BlockItem>> total = Futures.transform(blockResponse, getBlockItems, ex);
        return total;
    }

    public static ListenableFuture<Boolean> getUserExists(
            ListeningExecutorService ex, Context context) {
        return getBooleanValue(ex, getExistsPath(context));
    }

    public static ListenableFuture<Boolean> postUser(
            ListeningExecutorService ex, Context context) {
        try {
            JSONObject ret = new JSONObject();
            ret.put("user", getPhoneNumber(context));
            return postBooleanValue(ex, getNewUserPath(), ret);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
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
        AsyncFunction<Boolean, Boolean> submitUser = //fucking monad chaining in Java takes 5 lines
                new AsyncFunction<Boolean, Boolean>() {
                    @Override
                    public ListenableFuture<Boolean> apply(Boolean input) {
                        if (!input) return postUser(ex, context);
                        return returnListenableFuture(ex, true);
                    }
                };
        AsyncFunction<Boolean, Boolean> submitRegId =
                new AsyncFunction<Boolean, Boolean>() {
                    @Override
                    public ListenableFuture<Boolean> apply(Boolean input) throws Exception {
                        if (input) return postRegistrationId(ex, context, regId);
                        return returnListenableFuture(ex, true);
                    }
                };

        ListenableFuture<Boolean> total = Futures.transform(userExists, submitUser, ex);
        total = Futures.transform(total, submitRegId, ex);
        return total;
    }

    public static ListenableFuture<Boolean> postData(
            final ListeningExecutorService ex, Map<String, String> fields, Request request) {
        JSONObject data = new JSONObject();
        JSONObject fieldsObject = new JSONObject();
        try {
            for (String k : fields.keySet())
                fieldsObject.put(k, fields.get(k));
            data.put("fields", fieldsObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Encoded fields JSON: " + data.toString());

        ListenableFuture<Boolean> result = postBooleanValue(ex, getChannelPath(request.channelId), data);
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

    private static <T> ListenableFuture<T> returnListenableFuture(
            ListeningExecutorService service, final T value) {
        return service.submit(new Callable<T>() {
            @Override
            public T call() {
                return value;
            }
        });
    }

    private static String jsonFromResponse(HttpResponse response) {
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

                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "HTTP error, status: " + statusCode);
            try {
                String inputLine;
                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                while ((inputLine = reader.readLine()) != null) {
                    Log.i(TAG, inputLine);
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private static JSONObject jsonObjectFromResponse(HttpResponse response) {
        try {
            JSONObject object = new JSONObject(jsonFromResponse(response));
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static JSONArray jsonArrayFromResponse(HttpResponse response) {
        try {
            JSONArray object = new JSONArray(jsonFromResponse(response));
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static boolean booleanFromResponse(HttpResponse response) {
        JSONObject jsonObject = jsonObjectFromResponse(response);
        if (jsonObject != null) {
            try {
                return jsonObject.getBoolean("status");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

    private static AsyncFunction<HttpResponse, Boolean> responseFutureToBoolean(
            final ListeningExecutorService ex) {
        return new AsyncFunction<HttpResponse, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(HttpResponse input) throws Exception {
                return returnListenableFuture(ex, booleanFromResponse(input));
            }
        };
    }

    private static ListenableFuture<HttpResponse> response(
            final ListeningExecutorService ex, final HttpUriRequest uriRequest) {
        return ex.submit(new Callable<HttpResponse>() {
            @Override
            public HttpResponse call() throws Exception {
                Log.i(TAG, "Response being sent... " + uriRequest.getMethod()
                        + "->" + uriRequest.getURI().toString() + " : " + uriRequest.getParams().toString());

                HttpClient client = new DefaultHttpClient();
                HttpResponse response = null;

                try {
                    response = client.execute(uriRequest);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return response;
            }
        });
    }

    private static ListenableFuture<HttpResponse> getRequest(
            final ListeningExecutorService ex, final String url) {
        HttpGet httpGet = new HttpGet(url);

        return response(ex, httpGet);
    }

    private static ListenableFuture<HttpResponse> postRequest(
            final ListeningExecutorService ex, final String url, final JSONObject data) {
        HttpPost httpPost = new HttpPost(url);
        try {
            ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();

            /*
            Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                postParameters.add(new BasicNameValuePair(key, data.getString(key)));
            }
            httpPost.setEntity(new UrlEncodedFormEntity(postParameters)); */ //form mode
            StringEntity se = new StringEntity(data.toString());
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            httpPost.setEntity(se);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response(ex, httpPost);
    }

    private static ListenableFuture<Boolean> getBooleanValue(
            final ListeningExecutorService ex, final String url) {
        ListenableFuture<HttpResponse> response = getRequest(ex, url);

        AsyncFunction<HttpResponse, Boolean> responseToBoolean = responseFutureToBoolean(ex);
        ListenableFuture<Boolean> result = Futures.transform(response, responseToBoolean);

        return result;
    }

    private static ListenableFuture<Boolean> postBooleanValue(
            final ListeningExecutorService ex, final String url, final JSONObject data) {
        ListenableFuture<HttpResponse> response = postRequest(ex, url, data);

        AsyncFunction<HttpResponse, Boolean> responseToBoolean = responseFutureToBoolean(ex);
        ListenableFuture<Boolean> result = Futures.transform(response, responseToBoolean);

        return result;
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
