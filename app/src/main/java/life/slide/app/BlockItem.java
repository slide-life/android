package life.slide.app;

import android.util.Pair;
import org.json.JSONObject;

import java.util.Set;

/**
 * Created by Michael on 12/22/2014.
 */
public class BlockItem {
    public String blockName;
    public DataStore dataStore;

    public BlockItem(String blockName) {
        this.blockName = blockName;
        this.dataStore = DataStore.getSingletonInstance();
    }

    public BlockItem(JSONObject blockJson) {
        //TODO
    }

    public String getBlockName() {
        return this.blockName;
    }

    public Set<String> getOptions() {
        return dataStore.getBlockOptions(this.blockName);
    }

    public void addOption(String option) {
        dataStore.addOptionToBlock(this.blockName, option);
    }
}
