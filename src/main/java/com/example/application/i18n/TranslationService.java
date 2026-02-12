package com.example.application.i18n;

import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UI-scoped translation service supporting live language switching (EN / DE).
 * Default language is English.
 */
@Service
@UIScope
public class TranslationService {

    private String currentLanguage = "en";
    private final List<Runnable> listeners = new ArrayList<>();

    private static final Map<String, Map<String, String>> TRANSLATIONS = new LinkedHashMap<>();

    static {
        // ── English (default) ──────────────────────────────────────────────
        Map<String, String> en = new LinkedHashMap<>();
        en.put("control.panel.title",    "Control Panel");
        en.put("ships.title",           "Ships");
        en.put("error.unknown",         "Unknown error");
        en.put("ship.launched",         "Ship \"{0}\" successfully launched!");
        en.put("dialog.new.ship",       "New Ship");
        en.put("field.name",            "Name");
        en.put("field.name.placeholder", "e.g. Boat 1");
        en.put("field.x.coordinate",    "X Coordinate");
        en.put("field.x.placeholder",   "e.g. 20");
        en.put("field.y.coordinate",    "Y Coordinate");
        en.put("field.y.placeholder",   "e.g. 20");
        en.put("field.direction",       "Direction");
        en.put("button.add",            "Add");
        en.put("button.cancel",         "Cancel");
        en.put("button.launch",         "LAUNCH");
        en.put("button.radar",          "RADAR");
        en.put("button.scan",           "SCAN");
        en.put("button.exit",           "EXIT");
        en.put("ship.info.sector",      "sector");
        en.put("ship.info.direction",   "direction");
        TRANSLATIONS.put("en", en);

        // ── Deutsch ────────────────────────────────────────────────────────
        Map<String, String> de = new LinkedHashMap<>();
        de.put("control.panel.title",    "Kontrollfeld");
        de.put("ships.title",           "Schiffe");
        de.put("error.unknown",         "Unbekannter Fehler");
        de.put("ship.launched",         "Schiff \u00BB{0}\u00AB erfolgreich gestartet!");
        de.put("dialog.new.ship",       "Neues Schiff");
        de.put("field.name",            "Name");
        de.put("field.name.placeholder", "z.B. Boot 1");
        de.put("field.x.coordinate",    "X-Koordinate");
        de.put("field.x.placeholder",   "z. B. 20");
        de.put("field.y.coordinate",    "Y-Koordinate");
        de.put("field.y.placeholder",   "z. B. 20");
        de.put("field.direction",       "Richtung");
        de.put("button.add",            "Hinzuf\u00FCgen");
        de.put("button.cancel",         "Abbrechen");
        de.put("button.launch",         "STARTEN");
        de.put("button.radar",          "RADAR");
        de.put("button.scan",           "SCAN");
        de.put("button.exit",           "BEENDEN");
        de.put("ship.info.sector",      "Sektor");
        de.put("ship.info.direction",   "Richtung");
        TRANSLATIONS.put("de", de);
    }

    /** Get a simple translation for the current language. */
    public String get(String key) {
        Map<String, String> messages = TRANSLATIONS.getOrDefault(currentLanguage, TRANSLATIONS.get("en"));
        return messages.getOrDefault(key, key);
    }

    /** Get a parameterized translation (uses {@link MessageFormat}). */
    public String get(String key, Object... params) {
        String pattern = get(key);
        return MessageFormat.format(pattern, params);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /** Set the active language and notify all registered listeners. */
    public void setLanguage(String language) {
        this.currentLanguage = language;
        listeners.forEach(Runnable::run);
    }

    /** Toggle between EN and DE. */
    public void toggleLanguage() {
        if ("en".equals(currentLanguage)) {
            setLanguage("de");
        } else {
            setLanguage("en");
        }
    }

    public void addLanguageChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeLanguageChangeListener(Runnable listener) {
        listeners.remove(listener);
    }
}
