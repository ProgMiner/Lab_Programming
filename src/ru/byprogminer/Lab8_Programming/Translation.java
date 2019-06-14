package ru.byprogminer.Lab8_Programming;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class Translation {

    public final Locale locale;
    public final ResourceBundle resources;
    public final NumberFormat numberFormat;
    public final NumberFormat integerFormat;
    public final NumberFormat currencyFormat;
    public final NumberFormat percentFormat;
    public final DateFormat dateFormat;

    private final Map<String, MessageFormat> messageFormatCache = new HashMap<>();

    public Translation(
            Locale locale,
            ResourceBundle resources,
            NumberFormat numberFormat,
            NumberFormat integerFormat,
            NumberFormat currencyFormat,
            NumberFormat percentFormat,
            DateFormat dateFormat
    ) {
        this.locale = locale;
        this.resources = resources;
        this.numberFormat = numberFormat;
        this.integerFormat = integerFormat;
        this.currencyFormat = currencyFormat;
        this.percentFormat = percentFormat;
        this.dateFormat = dateFormat;
    }

    public MessageFormat getMessageFormat(String key) {
        return messageFormatCache.computeIfAbsent(key, s -> new MessageFormat(resources.getString(s), locale));
    }
}
