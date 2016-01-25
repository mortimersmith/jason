package com.github.mortimersmith.jason;

import com.github.mortimersmith.utils.Utils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.tools.ToolProvider;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CompilerTest
{
    @Test
    public void test() throws Exception
    {
        String data = Utils.read(CompilerTest.class.getResourceAsStream("/example.json"), StandardCharsets.UTF_8);
        JsonObject json = new JsonParser().parse(data).getAsJsonObject();
        Path pathSource = Paths.get("build/output/test/source");
        Path pathClasses = Paths.get("build/output/test/classes");
        Compiler.compileJson(json, pathSource);
        pathClasses.toFile().mkdirs();
        int code = ToolProvider.getSystemJavaCompiler().run
            ( null, System.out, System.err
            , "-cp"
            , System.getProperty("java.class.path")
            , "-d"
            , pathClasses.toString()
            , pathSource.resolve(Paths.get("com/github/mortimersmith/jason/Examples.java")).toString()
            );
        assertEquals(0, code);
    }
}
