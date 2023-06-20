# Ballerina Azure Functions Extension

Annotation based Azure Functions extension implementation for Ballerina. 

[![Daily build](https://github.com/ballerina-platform/module-ballerinax-azure.functions/workflows/Daily%20build/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-azure.functions/actions?query=workflow%3A%22Daily+build%22)
![Ballerina Azure Functions Build](https://github.com/ballerina-platform/module-ballerinax-azure.functions/workflows/Ballerina%20Azure%20Functions%20Build/badge.svg)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerinax-azure.functions/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerinax-azure.functions)

## Azure Function App Setup

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
## Build from the source

### Set Up the prerequisites

1. Download and install Java SE Development Kit (JDK) version 11 (from one of the following locations).

  * [Oracle](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)

  * [OpenJDK](https://adoptium.net/)

    > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.

2. Export GitHub Personal access token with read package permissions as follows,

        export packageUser=<Username>
        export packagePAT=<Personal access token>

3. Download and install [Docker](https://www.docker.com/).

### Build the source

Execute the commands below to build from source.

1. To build the library:
    ```
    ./gradlew clean build
    ```

2. To run the integration tests:
    ```
    ./gradlew clean test
    ```

3. To run a group of tests
    ```
    ./gradlew clean test -Pgroups=<test_group_names>
    ```

4. To build the package without the tests:
    ```
    ./gradlew clean build -x test
    ```

5. To debug the tests:
    ```
    ./gradlew clean test -Pdebug=<port>
    ```

6. To debug with Ballerina language:
    ```
    ./gradlew clean build -PbalJavaDebug=<port>
    ```

7. Publish the generated artifacts to the local Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToLocalCentral=true
    ```

8. Publish the generated artifacts to the Ballerina central repository:
    ```
    ./gradlew clean build -PpublishToCentral=true
    ```

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`azure_functions` library](https://lib.ballerina.io/ballerinax/azure_functions/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
* test