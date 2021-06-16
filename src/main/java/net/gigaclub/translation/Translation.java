package net.gigaclub.translation;

import net.gigaclub.base.odoo.Odoo;

import java.util.*;

public class Translation {

    private Odoo odoo;

    public Translation(String hostname, String database, String username, String password) {
        this.odoo = new Odoo(hostname, database, username, password);
    }

    public int getTranslation(String name) {
        return (int) this.odoo.search(
                "gc.translation",
                Arrays.asList(
                        Arrays.asList(
                                Arrays.asList("name", "=", name)
                        )
                ),
                new HashMap() {{ put("limit", 1); }}
        ).get(0);
    }

    // Todo
    // improve function later
    public String t(String name, ArrayList<String> values, String playerUUID) {
        if (this.odoo.search_count("gc.translation",
                Arrays.asList(
                        Arrays.asList(
                                Arrays.asList("name","=",name)
                        )
                )
        ) == 0) {
            this.odoo.create("gc.translation",
                    Arrays.asList(
                            new HashMap() {{ put("name", name); put("var_count", values.size()); }}
                    )
            );
        } else {
            this.odoo.write(
                    "gc.translation",
                    Arrays.asList(
                            Arrays.asList(this.getTranslation(name)),
                            new HashMap() {{ put("var_count", values.size()); }}
                    )
            );
        }
        HashMap<Object, Object> language = (HashMap<Object, Object>) this.odoo.search_read("gc.user",
                Arrays.asList(
                        Arrays.asList(
                                Arrays.asList("mc_uuid","=",playerUUID)
                        )
                ),
                new HashMap() {{ put("fields", Arrays.asList("language_id")); put("limit", 1); }}
        ).get(0);
        HashMap<Object, Object> translation = (HashMap<Object, Object>) this.odoo.search_read("gc.translation",
                Arrays.asList(
                        Arrays.asList(
                                Arrays.asList("name","=",name)
                        )
                ),
                new HashMap() {{ put("fields", Arrays.asList("id")); put("limit", 1); }}
        ).get(0);
        Object entry = this.odoo.search_read("gc.translation.entry",
                Arrays.asList(
                        Arrays.asList(
                                Arrays.asList("language_id","=",language.get("id")),
                                Arrays.asList("translation_id","=",translation.get("id"))
                        )
                ),
                new HashMap() {{ put("fields", Arrays.asList("content")); put("limit", 1); }}
        );
        if (entry != null) {
            List<Object> a = Collections.singletonList(entry);
            if (a.size() > 0) {
                List<Object> b = (List<Object>) a.get(0);
                HashMap<Object, Object> c = (HashMap<Object, Object>) b.get(0);
                String rawString = (String) c.get("content");
                System.out.println(rawString);
                return String.format(rawString, values.toArray(new String[0]));
            }
        }
        return name;
    }

}