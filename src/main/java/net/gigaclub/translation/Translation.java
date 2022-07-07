package net.gigaclub.translation;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.gigaclub.base.odoo.Odoo;
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
    public void sendMessage(String name, Player player, JsonObject values) {
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

                JsonElement translation = gson.toJsonTree(result).getAsJsonObject().get("values");
                for (JsonElement t : translation.getAsJsonArray()) {
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
                    }
                }
                TextComponent message = (TextComponent) GsonComponentSerializer.gson().deserializeFromTree(translation);
                player.sendMessage(message);
            } catch (IllegalStateException e) {
                String translationString = result.toString();
                player.sendMessage(translationString);
            }
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String name, Player player) {
        try {
            String playerUUID = player.getUniqueId().toString();
            Object result = this.odoo.getModels().execute("execute_kw", Arrays.asList(
                    this.odoo.getDatabase(), this.odoo.getUid(), this.odoo.getPassword(),
                    "gc.translation", "get_translation_by_player_uuid", Arrays.asList(name, playerUUID, new JsonObject().toString(), this.category)
            ));
            try {
                Gson gson = new Gson();
                JsonElement translation = gson.toJsonTree(result).getAsJsonObject().get("values");
                TextComponent message = (TextComponent) GsonComponentSerializer.gson().deserializeFromTree(translation);
                player.sendMessage(message);
            } catch (IllegalStateException e) {
                String translationString = result.toString();
                player.sendMessage(translationString);
            }
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
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