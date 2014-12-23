package life.slide.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Michael on 12/22/2014.
 */
public class Request {
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

            JSONArray blocksJson = object.getJSONArray("blocks");
            ArrayList<String> retBlocks = new ArrayList<String>();
            for (int i = 0; i < blocksJson.length(); i++)
                retBlocks.add(blocksJson.getString(i));

            this.channelId = object.getString("channelId");
            this.name = object.getString("name");
            this.description = object.getString("description");
            this.blocks = retBlocks;
            this.pubKey = object.getString("pubKey");
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
            object.put("channelId", channelId);
            object.put("name", name);
            object.put("description", description);
            object.put("pubKey", pubKey);

            JSONArray blocksJson = new JSONArray(blocks);
            object.put("blocks", blocksJson);

            return object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
