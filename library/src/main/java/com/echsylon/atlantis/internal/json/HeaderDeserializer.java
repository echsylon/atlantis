package com.echsylon.atlantis.internal.json;

import com.echsylon.atlantis.template.Request;
import com.echsylon.atlantis.template.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * This class is responsible for parsing header attributes of {@link Request} and {@link Response}
 * JSON objects. The headers can either be expressed as an '\n' separated string, which further more
 * separates the keys from the values by a ':', or they can be expressed as a JSON array of objects
 * with "key" and "value" attributes.
 */
class HeaderDeserializer implements JsonDeserializer<Object> {
    // This class' only purpose is to offer an alternative representation of a Request class. This
    // is needed during deserialization in order to prevent eternal loops, while we have a custom
    // parsing implementation of "headers" fields - which could be both '\n' separated strings as
    // well as key/value pair object arrays.
    private static final class InternalRequest extends Request {
    }

    // Same as InternalRequest but for the Response class.
    private static final class InternalResponse extends Response {
    }

    static HeaderDeserializer newRequestHeaderDeserializer() {
        return new HeaderDeserializer(InternalRequest.class);
    }

    static HeaderDeserializer newResponseHeaderDeserializer() {
        return new HeaderDeserializer(InternalResponse.class);
    }

    // An internal type representation of an object containing HTTP header data. This is field is
    // necessary in order to prevent eternal loops due to Gson implementation technical reasons.
    // See InternalRequest and InternalResponse classes above and the official Gson deserialize()
    // method documentation for more details.
    private final Type internalType;

    // Intentionally hidden constructor.
    private HeaderDeserializer(Type internalType) {
        this.internalType = internalType;
    }

    @Override
    public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject requestJsonObject = json.getAsJsonObject();
        JsonElement headers = requestJsonObject.get("headers");

        if (headers != null && headers.isJsonPrimitive()) {
            String headerString = headers.getAsString();
            JsonArray headersJsonArray = splitHeaders(headerString);
            requestJsonObject.add("headers", headersJsonArray);
        }

        return context.deserialize(requestJsonObject, internalType);
    }

    // Splits a header string into an array of json objects.
    private JsonArray splitHeaders(String headerString) {
        JsonArray jsonArray = new JsonArray();
        String[] splitHeaders = headerString.split("\n");

        for (String header : splitHeaders) {
            int firstIndex = header.indexOf(':');
            if (firstIndex != -1) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("key", header.substring(0, firstIndex).trim());
                jsonObject.addProperty("value", header.substring(firstIndex + 1).trim());
                jsonArray.add(jsonObject);
            }
        }

        return jsonArray;
    }

}
