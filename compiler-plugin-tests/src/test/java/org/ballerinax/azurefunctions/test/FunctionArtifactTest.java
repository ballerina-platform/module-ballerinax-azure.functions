/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.ballerinax.azurefunctions.test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ballerina.projects.CodeModifierResult;
import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.directory.BuildProject;
import org.ballerinax.azurefunctions.AzureFunctionServiceExtractor;
import org.ballerinax.azurefunctions.FunctionContext;
import org.ballerinax.azurefunctions.service.Binding;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases to generated function.json in different cases.
 * 
 * @since 2201.3.0
 */
public class FunctionArtifactTest {

    public static final Path RESOURCE_DIRECTORY = Paths.get("src/test/resources/handlers/");

    private JsonParser jsonParser = new JsonParser();
    private Map<String, JsonObject> generatedFunctions = new HashMap<>();

    @BeforeClass
    public void compileSample() {
        BuildProject project = BuildProject.load(RESOURCE_DIRECTORY);
        CodeModifierResult codeModifierResult = project.currentPackage().runCodeModifierPlugins();
        Package updatedPackage = codeModifierResult.updatedPackage().orElseThrow();
        PackageCompilation compilation = updatedPackage.getCompilation();

        AzureFunctionServiceExtractor azureFunctionServiceExtractor =
                new AzureFunctionServiceExtractor(updatedPackage);
        List<FunctionContext> functionContexts = azureFunctionServiceExtractor.extractFunctions();

        for (FunctionContext ctx : functionContexts) {
            JsonObject functions = new JsonObject();
            JsonArray bindings = new JsonArray();
            List<Binding> bindingList = ctx.getBindingList();
            for (Binding binding : bindingList) {
                bindings.add(binding.getJsonObject());
            }
            functions.add("bindings", bindings);
            generatedFunctions.put(ctx.getFunctionName(), functions);
        }

        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertFalse(diagnosticResult.hasErrors());
        Assert.assertEquals(generatedFunctions.size(), 24);
    }

    @Test
    public void testOptionalHttp() {
        JsonObject httpHello = generatedFunctions.get("post-hello-optional");
        String str =
                "{\"bindings\":[{\"type\":\"httpTrigger\",\"authLevel\":\"anonymous\",\"methods\":[\"post\"]," +
                        "\"direction\":\"in\",\"name\":\"httpPayload\",\"route\":\"hello/optional\"}," +
                        "{\"type\":\"http\",\"direction\":\"out\",\"name\":\"resp\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(httpHello, parse);
    }

    @Test
    public void testHttpTriggerInlineListener() {
        JsonObject httpHello = generatedFunctions.get("post-helo-hello-query");
        String str =
                "{\"bindings\":[{\"type\":\"httpTrigger\",\"authLevel\":\"anonymous\",\"methods\":[\"post\"]," +
                        "\"direction\":\"in\",\"name\":\"httpPayload\",\"route\":\"helo/hello-query\"}," +
                        "{\"type\":\"http\",\"direction\":\"out\",\"name\":\"resp\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(httpHello, parse);
    }

    @Test
    public void testHttpHello() {
        JsonObject httpHello = generatedFunctions.get("post-hello");
        String str =
                "{\"bindings\":[{\"type\":\"httpTrigger\",\"authLevel\":\"anonymous\",\"methods\":[\"post\"]," +
                        "\"direction\":\"in\",\"name\":\"httpPayload\",\"route\":\"hello\"}," +
                        "{\"type\":\"http\",\"direction\":\"out\",\"name\":\"resp\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(httpHello, parse);
    }

    @Test
    public void testHttpDefault() {
        JsonObject httpHello = generatedFunctions.get("default-hello-all");
        String str = "{\"bindings\":[{\"type\":\"httpTrigger\",\"authLevel\":\"anonymous\",\"methods\":[\"DELETE\"," +
                "\"GET\",\"HEAD\",\"OPTIONS\",\"POST\",\"PUT\"],\"direction\":\"in\",\"name\":\"httpPayload\"," +
                "\"route\":\"hello/all\"},{\"type\":\"http\",\"direction\":\"out\",\"name\":\"resp\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(httpHello, parse);
    }

