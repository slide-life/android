package life.slide.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Michael on 12/22/2014.
 */
public class Request {
    private final String CHANNEL_ID = "id"; //TODO: update the SharedPrefs xml in the tests
    private final String NAME = "name";
    private final String DESCRIPTION = "description";
    private final String BLOCKS = "blocks";
    private final String PUBLIC_KEY = "key";

    public String channelId;
    public String name;
    public String description;
    public ArrayList<String> blocks;
    public String pubKey;

    public Request(String channelId, String name, String description,
                   ArrayList<String> blocks, String pubKey) {
        this.channelId = channelId;
        this.name = name;
        this.description = description;
        this.blocks = blocks;
        this.pubKey = pubKey;
    }

    public Request(String json) {
        try {
            JSONObject object = new JSONObject(json);

            JSONArray blocksJson = object.getJSONArray(BLOCKS);
            ArrayList<String> retBlocks = new ArrayList<String>();
            for (int i = 0; i < blocksJson.length(); i++)
                retBlocks.add(blocksJson.getString(i)); //TODO: replace with getObject

            this.channelId = object.getString(CHANNEL_ID);
            this.name = object.getString(NAME);
            this.description = object.getString(DESCRIPTION);
            this.blocks = retBlocks;
            this.pubKey = object.getString(PUBLIC_KEY);
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
            object.put(CHANNEL_ID, channelId);
            object.put(NAME, name);
            object.put(DESCRIPTION, description);
            object.put(PUBLIC_KEY, pubKey);

            JSONArray blocksJson = new JSONArray(blocks);
            object.put(BLOCKS, blocksJson);

            return object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
