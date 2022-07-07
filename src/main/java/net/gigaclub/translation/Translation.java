package net.gigaclub.translation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.gigaclub.base.odoo.Odoo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.apache.commons.text.StringSubstitutor;
import org.apache.xmlrpc.XmlRpcException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class Translation {

    private Odoo odoo;
    private String category;
    private Plugin plugin;

    public Translation(String hostname, String database, String username, String password, Plugin plugin) {
        this.odoo = new Odoo(hostname, database, username, password);
        this.plugin = plugin;
        this.category = "";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    // values rules
    // widgets:
    // - params: {"params": {params1: string, params2: string, ....}}
    // - list: {"list": {list1: array of strings, list2: array of strings, ....}}
    public TextComponent t(String name, Player player, JsonObject values) {
        try {
            String playerUUID = player.getUniqueId().toString();
            Object result = this.odoo.getModels().execute("execute_kw", Arrays.asList(
                    this.odoo.getDatabase(), this.odoo.getUid(), this.odoo.getPassword(),
                    "gc.translation", "get_translation_by_player_uuid", Arrays.asList(name, playerUUID, values.toString(), this.category)
            ));
            try {
                boolean paramsExists = values.has("params");
                boolean listsExists = values.has("list");
                Gson gson = new Gson();
                JsonArray translation = gson.toJsonTree(result).getAsJsonObject().get("values").getAsJsonArray();
                JsonArray translationArray = new JsonArray();
                for (JsonElement t : translation) {
                    if (t instanceof JsonObject) {
                        JsonObject translationObject = t.getAsJsonObject();
                        if (paramsExists) {
                            JsonElement params = values.get("params");
                            HashMap<String, Object> v = new Gson().fromJson(params, HashMap.class);
                            StringSubstitutor sub = new StringSubstitutor(v);
                            if (translationObject.has("text")) {
                                String text = translationObject.get("text").getAsString();
                                text = sub.replace(text);
                                translationObject.addProperty("text", text);
                            }
                            if (translationObject.has("clickEvent")) {
                                JsonObject clickEvent = translationObject.get("clickEvent").getAsJsonObject();
                                if (clickEvent.has("value")) {
                                    String value = clickEvent.get("value").getAsString();
                                    value = sub.replace(value);
                                    clickEvent.addProperty("value", value);
                                }
                            }
                            if (translationObject.has("hoverEvent")) {
                                JsonObject hoverEvent = translationObject.get("hoverEvent").getAsJsonObject();
                                if (hoverEvent.get("action").getAsString().equals("show_text")) {
                                    JsonArray value = hoverEvent.get("value").getAsJsonArray();
                                    for (JsonElement vv : value) {
                                        if (vv instanceof JsonObject) {
                                            JsonObject vvv = vv.getAsJsonObject();
                                            if (vvv.has("text")) {
                                                String text = vvv.get("text").getAsString();
                                                text = sub.replace(text);
                                                vvv.addProperty("text", text);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (listsExists) {
                            if (translationObject.has("type")) {
                                if (translationObject.get("type").getAsString().equals("list")) {
                                    JsonObject lists = values.get("list").getAsJsonObject();
                                    for(Map.Entry<String, JsonElement> entry : lists.entrySet()) {
                                        if (translationObject.get("widget").getAsString().equals(entry.getKey())) {
                                            JsonArray items = entry.getValue().getAsJsonArray();
                                            JsonArray valueArray = translationObject.get("values").getAsJsonArray();
                                            for (JsonElement item : items) {
                                                String itemStr = item.getAsString();
                                                for (JsonElement value : valueArray) {
                                                    if (value instanceof JsonObject) {
                                                        JsonObject valueObject = value.getAsJsonObject();
                                                        if (valueObject.has("listitem") && valueObject.get("listitem").getAsBoolean()) {
                                                            if (valueObject.has("text")) {
                                                                valueObject.addProperty("text", itemStr);
                                                            }
                                                        }
                                                        translationArray.add(valueObject.deepCopy());
                                                    } else {
                                                        translationArray.add(value);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!translationObject.has("type")) {
                            translationArray.add(translationObject);
                        }
                    } else {
                        translationArray.add(t);
                    }
                }
                return (TextComponent) GsonComponentSerializer.gson().deserializeFromTree(translationArray);
            } catch (IllegalStateException e) {
                return Component.text(result.toString());
            }
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
        return null;
    }

    public TextComponent t(String name, Player player) {
        try {
            String playerUUID = player.getUniqueId().toString();
            Object result = this.odoo.getModels().execute("execute_kw", Arrays.asList(
                    this.odoo.getDatabase(), this.odoo.getUid(), this.odoo.getPassword(),
                    "gc.translation", "get_translation_by_player_uuid", Arrays.asList(name, playerUUID, new JsonObject().toString(), this.category)
            ));
            try {
                Gson gson = new Gson();
                JsonElement translation = gson.toJsonTree(result).getAsJsonObject().get("values");
                return (TextComponent) GsonComponentSerializer.gson().deserializeFromTree(translation);
            } catch (IllegalStateException e) {
                return Component.text(result.toString());
            }
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendMessage(String name, Player player, JsonObject values) {
        player.sendMessage(t(name, player, values));
    }

    public void sendMessage(String name, Player player) {
        player.sendMessage(t(name, player));
    }

    public boolean checkIfTranslationExists(String translationName) {
        return this.odoo.search_count(
                "gc.translation",
                Arrays.asList(
                        Arrays.asList(
                                Arrays.asList("name", "=", translationName)
                        )
                )
        ) > 0;
    }

    public void registerTranslation(String translationName) {
        if (!this.checkIfTranslationExists(translationName)) {
            String category = this.category;
            this.odoo.create(
                    "gc.translation",
                    Arrays.asList(
                            new HashMap() {{
                                put("name", translationName);
                                put("category", category);
                            }}
                    )
            );
        }
    }

    public void registerTranslations(List<String> translationNames) {
        for(String translationName : translationNames) {
            this.registerTranslation(translationName);
        }
    }

}