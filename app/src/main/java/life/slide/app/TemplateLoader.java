package life.slide.app;

import android.content.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zeigjeder on 1/13/15.
 */
public class TemplateLoader {
    private static Map<Integer, String> cache;

    public static String loadTemplate(Context context, String template) {
        int hash = template.hashCode();

        if (cache == null) cache = new HashMap<>();
        if (cache.containsKey(hash)) {
            return cache.get(hash);
        }

        String ret = new String(template);

        Pattern p = Pattern.compile("(\\{\\{([^}]+)\\}\\})", Pattern.MULTILINE);
        Matcher m = p.matcher(template);
        if (m.matches()) {
            while (m.find()) {
                MatchResult result = m.toMatchResult();
                String group = m.group(2); //identifier
                int start = result.start(), end = result.end();
                String replacement = "";

                if (group.charAt(0) == '@') {
                    try {
                        replacement = API.readResource(context, Integer.parseInt(group.substring(1)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                ret = ret.substring(0, start) + replacement + ret.substring(end);
            }
        }

        cache.put(hash, ret);
        return ret;
    }

    public static String loadTemplate(Context context, String template, Map<String, String> variables) {
        int hash = template.hashCode() + variables.hashCode();

        if (cache == null) cache = new HashMap<>();
        if (cache.containsKey(hash)) {
            return cache.get(hash);
        }

        String ret = new String(template);

        Pattern p = Pattern.compile("(\\{\\{([^}]+)\\}\\})", Pattern.MULTILINE);
        Matcher m = p.matcher(template);
        if (m.matches()) {
            while (m.find()) {
                MatchResult result = m.toMatchResult();
                String group = m.group(2); //identifier
                int start = result.start(), end = result.end();
                String replacement = "";

                if (group.charAt(0) == '@') {
                    try {
                        replacement = API.readResource(context, Integer.parseInt(group.substring(1)));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (variables.containsKey(group)) {
                        replacement = variables.get(group);
                    } else {
                        replacement = group;
                    }
                }

                ret = ret.substring(0, start) + replacement + ret.substring(end);
            }
        }

        cache.put(hash, ret);
        return ret;
    }
}
