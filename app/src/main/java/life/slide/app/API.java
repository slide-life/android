package life.slide.app;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import android.webkit.WebView;
import com.google.common.util.concurrent.*;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.*;

/**
 * Created by Michael on 12/22/2014.
 */
public class API {
    private static final String TAG = "Slide -> API";
    private static final String HOSTNAME = "slide-sandbox.herokuapp.com";
    private static final String PORT = "";

    private static final String BLOCKS_PATH = "/blocks";
    private static final String USER_PATH = "/users";
    private static final String NEW_DEVICE_PATH = "/devices";
    private static final String EXISTS_PATH = "/exists";
    private static final String CONVERSATION_PATH = "/conversations";
    private static final String PROFILE_PATH = "/profile";

    private static final String REGISTRATION_ID = "registration_id";
    private static final String TYPE = "type";

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
        return "http://" + HOSTNAME + (PORT.equals("") ? "" : ":" + PORT); }
    public static String getBlocksPath() {
        return getRootPath() + BLOCKS_PATH; }
    public static String getNewUserPath() {
        return getRootPath() + USER_PATH; }
    public static String getUserPath(Context context) {
        return getRootPath() + USER_PATH + "/" + getPhoneNumber(context); }
    public static String getProfilePath(Context context) {
        return getUserPath(context) + PROFILE_PATH; }
    public static String getNewDevicePath(Context context) {
        return getUserPath(context) + NEW_DEVICE_PATH; }
    public static String getExistsPath(Context context) {
        return getUserPath(context) + EXISTS_PATH; }
    public static String getConversationPath(String conversationId) {
        return getRootPath() + CONVERSATION_PATH + "/" + conversationId; }

    public static ListeningExecutorService newExecutorService() {
        return MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
    }

    public static ListenableFuture<ArrayList<BlockItem>> getBlocks(
            final ListeningExecutorService ex, Context context) {
        ListenableFuture<HttpResponse> blockResponse = getRequest(ex, getBlocksPath());

        AsyncFunction<HttpResponse, ArrayList<BlockItem>> getBlockItems = (input) -> {
            //get json
            JSONArray array = jsonArrayFromResponse(input);
            //create blocks through json
            ArrayList<BlockItem> blocks = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                blocks.add(new BlockItem(array.getJSONObject(i)));
            }

            return returnListenableFuture(ex, blocks);
        };
        ListenableFuture<ArrayList<BlockItem>> total = Futures.transform(blockResponse, getBlockItems, ex);
        return total;
    }

    public static ListenableFuture<Boolean> getUserExists(
            ListeningExecutorService ex, Context context) {
        return getBooleanValue(ex, getExistsPath(context));
    }

    public static ListenableFuture<Boolean> postUser(
            ListeningExecutorService ex, Context context, String regId) {
        DataStore dataStore = DataStore.getSingletonInstance(context);

        try {
            JSONObject device = new JSONObject();
            device.put(REGISTRATION_ID, regId);
            device.put(TYPE, "android");

            JSONObject ret = new JSONObject();
            ret.put("user", getPhoneNumber(context));
            ret.put("device", device);
            ret.put("key", dataStore.getSymmetricKey());
            ret.put("public_key", dataStore.getPublicKey());

            return postSuccess(ex, getNewUserPath(), ret);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ListenableFuture<Boolean> postRegistrationId(
            ListeningExecutorService ex, Context context, String regId) {
        try {
            JSONObject ret = new JSONObject();
            ret.put(REGISTRATION_ID, regId);
            ret.put(TYPE, "android");
            return postSuccess(ex, getNewDevicePath(context), ret);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static ListenableFuture<Boolean> processRegistrationId(
            final ListeningExecutorService ex, final Context context, final String regId) {
        ListenableFuture<Boolean> userExists = getUserExists(ex, context);
        AsyncFunction<Boolean, Boolean> submitUser = (input) -> {
            if (!input) return postUser(ex, context, regId);
            return postRegistrationId(ex, context, regId);
        };

        ListenableFuture<Boolean> total = Futures.transform(userExists, submitUser, ex);
        return total;
    }

    public static ListenableFuture<Boolean> postData(
            final ListeningExecutorService ex,
            WebView webView, JSONObject fields, JSONObject encryptedPatch, Request request)
            throws JSONException {
        JSONObject patchedFields = new JSONObject();

        Iterator<String> keys = fields.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            patchedFields.put(key, fields.get(key));
        }

        patchedFields.put("patch", encryptedPatch);
        return postPatchedData(ex, patchedFields, request);
    }

    public static ListenableFuture<Boolean> postPatchedData(
            final ListeningExecutorService ex, JSONObject fields, Request request) {
        ListenableFuture<Boolean> result = putSuccess(ex, getConversationPath(request.conversationId), fields);
        return result;
    }

    public static ListenableFuture<JSONObject> getProfile(
            final ListeningExecutorService ex, final Context context) {
        ListenableFuture<HttpResponse> response = getRequest(ex, getProfilePath(context));
        return Futures.transform(response, responseFutureToJSON(ex));
    }

    private static <T> ListenableFuture<T> returnListenableFuture(
            ListeningExecutorService service, final T value) {
        return service.submit(() -> value);
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

    private static boolean requestSucceeded(HttpResponse response) {
        return response.getStatusLine().getStatusCode() == 200;
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

    private static AsyncFunction<HttpResponse, JSONObject> responseFutureToJSON(
            final ListeningExecutorService ex) {
        return (input) -> returnListenableFuture(ex, jsonObjectFromResponse(input));
    }

    private static AsyncFunction<HttpResponse, Boolean> responseFutureToSuccess(
            final ListeningExecutorService ex) {
        return (input) -> returnListenableFuture(ex, requestSucceeded(input));
    }

    private static AsyncFunction<HttpResponse, Boolean> responseFutureToBoolean(
            final ListeningExecutorService ex) {
        return (input) -> returnListenableFuture(ex, booleanFromResponse(input));
    }

    private static ListenableFuture<HttpResponse> response(
            final ListeningExecutorService ex, final HttpUriRequest uriRequest) {
        return ex.submit(() -> {
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
        });
    }

    private static ListenableFuture<HttpResponse> getRequest(
            final ListeningExecutorService ex, final String url) {
        HttpGet httpGet = new HttpGet(url);

        return response(ex, httpGet);
    }

    private static void setJsonEntity(HttpEntityEnclosingRequest httpRequest, final JSONObject data) {
        try {
            StringEntity se = new StringEntity(data.toString());
            se.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
            httpRequest.setEntity(se);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ListenableFuture<HttpResponse> postRequest(
            final ListeningExecutorService ex, final String url, final JSONObject data) {
        HttpPost httpPost = new HttpPost(url);
        setJsonEntity(httpPost, data);

        return response(ex, httpPost);
    }

    private static ListenableFuture<HttpResponse> putRequest(
            final ListeningExecutorService ex, final String url, final JSONObject data) {
        HttpPut httpPut = new HttpPut(url);
        setJsonEntity(httpPut, data);

        return response(ex, httpPut);
    }

    private static ListenableFuture<HttpResponse> patchRequest(
            final ListeningExecutorService ex, final String url, final JSONObject data) {
        //TODO: http patch
    }

    private static ListenableFuture<Boolean> succeeded(
            final ListeningExecutorService ex, ListenableFuture<HttpResponse> response) {
        AsyncFunction<HttpResponse, Boolean> responseToBoolean = responseFutureToSuccess(ex);
        ListenableFuture<Boolean> result = Futures.transform(response, responseToBoolean);
        return result;
    }

    private static ListenableFuture<Boolean> booleanValue(
            final ListeningExecutorService ex, ListenableFuture<HttpResponse> response) {
        AsyncFunction<HttpResponse, Boolean> responseToBoolean = responseFutureToBoolean(ex);
        ListenableFuture<Boolean> result = Futures.transform(response, responseToBoolean);
        return result;
    }

    private static ListenableFuture<Boolean> getBooleanValue(
            final ListeningExecutorService ex, final String url) {
        ListenableFuture<HttpResponse> response = getRequest(ex, url);
        return booleanValue(ex, response);
    }

    private static ListenableFuture<Boolean> postSuccess(
            final ListeningExecutorService ex, final String url, final JSONObject data) {
        ListenableFuture<HttpResponse> response = postRequest(ex, url, data);
        return succeeded(ex, response);
    }

    private static ListenableFuture<Boolean> putSuccess(
            final ListeningExecutorService ex, final String url, final JSONObject data) {
        ListenableFuture<HttpResponse> response = putRequest(ex, url, data);
        return succeeded(ex, response);
    }
}
