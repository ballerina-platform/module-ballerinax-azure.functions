# Ballerina Azure Functions Extension

Annotation based Azure Functions extension implementation for Ballerina. 

[![Daily build](https://github.com/ballerina-platform/module-ballerinax-azure.functions/workflows/Daily%20build/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-azure.functions/actions?query=workflow%3A%22Daily+build%22)
![Ballerina Azure Functions Build](https://github.com/ballerina-platform/module-ballerinax-azure.functions/workflows/Ballerina%20Azure%20Functions%20Build/badge.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerinax-azure.functions/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerinax-azure.functions)

## Azure Setup

* An Azure "Function App" needs to be created in a given resource group with the following requirements
   - Runtime stack - "Java 11"
   - Hosting operating system - "Windows" (default; Linux is not supported in Azure for custom handlers at the moment)

## Supported Triggers and bindings:

An Azure Function consists of a trigger and optional bindings. A trigger defines how a function is invoked. A binding is
an approach in which you can declaratively connect other resources to the function. There are input and output bindings.
An input binding is a source of data into the function. An output binding allows outputting data from the function to an
external resource. For more information, go
to <a href="https://docs.microsoft.com/en-us/azure/azure-functions/functions-triggers-bindings" target="_blank">Azure
Functions triggers and bindings concepts</a>.

- Http - Trigger and Output Binding
- Queue - Trigger and Output Binding
- Blob - Trigger, Input and Output Binding
- Twilio - Output Binding
- CosmosDB - Trigger, Input and Output Binding
- Timer - Trigger

#### Sample Code:

In ballerina, Triggers are represented with listeners. When the `af:HttpListener` gets attached to the service, it
implies that the function is a HTTP Trigger. The resource function behaves exactly the same as a service written
from `ballerina/http`. It supports `http:Payload, http:Header` annotations for parameters. Input binding annotations can
be used to annotate parameters to make use of external services in azure. if no annotations are specified for a
parameter, it's identified as a query parameter.

Output bindings are defined in the return type definition. For services with the `HttpListener` attachment, `HttpOutput`
is the default Output binding. Of course, you can override the default behavior by specifying them explicitly in the
return type.

```ballerina
import ballerinax/azure_functions as af;

service / on new af:HttpListener() {
    resource function get hello(string name) returns string {
        return "Hello, " + name + "!";
    }
}
```

In the code sample shown above, it has an empty service path and resource path named `hello`. The accessor is `get`. It
expects a request with a query parameter for the field `name`. The required artifact generation and data binding will be
handled by `ballerinax/azure_functions` package automatically.

The Azure Functions functionality is implemented as a compiler extension. Thus, artifact generation happens automatically when you build a Ballerina module. Let's see how this works by building the above code.

```
$ bal build
Compiling source
        wso2/azure_functions_deployment:0.1.0

Generating executable
        @azure_functions:Function: get-hello

        Execute the below command to deploy the function locally:
        func start --script-root target/azure_functions --java

        Execute the below command to deploy Ballerina Azure Functions:
        func azure functionapp publish <function_app_name> --script-root target/azure_functions 

        target/bin/azure_functions_deployment.jar
```
