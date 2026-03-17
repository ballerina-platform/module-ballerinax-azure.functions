## Overview

The Azure Functions connector provides an annotation-based [Azure Functions](https://azure.microsoft.com/en-us/services/functions/) extension implementation for serverless function development.

### Key Features

- Deploy functions with HTTP, Queue, Blob, CosmosDB, and Timer triggers
- Declaratively connect external resources using input and output bindings
- Automatic artifact generation and data binding through compiler extension
- Support for Twilio output binding for messaging integration

### Azure Setup

* An Azure "Function App" needs to be created in a given resource group with the following requirements
   - Runtime stack - "Java 21"
   - Hosting operating system - "Windows" (This is the default and Linux is not supported in Azure for custom handlers at the moment)

## Supported Triggers and bindings:

An Azure Function consists of a trigger and optional bindings. A trigger defines how a function is invoked. A binding is
an approach in which you can declaratively connect other resources to the function. There are input and output bindings.
An input binding is a source of data into the function. An output binding allows outputting data from the function to an
external resource. For more information, go
to <a href="https://docs.microsoft.com/en-us/azure/azure-functions/functions-triggers-bindings" target="_blank">Azure
Functions triggers and bindings concepts</a>.

- HTTP - Trigger and Output Binding
- Queue - Trigger and Output Binding
- Blob - Trigger, Input and Output Binding
- Twilio - Output Binding
- CosmosDB - Trigger, Input and Output Binding
- Timer - Trigger

#### Sample Code:

In Ballerina, Triggers are represented with listeners. When the `af:HttpListener` gets attached to the service, it
implies that the function is a HTTP Trigger. The resource function behaves exactly the same as a service written
from `ballerina/http`. It supports `http:Payload, http:Header` annotations for parameters. Input binding annotations can
be used to annotate parameters to make use of external services in azure. if no annotations are specified for a
parameter, it's identified as a query parameter.

Output bindings are defined in the return type definition. For services with the `HttpListener` attachment, `HttpOutput`
is the default Output binding. Of course, you can override the default behavior by specifying them explicitly in the
return type.

```ballerina
import ballerinax/azure.functions as af;

service / on new af:HttpListener() {
    resource function get hello(string name) returns string {
        return "Hello, " + name + "!";
    }
}
```

In the code sample shown above, it has an empty service path and resource path named `hello`. The accessor is `get`. It
expects a request with a query parameter for the field `name`. The required artifact generation and data binding will be
handled by `ballerinax/azure.functions` package automatically.

- For information on the operations, which you can perform with this module, see [API Docs](https://docs.central.ballerina.io/ballerinax/azure-functions/).
- For more information on the deployment, see the [Azure Functions Deployment Guide](https://ballerina.io/swan-lake/learn/user-guide/deployment/azure-functions/).
- For examples on the usage of the operations, see the [Azure Functions Deployment Example](https://ballerina.io/swan-lake/learn/by-example/azure-functions-deployment.html).

### Report Issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina Azure Functions repository](https://github.com/ballerina-platform/module-ballerinax-azure.functions).

### Useful Links
- Discuss code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
- Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
- Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