    @Test
    public void testHttpHelloFoo() {
        JsonObject actual = generatedFunctions.get("post-hello-foo");
        String str =
                "{\"bindings\":[{\"type\":\"httpTrigger\",\"authLevel\":\"anonymous\",\"methods\":[\"post\"]," +
                        "\"direction\":\"in\",\"name\":\"httpPayload\",\"route\":\"hello/foo\"},{\"type\":\"http\"," +
                        "\"direction\":\"out\",\"name\":\"resp\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testHttpHelloFooParam() {
        JsonObject actual = generatedFunctions.get("post-hello-foo-bar-1");
        String str =
                "{\"bindings\":[{\"type\":\"httpTrigger\",\"authLevel\":\"anonymous\",\"methods\":[\"post\"]," +
                        "\"direction\":\"in\",\"name\":\"httpPayload\",\"route\":\"hello/foo/{bar}\"}," +
                        "{\"type\":\"http\",\"direction\":\"out\",\"name\":\"resp\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testHttpHelloFooConflictPathParam() {
        JsonObject actual = generatedFunctions.get("post-hello-foo-bar-2");
        String str =
                "{\"bindings\":[{\"type\":\"httpTrigger\",\"authLevel\":\"anonymous\",\"methods\":[\"post\"]," +
                        "\"direction\":\"in\",\"name\":\"httpPayload\",\"route\":\"hello/foo/bar\"},{\"type\":" +
                        "\"http\",\"direction\":\"out\",\"name\":\"resp\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testQueueTrigger() {
        JsonObject actual = generatedFunctions.get("queue");
        String str =
                "{\"bindings\":[{\"type\":\"queueTrigger\",\"connection\":\"AzureWebJobsStorage\"," +
                        "\"queueName\":\"queue2\",\"direction\":\"in\",\"name\":\"inMsg\"},{\"type\":\"queue\"," +
                        "\"connection\":\"AzureWebJobsStorage\",\"queueName\":\"queue3\",\"direction\":\"out\"," +
                        "\"name\":\"outMsg\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testQueueTriggerInlineListener() {
        JsonObject actual = generatedFunctions.get("queue1");
        String str =
                "{\"bindings\":[{\"type\":\"queueTrigger\",\"connection\":\"AzureWebJobsStorage\"," +
                        "\"queueName\":\"queue21\",\"direction\":\"in\",\"name\":\"inMsg\"},{\"type\":\"queue\"," +
                        "\"connection\":\"AzureWebJobsStorage\",\"queueName\":\"queue3\",\"direction\":\"out\"," +
                        "\"name\":\"outMsg\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testCosmosTrigger() {
        JsonObject actual = generatedFunctions.get("cosmos");
        String str =
                "{\"bindings\":[{\"type\":\"cosmosDBTrigger\",\"connectionStringSetting\":\"CosmosDBConnection\"," +
                        "\"databaseName\":\"db1\",\"collectionName\":\"c2\",\"name\":\"inMsg\",\"direction\":\"in\"," +
                        "\"createLeaseCollectionIfNotExists\":true,\"leasesCollectionThroughput\":400}," +
                        "{\"type\":\"queue\",\"connection\":\"AzureWebJobsStorage\",\"queueName\":\"queue3\"," +
                        "\"direction\":\"out\",\"name\":\"outMsg\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testCosmosTriggerInlineListener() {
        JsonObject actual = generatedFunctions.get("cosmos1");
        String str =
                "{\"bindings\":[{\"type\":\"cosmosDBTrigger\",\"connectionStringSetting\":\"CosmosDBConnection\"," +
                        "\"databaseName\":\"db1\",\"collectionName\":\"c2\",\"name\":\"inMsg\",\"direction\":\"in\"," +
                        "\"createLeaseCollectionIfNotExists\":true,\"leasesCollectionThroughput\":400}," +
                        "{\"type\":\"queue\",\"connection\":\"AzureWebJobsStorage\",\"queueName\":\"queue3\"," +
                        "\"direction\":\"out\",\"name\":\"outMsg\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testTimerTrigger() {
        JsonObject actual = generatedFunctions.get("timer");
        String str = "{\"bindings\":[{\"type\":\"timerTrigger\",\"schedule\":\"*/10 * * * * *\"," +
                "\"runOnStartup\":true,\"direction\":\"in\",\"name\":\"inMsg\"},{\"type\":\"queue\",\"connection\":" +
                "\"AzureWebJobsStorage\",\"queueName\":\"queue3\",\"direction\":\"out\",\"name\":\"outMsg\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testTimerTriggerInlineListener() {
        JsonObject actual = generatedFunctions.get("timer1");
        String str = "{\"bindings\":[{\"type\":\"timerTrigger\",\"schedule\":\"*/10 * * * * *\"," +
                "\"runOnStartup\":true,\"direction\":\"in\",\"name\":\"inMsg\"},{\"type\":\"queue\",\"connection\":" +
                "\"AzureWebJobsStorage\",\"queueName\":\"queue3\",\"direction\":\"out\",\"name\":\"outMsg\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testBlobTrigger() {
        JsonObject actual = generatedFunctions.get("blob");
        String str = "{\"bindings\":[{\"type\":\"blobTrigger\",\"name\":\"blobIn\",\"direction\":\"in\"," +
                "\"path\":\"bpath1/{name}\",\"connection\":\"AzureWebJobsStorage\"},{\"type\":\"blob\"," +
                "\"direction\":\"out\",\"name\":\"outMsg\",\"path\":\"bpath1/newBlob\"," +
                "\"connection\":\"AzureWebJobsStorage\",\"dataType\":\"string\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testBlobTriggerInlineListener() {
        JsonObject actual = generatedFunctions.get("blob1");
        String str = "{\"bindings\":[{\"type\":\"blobTrigger\",\"name\":\"blobIn\",\"direction\":\"in\"," +
                "\"path\":\"bpath1/{name}\",\"connection\":\"AzureWebJobsStorage\"},{\"type\":\"blob\"," +
                "\"direction\":\"out\",\"name\":\"outMsg\",\"path\":\"bpath1/newBlob\"," +
                "\"connection\":\"AzureWebJobsStorage\",\"dataType\":\"string\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(actual, parse);
    }

    @Test
    public void testEscapeSequence() {
        JsonObject httpHello = generatedFunctions.get("post-hello--hello-query");
        String str =
                "{\"bindings\":[{\"type\":\"httpTrigger\",\"authLevel\":\"anonymous\",\"methods\":[\"post\"]," +
                        "\"direction\":\"in\",\"name\":\"httpPayload\",\"route\":\"hello-/hello-query\"}," +
                        "{\"type\":\"http\",\"direction\":\"out\",\"name\":\"resp\"}]}";
        JsonElement parse = jsonParser.parse(str);
        Assert.assertEquals(httpHello, parse);
    }
}
