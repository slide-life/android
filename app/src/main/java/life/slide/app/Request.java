package life.slide.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Created by Michael on 12/22/2014.
 */
public class Request {
    private static final String CONVERSATION = "conversation";
    private static final String CONVERSATION_ID = "id"; //TODO: update the SharedPrefs xml in the tests
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String KEY = "key";

    private static final String BLOCKS = "blocks";
    private static final String FIELDS = "fields";

    private static final String VERB = "verb";

    public static final String REQUEST = "verb_request";
    public static final String DEPOSIT = "verb_deposit";
    public static final String FORCE_DEPOSIT = "verb_force_deposit";

    public String conversationId; //actually conversation id
    public String name;
    public String description;
    public ArrayList<String> blocks;
    public Map<String, Set<String>> fields;
    public String key;
    public String verb;

    public Request(String json) {
        try {
            JSONObject object = new JSONObject(json);
            this.verb = object.getString(VERB);

            if (this.verb.equals(REQUEST)) {
                JSONArray blocksJson = object.getJSONArray(BLOCKS);
                ArrayList<String> retBlocks = new ArrayList<>();
                for (int i = 0; i < blocksJson.length(); i++)
                    retBlocks.add(blocksJson.getString(i)); //TODO: replace with getObject
                this.blocks = retBlocks;
            } else if (this.verb.equals(DEPOSIT) ||
                       this.verb.equals(FORCE_DEPOSIT)) {
                JSONObject fieldsJson = object.getJSONObject(FIELDS);
                this.fields = Javascript.jsonObjectToProfile(fieldsJson);
            }

            JSONObject conversationJson = object.getJSONObject(CONVERSATION);
            this.conversationId = conversationJson.getString(CONVERSATION_ID);
            this.name = conversationJson.getString(NAME);
            this.description = conversationJson.getString(DESCRIPTION);
            this.key = conversationJson.getString(KEY);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String toJson() {
        JSONObject object = new JSONObject();

        try {
            object.put(VERB, verb);

            JSONObject conversationJson = new JSONObject();
            conversationJson.put(CONVERSATION_ID, conversationId);
            conversationJson.put(NAME, name);
            conversationJson.put(DESCRIPTION, description);
            conversationJson.put(KEY, key);
            object.put(CONVERSATION, conversationJson);

            JSONArray blocksJson = new JSONArray(blocks);
            object.put(BLOCKS, blocksJson);

            return object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
