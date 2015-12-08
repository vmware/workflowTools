package com.vmware.rest.json;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateWithTimezoneMapper implements JsonDeserializer<Date> {

    private SimpleDateFormat formatter;
    private TimeZone serverTimezone;

    public DateWithTimezoneMapper(String dateFormat, TimeZone serverTimezone) {
        this.serverTimezone = serverTimezone;
        formatter = new SimpleDateFormat(dateFormat);
        formatter.setTimeZone(serverTimezone);
    }

    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!(json instanceof JsonPrimitive)) {
            throw new JsonParseException("The date should be a string value");
        }
        String dateText = json.getAsString();
        try {
            if (dateText.endsWith("Z")) {
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                dateText = dateText.substring(0, dateText.lastIndexOf("Z"));
            }
            Date serverDate = formatter.parse(dateText);
            return serverDate;
        } catch (ParseException e) {
            throw new JsonParseException(String.format("Failed to parse date %s with pattern %s", dateText, formatter.toPattern()), e);
        } finally {
            formatter.setTimeZone(serverTimezone);
        }
    }
}
