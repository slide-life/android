package life.slide.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Michael on 12/22/2014.
 */
public class Request {
    private final String CONVERSATION = "conversation";
    private final String CONVERSATION_ID = "id"; //TODO: update the SharedPrefs xml in the tests
    private final String NAME = "name";
    private final String DESCRIPTION = "description";
    private final String BLOCKS = "blocks";
    private final String PUBLIC_KEY = "key";

    public String conversationId; //actually conversation id
    public String name;
    public String description;
    public ArrayList<String> blocks;
    public String pubKey;

    public Request(String conversationId, String name, String description,
                   ArrayList<String> blocks, String pubKey) {
        this.conversationId = conversationId;
        this.name = name;
        this.description = description;
        this.blocks = blocks;
        this.pubKey = pubKey;
    }

    public Request(String json) {
        try {
            JSONObject object = new JSONObject(json);

            JSONArray blocksJson = object.getJSONArray(BLOCKS);
            ArrayList<String> retBlocks = new ArrayList<>();
            for (int i = 0; i < blocksJson.length(); i++)
                retBlocks.add(blocksJson.getString(i)); //TODO: replace with getObject

            JSONObject conversationJson = object.getJSONObject(CONVERSATION);

            this.conversationId = conversationJson.getString(CONVERSATION_ID);
            this.name = conversationJson.getString(NAME);
            this.description = conversationJson.getString(DESCRIPTION);
            this.blocks = retBlocks;
            this.pubKey = conversationJson.getString(PUBLIC_KEY);
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
            JSONObject conversationJson = new JSONObject();
            conversationJson.put(CONVERSATION_ID, conversationId);
            conversationJson.put(NAME, name);
            conversationJson.put(DESCRIPTION, description);
            conversationJson.put(PUBLIC_KEY, pubKey);
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
