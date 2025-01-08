## Package Overview

This module provides an annotation-based [Azure Functions](https://azure.microsoft.com/en-us/services/functions/) extension implementation for Ballerina. 

- For information on the operations, which you can perform with this module, see [API Docs](https://docs.central.ballerina.io/ballerinax/azure-functions/). 
- For more information on the deployment, see the [Azure Functions Deployment Guide](https://ballerina.io/swan-lake/learn/user-guide/deployment/azure-functions/).
- For examples on the usage of the operations, see the [Azure Functions Deployment Example](https://ballerina.io/swan-lake/learn/by-example/azure-functions-deployment.html).

### Azure Setup

* An Azure "Function App" needs to be created in a given resource group with the following requirements
   - Runtime stack - "Java 21"
   - Hosting operating system - "Windows" (This is the default and Linux is not supported in Azure for custom handlers at the moment)

## Supported Triggers and bindings:

- Http - Trigger and Output Binding
- Queue - Trigger and Output Binding
- Blob - Trigger, Input and Output Binding
- Twilio - Output Binding
- CosmosDB - Trigger, Input and Output Binding
- Timer - Trigger

### Report Issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina Azure Functions repository](https://github.com/ballerina-platform/module-ballerinax-azure.functions).

### Useful Links
- Discuss code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
- Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
- Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
