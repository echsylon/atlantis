package com.echsylon.atlantis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.isEmpty;
import static com.echsylon.atlantis.Utils.notAnyEmpty;
import static com.echsylon.atlantis.Utils.notEmpty;
import static com.echsylon.atlantis.Utils.parseBoolean;
import static com.echsylon.atlantis.Utils.parseInt;
import static com.echsylon.atlantis.Utils.parseLong;

/**
 * This class holds user configured server behavior. These settings are not
 * related to the HTTP request/response protocol but rather reflect mocked
 * physical characteristics of a remote server.
 */
@SuppressWarnings("WeakerAccess")
public class SettingsManager {
    public static final String FOLLOW_REDIRECTS = "followRedirects";
    public static final String FALLBACK_BASE_URL = "fallbackBaseUrl";
    public static final String THROTTLE_BYTE_COUNT = "throttleByteCount";
    public static final String THROTTLE_MIN_DELAY_MILLIS = "throttleMinDelayMillis";
    public static final String THROTTLE_MAX_DELAY_MILLIS = "throttleMaxDelayMillis";
    public static final String TOKEN_HELPER = "tokenHelper";
    public static final String TRANSFORMATION_HELPER = "transformationHelper";
    public static final String REQUEST_FILTER = "requestFilter";
    public static final String RESPONSE_FILTER = "responseFilter";

    private transient final Map<String, String> settings = new LinkedHashMap<>();

    /**
     * Sets the given setting value in the internal collection, overwriting any
     * existing value for it.
     *
     * @param key   The setting key.
     * @param value The corresponding value.
     */
    void set(String key, String value) {
        if (notAnyEmpty(key, value))
            settings.put(key, value);
    }

    /**
     * Sets the given setting value in the internal collection, but only if
     * there isn't already a value with the same key present.
     *
     * @param key   The setting key.
     * @param value The corresponding value.
     */
    void setIfAbsent(String key, String value) {
        if (!settings.containsKey(key))
            set(key, value);
    }

    /**
     * Sets the given settings values, but only those keys which doesn't already
     * have a value present in the internal collection.
     *
     * @param settings The new settings key/value pairs.
     */
    void setIfAbsent(final Map<String, String> settings) {
        if (notEmpty(settings))
            for (Map.Entry<String, String> entry : settings.entrySet())
                setIfAbsent(entry.getKey(), entry.getValue());
    }

    /**
     * Sets all settings in the internal collection, overwriting any existing
     * keys.
     *
     * @param settings The new settings.
     */
    void set(final Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet())
            set(entry.getKey(), entry.getValue());
    }

    /**
     * Returns all settings keys and values (expressed as strings) as an
     * immutable map.
     *
     * @return The map of all settings. May be empty but never null.
     */
    Map<String, String> getAllAsMap() {
        return Collections.unmodifiableMap(settings);
    }

    /**
     * Returns the number of unique settings keys managed by this settings
     * manager.
     *
     * @return The number of added settings keys.
     */
    int entryCount() {
        return settings.size();
    }

    /**
     * Returns a flag telling whether a response should automatically follow any
     * redirects or not.
     *
     * @return Boolean true if a redirect should be followed, false otherwise.
     */
    boolean followRedirects() {
        return parseBoolean(get(FOLLOW_REDIRECTS), true);
    }

    /**
     * Returns the number of bytes from the response body to send at a time as a
     * "package".
     *
     * @return The desired response body package size.
     */
    long throttleByteCount() {
        return parseLong(get(THROTTLE_BYTE_COUNT), Long.MAX_VALUE);
    }

    /**
     * Returns a random amount of milliseconds for each time this method is
     * called. The delay will be between {@code throttleMinDelayMillis} and
     * {@code throttleMaxDelayMillis} and never less than zero.
     *
     * @return A random delay in milliseconds.
     */
    long throttleDelayMillis() {
        int min = Math.max(0, parseInt(get(THROTTLE_MIN_DELAY_MILLIS), 0));
        int max = Math.max(0, parseInt(get(THROTTLE_MAX_DELAY_MILLIS), 0));

        return max > min ?
                new Random().nextInt((max - min)) + min :
                min;
    }

    /**
     * Returns the fallback base url for the entity holding this manager. If
     * given and Atlantis is configured to fall back to real world responses,
     * this will be a suggested real world base URL candidate (replacing the
     * mock "localhost").
     *
     * @return The fallback base url or null.
     */
    String fallbackBaseUrl() {
        return settings.get(FALLBACK_BASE_URL);
    }

    /**
     * Tries to return a token helper instance from the configured absolute
     * class name.
     *
     * @return A token helper instance or null if couldn't instantiate one.
     */
    Atlantis.TokenHelper tokenHelper() {
        String className = get(TOKEN_HELPER);
        if (isEmpty(className))
            return null;

        try {
            return (Atlantis.TokenHelper) Class.forName(className).newInstance();
        } catch (Exception e) {
            info(e, "Couldn't deserialize token helper");
            return null;
        }
    }

    /**
     * Tries to return a transformation helper instance from the configured
     * absolute class name.
     *
     * @return A transformation helper instance or null if couldn't instantiate
     * one.
     */
    Atlantis.TransformationHelper transformationHelper() {
        String className = get(TRANSFORMATION_HELPER);
        if (isEmpty(className))
            return null;

        try {
            return (Atlantis.TransformationHelper) Class.forName(className).newInstance();
        } catch (Exception e) {
            info(e, "Couldn't deserialize transformation helper");
            return null;
        }
    }

    /**
     * Tries to return a request filter instance from the configured absolute
     * class name.
     *
     * @return A request filter instance or null if couldn't instantiate one.
     */
    MockRequest.Filter requestFilter() {
        String className = get(REQUEST_FILTER);
        if (isEmpty(className))
            return null;

        try {
            return (MockRequest.Filter) Class.forName(className).newInstance();
        } catch (Exception e) {
            info(e, "Couldn't deserialize request filter");
            return null;
        }
    }

    /**
     * Tries to return a response filter instance from the configured absolute
     * class name.
     *
     * @return A response filter instance or null if couldn't instantiate one.
     */
    MockResponse.Filter responseFilter() {
        String className = get(RESPONSE_FILTER);
        if (isEmpty(className))
            return null;

        try {
            return (MockResponse.Filter) Class.forName(className).newInstance();
        } catch (Exception e) {
            info(e, "Couldn't deserialize response filter");
            return null;
        }
    }

    /**
     * Returns any custom setting with the provided key.
     *
     * @param key The key of the setting to fetch.
     * @return The settings value or null if the provided key doesn't exist.
     */
    String get(String key) {
        return settings.get(key);
    }
}
