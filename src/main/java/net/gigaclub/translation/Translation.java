package net.gigaclub.translation;

import net.gigaclub.base.odoo.Odoo;
import org.apache.xmlrpc.XmlRpcException;

import java.util.*;

public class Translation {

    private Odoo odoo;
    private String category;

    public Translation(String hostname, String database, String username, String password) {
        this.odoo = new Odoo(hostname, database, username, password);
        this.category = "";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String t(String name, String playerUUID, List<String> values) {
        try {
            return (String) this.odoo.getModels().execute("execute_kw", Arrays.asList(
                    this.odoo.getDatabase(), this.odoo.getUid(), this.odoo.getPassword(),
                    "gc.translation", "get_translation_by_player_uuid", Arrays.asList(name, playerUUID, values, this.category)
            ));
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
        return name;
    }

    public String t(String name, String playerUUID) {
        try {
            return (String) this.odoo.getModels().execute("execute_kw", Arrays.asList(
                    this.odoo.getDatabase(), this.odoo.getUid(), this.odoo.getPassword(),
                    "gc.translation", "get_translation_by_player_uuid", Arrays.asList(name, playerUUID, new ArrayList<String>(), this.category)
            ));
        } catch (XmlRpcException e) {
            e.printStackTrace();
        }
        return name;
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