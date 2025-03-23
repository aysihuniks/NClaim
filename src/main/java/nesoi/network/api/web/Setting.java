package nesoi.network.api.web;

import org.nandayo.DAPI.Util;

import java.util.ArrayList;
import java.util.List;

public class Setting {

    private static final List<Setting> settings = new ArrayList<>();

    public String title;
    public String description;
    public String type;
    public Object value;

    public Setting(String title, String description, String type, Object value) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.value = value;
    }

    public static void add(String title, String description, String type, Object value) {
        settings.add(new Setting(title, description, type, value));
        Util.log("Yeni ayar eklendi: " + title);
    }

    public static List<Setting> getSettings() {
        return settings;
    }

}
