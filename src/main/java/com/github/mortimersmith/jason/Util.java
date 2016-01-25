package com.github.mortimersmith.jason;

import static com.github.mortimersmith.utils.Utils.split;
import static com.github.mortimersmith.utils.Utils.splitE;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.function.BiConsumer;

public class Util
{
    public static <E extends Exception> void forEach(JsonObject json, BiConsumer<String, JsonElement> c)
    {
        for (Map.Entry<String, JsonElement> e : json.entrySet()) split(c).accept(e);
    }

    public static <E extends Exception> void forEachE(JsonObject json, com.github.mortimersmith.utils.Utils.ThrowingBiConsumer<String, JsonElement, E> c) throws E
    {
        for (Map.Entry<String, JsonElement> e : json.entrySet()) splitE(c).accept(e);
    }
}
