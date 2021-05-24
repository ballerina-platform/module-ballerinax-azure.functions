## Package Overview

This module provides an annotation based [Azure Functions](https://azure.microsoft.com/en-us/services/functions/) extension implementation for Ballerina language. 

- For information on the operations, which you can perform with this module, see [API Docs](https://docs.central.ballerina.io/ballerinax/azure-functions/). 
- For more information on the deployment, see the [Azure Functions Deployment Guide](https://ballerina.io/swan-lake/learn/user-guide/deployment/azure-functions/).
- For examples on the usage of the operations, see the [Azure Functions Deployment Example](https://ballerina.io/swan-lake/learn/by-example/azure-functions-deployment.html).

### Azure Setup

* An Azure "Function App" needs to be created in a given resource group with the following requirements
   - Runtime stack - "Java 11"
   - Hosting operating system - "Windows" (default; Linux is not supported in Azure for custom handlers at the moment)

## Supported Annotations:

### @azure.functions:Function

#### Custom 'host.json'

A custom [host.json](https://docs.microsoft.com/en-us/azure/azure-functions/functions-host-json) file for the functions deployment can be optionally provided by placing a 'host.json' file in the current working directory in which the `bal build` is done. The required `host.json` properties are provided/overridden by the values derived from the source code by the compiler extension. 

### Report Issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina Azure Functions repository](https://github.com/ballerina-platform/module-ballerinax-azure.functions).

### Useful Links
- Discuss code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
- Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
- Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.

