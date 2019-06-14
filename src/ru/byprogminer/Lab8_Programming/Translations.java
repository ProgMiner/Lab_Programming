package ru.byprogminer.Lab8_Programming;

import ru.byprogminer.Lab8_Programming.i18n.Lang;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;

public class Translations {

    public static String BASE_RESOURCES_NAME = Lang.class.getName();
    public static int DATE_FORMAT = DateFormat.MEDIUM;
    public static int TIME_FORMAT = DateFormat.LONG;

    public static List<Locale> AVAILABLE_LANGUAGES = new ArrayList<>();

    static {
        AVAILABLE_LANGUAGES.add(new Locale("en", "US", "US"));
        AVAILABLE_LANGUAGES.add(new Locale("ru", "RU", "RU"));
        AVAILABLE_LANGUAGES.add(new Locale("slo", "SK", "SK"));
        AVAILABLE_LANGUAGES.add(new Locale("hun", "HU", "HU"));
        AVAILABLE_LANGUAGES.add(new Locale("spa", "GT", "GT"));
    }

    private static Map<Locale, Translation> translationCache = new HashMap<>();

    public static Translation getTranslation(Locale locale) {
        return translationCache.computeIfAbsent(locale, s -> new Translation(locale,
                ResourceBundle.getBundle(BASE_RESOURCES_NAME, locale),
                NumberFormat.getNumberInstance(locale),
                NumberFormat.getIntegerInstance(locale),
                NumberFormat.getCurrencyInstance(locale),
                NumberFormat.getPercentInstance(locale),
                DateFormat.getDateTimeInstance(DATE_FORMAT, TIME_FORMAT, locale)));
    }
}
