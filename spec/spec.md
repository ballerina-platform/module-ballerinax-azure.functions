### Supported Triggers And Bindings

Supported From Ballerina - :heavy_check_mark: Supported From Azure - :white_check_mark: Not supported - :x:

| Type                	| Trigger            	| Input              	| Output             	|
|---------------------	|--------------------	|--------------------	|--------------------	|
| Blob storage        	| :heavy_check_mark: 	| :heavy_check_mark: 	| :heavy_check_mark: 	|
| Azure Cosmos DB     	| :heavy_check_mark: 	| :heavy_check_mark: 	| :heavy_check_mark: 	|
| Azure SQL (preview) 	|                    	| :white_check_mark: 	| :white_check_mark: 	|
| Dapr                	|                    	| :white_check_mark: 	| :white_check_mark: 	|
| Event Grid          	| :white_check_mark: 	|                    	| :white_check_mark: 	|
| Event Hubs          	| :white_check_mark: 	|                    	| :white_check_mark: 	|
| HTTP & webhooks     	| :heavy_check_mark: 	|                    	| :heavy_check_mark: 	|
| IoT Hub             	| :white_check_mark: 	|                    	|                    	|
| Kafka               	| :white_check_mark: 	|                    	| :white_check_mark: 	|
| Mobile Apps         	|                    	| :white_check_mark: 	| :white_check_mark: 	|
| Notification Hubs   	|                    	|                    	| :white_check_mark: 	|
| Queue storage       	| :heavy_check_mark: 	|                    	| :heavy_check_mark: 	|
| RabbitMQ            	| :white_check_mark: 	|                    	| :white_check_mark: 	|
| SendGrid            	|                    	|                    	| :white_check_mark: 	|
| Service Bus         	| :white_check_mark: 	|                    	| :white_check_mark: 	|
| SignalR             	| :white_check_mark: 	| :white_check_mark: 	| :white_check_mark: 	|
| Table storage       	|                    	| :white_check_mark: 	| :white_check_mark: 	|
| Timer               	| :heavy_check_mark: 	|                    	|                    	|
| Twilio              	|                    	|                    	| :heavy_check_mark: 	|

## HTTP

### 2.1 Listener
Azure functions HTTP trigger is mapped to HttpListener defined in the module. You can define the listener in two ways.

Separate listener declaration
```ballerina
import ballerinax/azure_functions as af;

listener af:HTTPListener ep = new ();

service "hello" on ep {
}
```

Inline listener declaration
```ballerina
import ballerinax/azure_functions as af;

service "hello" on new af:HTTPListener() {
}
```


### 2.2 Service
Service is a collection of resources functions, which are the network entry points of a ballerina program.
In addition to that a service can contain public and private functions which can be accessed by calling with `self`.

#### 2.2.1. Service type
```ballerina
public type HttpService distinct service object {

};
```
#### 2.2.2.Annotation
Each Listener can be attached with an optional annotation which contains the configurations required for the azure platform.
```ballerina
public type AUTH_LEVEL "anonymous"|"function"|"admin";

public type HTTPTriggerConfiguration record {|
    AUTH_LEVEL authLevel = "anonymous";
|};
```

If an output Binding annotation is specified, HTTPOutput will be taken implicitly.

#### 2.2.2. Service base path

The base path is considered during the request dispatching to discover the service. Identifiers and string literals
are allowed to be stated as base path and it should be started with `/`. The base path is optional and it will be
defaulted to `/` when not defined. If the base path contains any special characters, those should be escaped or defined
as string literals

```ballerina
service hello\-world on new af:HTTPListener() {
   resource function get foo() {

   }
}

service http:Service "hello-world" on new af:HTTPListener() {
   resource function get foo() {

   }
}
```

A service can be declared in three ways upon the requirement.

#### 2.2.3. Service declaration
The [Service declaration](https://ballerina.io/spec/lang/2021R1/#section_8.3.2) is a syntactic sugar for creating a
service and it is the mostly used approach for creating a service. The declaration gets desugared into creating a
listener object, creating a service object, attaching the service object to the listener object.

```ballerina
service af:HttpService /foo/bar on new af:HTTPListener() {
  resource function get greeting() returns string {
      return "hello world";
  }
}
```

### 2.3. Resource

A method of a service can be declared as a [resource function](https://ballerina.io/spec/lang/2021R1/#resources)
which is associated with configuration data that is invoked by a network message by a Listener. Users write the
business logic inside a resource and expose it over the network.

#### 2.3.1. Accessor
The accessor-name of the resource represents the HTTP method and it can be get, post, put, delete, head, patch, options
and default. If the accessor is unmatched, 405 Method Not Allowed response is returned. When the accessor name is
stated as default, any HTTP method can be matched to it in the absence of an exact match. Users can define custom
methods such as copy, move based on their requirement. A resource which can handle any method would look like as
follows. This is useful when handling unmatched verbs.

```ballerina
resource function 'default NAME_TEMPLATE () {
    
}
```
#### 2.3.2. Resource name
The resource-name represents the path of the resource which is considered during the request dispatching. The name can
be hierarchical(foo/bar/baz). Each path identifier should be separated by `/` and first path identifier should not
contain a prefixing `/`. If the paths are unmatched, 404 NOT FOUND response is returned.
```ballerina
resource function post hello() {
    
}
```
Only the identifiers can be used as resource path not string literals. Dot identifier is
used to denote the `/` only if the path contains a single identifier.
```ballerina
resource function post .() {
    
}
```
Any special characters can be used in the path by escaping.
```ballerina
resource function post hello\-world() {
    
}
```

#### 2.3.3. Path parameter
The path parameter segment is also a part of the resource name which is declared within brackets along with the type.
As per the following resource name, baz is the path param segment and it’s type is string. Like wise users can define
string, int, boolean, float, and decimal typed path parameters. If the paths are unmatched, 404 NOT FOUND response
is returned. If the segment failed to parse into the expected type, 500 Internal Server Error response is returned.

```ballerina
resource function post foo/bar/[string baz]/qux() {
    // baz is the path param
}

resource function get data/[int age]/[string name]/[boolean status]/[float weight]() returns json {
   int balAge = age + 1;
   float balWeight = weight + 2.95;
   string balName = name + " lang";
   if (status) {
       balName = name;
   }
   json responseJson = { Name:name, Age:balAge, Weight:balWeight, Status:status, Lang: balName};
   return responseJson;
}
```

If multiple path segments needs to be matched after the last identifier, Rest param should be used at the end of the
resource name as the last identifier. string, int, boolean, float, and decimal types are supported as rest parameters.
```ballerina
resource function get foo/[string... bar]() returns json {
   json responseJson = {"echo": bar[0]};
   return responseJson;
}
```

Using both `'default` accessor and the rest parameters, a default resource can be defined to a service. This
default resource can act as a common destination where the unmatched requests (either HTTP method or resource path) may
get dispatched.

```ballerina
resource function 'default [string... s]() {

}
```

##### 2.3.4.3. Query parameter

The query param is a URL parameter which is available as a resource function parameter and it's not associated
with any annotation or additional detail. This parameter is not compulsory and not ordered. The type of query param
are as follows

```ballerina
type BasicType boolean|int|float|decimal|string|map<json>;
```

The same query param can have multiple values. In the presence of multiple such values,  If the user has specified
the param as an array type, then all values will return. If not the first param values will be returned. As per the
following resource function, the request may contain at least two query params with the key of bar and id.
Eg : “/hello?bar=hi&id=56”

```ballerina
resource function get hello(string bar, int id) { 
    
}
```

If the query parameter is not defined in the function signature, then the query param binding does not happen. If a
query param of the request URL has no corresponding parameter in the resource function, then that param is ignored.
If the parameter is defined in the function, but there is no such query param in the URL, that request will lead
to a 400 BAD REQUEST error response unless the type is nilable (string?)

<table>
<tr>
<th> Case </th>
<th>  Resource argument </th>
<th>  Query </th>
<th>  Mapping </th>
</tr>
<tr>
<td rowspan=4> 1 </td>
<td rowspan=4> string foo </td>
<td> foo=bar </td>
<td> bar </td>
</tr>
<tr>
<td> foo=</td>
<td> "" </td>
</tr>
<tr>
<td> foo</td>
<td> Error : no query param value found for 'foo' </td>
</tr>
<tr>
<td> No query</td>
<td> Error : no query param value found for 'foo' </td>
</tr>
<tr>
<td rowspan=4> 2 </td>
<td rowspan=4> string? foo </td>
<td> foo=bar </td>
<td> bar </td>
</tr>
<tr>
<td> foo=</td>
<td> "" </td>
</tr>
<tr>
<td> foo</td>
<td> nil </td>
</tr>
<tr>
<td> No query</td>
<td> nil </td>
</tr>
</table>

### Payload parameter

The payload parameter is used to access the request payload during the resource invocation. When the payload param is
defined with @http:Payload annotation, the listener deserialize the inbound request payload based on the media type
which retrieved by the `Content-type` header of the request. The data binding happens thereafter considering the
parameter type. The type of payload parameter can be one of the `anytype`. If the header is not present or not a
standard header, the binding type is inferred by the parameter type.

Following table explains the compatible `anydata` types with each common media type. In the absence of a standard media
type, the binding type is inferred by the payload parameter type itself. If the type is not compatible with the media
type, error is returned.

|Ballerina Type | Structure|"text" | "xml" | "json" | "x-www-form-urlencoded" | "octet-stream"|
|---------------|----------|-------|-------|--------|-------------------------|---------------|
|boolean| | ❌ | ❌ | ✅|❌|❌
| |boolean[]| ❌ | ❌ | ✅|❌|❌
| |map\<boolean\>| ❌ | ❌ | ✅|❌|❌
| |table\<map\<boolean\>\>| ❌ | ❌ | ✅|❌|❌
|int| | ❌ | ❌ | ✅|❌|❌
| |int[]| ❌ | ❌ | ✅|❌|❌
| |map\<int\>| ❌ | ❌ | ✅|❌|❌
| |table\<map\<int\>\>| ❌ | ❌ | ✅|❌|❌
float| | ❌ | ❌ | ✅|❌|❌
| |float[]| ❌ | ❌ | ✅|❌|❌
| |map\<float\>| ❌ | ❌ | ✅|❌|❌
| |table\<map\<float\>\>| ❌ | ❌ | ✅|❌|❌
decimal| | ❌ | ❌ | ✅|❌|❌
| |decimal[]| ❌ | ❌ | ✅|❌|❌
| |map\<decimal\>| ❌ | ❌ | ✅|❌|❌
| |table\<map\<decimal\>\>| ❌ | ❌ | ✅|❌|❌
byte[]| | ✅ | ❌ | ✅|❌|✅
| |byte[][]| ❌ | ❌ | ✅|❌|❌
| |map\<byte[]\>| ❌ | ❌ | ✅|❌|❌
| |table\<map\<byte[]\>\>| ❌ | ❌ | ✅|❌|❌
string| |✅|❌|✅|✅|❌
| |string[]| ❌ | ❌ | ✅|❌|❌
| |map\<string\>| ❌ | ❌ | ✅|✅|❌
| |table\<map\<string\>\>| ❌ | ❌ | ✅|❌|❌
xml| | ❌ | ✅ | ❌|❌|❌
json| | ❌ | ❌ | ✅|❌|❌
| |json[]| ❌ | ❌ | ✅|❌|❌
| |map\<json\>| ❌ | ❌ | ✅|❌|❌
| |table\<map\<json\>\>| ❌ | ❌ | ✅|❌|❌
map| | ❌ | ❌ | ✅|❌|❌
| |map[]| ❌ | ❌ | ✅|❌|❌
| |map\<map\>| ❌ | ❌ | ✅|❌|❌
| |table\<map\<map\>\>| ❌ | ❌ | ✅|❌|❌
record| |❌|❌|✅|❌|❌
| |record[]| ❌ | ❌ | ✅|❌|❌
| |map\<record\>| ❌ | ❌ | ✅|❌|❌
| |table\<record\>| ❌ | ❌ | ✅|❌|❌
 ```bal
   resource function post query(string name, @af:Payload string greeting) returns @af:HTTPOutput string|error {
               return "Hello from the query " + greeting + " " + name;
   }
```


##### 2.3.4.5. Header parameter

The header parameter is to access the inbound request headers The header param is defined with `@http:Header` annotation
The type of header param can be defined as follows;

```ballerina
type BasicType string|int|float|decimal|boolean;
public type HeaderParamType ()|BasicType|BasicType[]|record {| BasicType...; |};
```

When multiple header values are present for the given header, the first header value is returned when the param
type is `string` or any of the basic types. To retrieve all the values, use `string[]` type or any array of the
basic types. This parameter is not compulsory and not ordered.

The header param name is considered as the header name during the value retrieval. However, the header annotation name
field can be used to define the header name whenever user needs some different variable name for the header.

User cannot denote the type as a union of pure type, array type, or record type together, that way the resource
cannot infer a single type to proceed. Hence, returns a compiler error.

In the absence of a header when the param is defined in the resource signature, listener returns 400 BAD REQUEST unless
the type is nilable.

```ballerina
//Single header value extraction
resource function post hello1(@http:Header string referer) {
    
}

//Multiple header value extraction
resource function post hello2(@http:Header {name: "Accept"} string[] accept) {
    
}

public type RateLimitHeaders record {|
    string x\-rate\-limit\-id;
    int x\-rate\-limit\-remaining;
    string[] x\-rate\-limit\-types;
|};

//Populate selected headers to a record
resource function get hello3(@http:Header RateLimitHeaders rateLimitHeaders) {
}
```

If the requirement is to access all the header of the inbound request, it can be achieved through the `http:Headers`
typed param in the signature. It does not need the annotation and not ordered.

```ballerina
resource function get hello3(http:Headers headers) {
   String|error referer = headers.getHeader("Referer");
   String[]|error accept = headers.getHeaders("Accept");
   String[] keys = headers.getHeaderNames();
}
```

The header consists of header name and values. Sometimes user may send header without value(`foo:`). In such
situations, when the header param type is nilable, the values returns nil and same happened when the complete header is
not present in the request. In order to avoid the missing detail, a service level configuration has introduced naming
`treatNilableAsOptional`

```ballerina
@http:ServiceConfig {
    treatNilableAsOptional : false
}
service /headerparamservice on HeaderBindingIdealEP {

    resource function get test1(@http:Header string? foo) returns json {
        
    }
}
```

<table>
<tr>
<th>  Case </th>
<th>  Resource argument </th>
<th>  Header </th>
<th>  Current Mapping (treatNilableAsOptional=true - Default) </th>
<th>  Ideal Mapping (treatNilableAsOptional=false) </th>
</tr>
<tr>
<td rowspan=3> 1 </td>
<td rowspan=3> string foo </td>
<td> foo:bar </td>
<td> bar </td>
<td> bar </td>
</tr>
<tr>
<td> foo:</td>
<td> Error : no header value found for 'foo' </td>
<td> Error : no header value found for 'foo' </td>
</tr>
<tr>
<td> No header</td>
<td> Error : no header value found for 'foo' </td>
<td> Error : no header value found for 'foo' </td>
</tr>
<tr>
<td rowspan=3> 2 </td>
<td rowspan=3> string? foo </td>
<td> foo:bar </td>
<td> bar </td>
<td> bar </td>
</tr>
<tr>
<td> foo:</td>
<td> nil </td>
<td> nil </td>
</tr>
<tr>
<td> No header</td>
<td> nil </td>
<td> Error : no header value found for 'foo' </td>
</tr>
</table>

#### 2.3.5. Return types
The resource function supports anydata, error?, http:Response and http:StatusCodeResponse as return types.

```ballerina
resource function XXX NAME_TEMPLATE () returns @http:Payload anydata|http:Response|http:StatusCodeResponse|http:Error? {
}
```

In addition to that the `@http:Payload` annotation can be specified along with anydata return type
mentioning the content type of the outbound payload.

```ballerina
resource function get test() returns @http:Payload {mediaType:"text/id+plain"} string {
    return "world";
}
```

Based on the return types respective header value is added as the `Content-type` of the `http:Response`.

| Type                                                                  | Content Type             |
|-----------------------------------------------------------------------|--------------------------|
| ()                                                                    | -                        |
| string                                                                | text/plain               |
| xml                                                                   | application/xml          |
| byte[]                                                                | application/octet-stream |
| int, float, decimal, boolean                                          | application/json         |
| map\<json\>, table<map\<json\>>, map\<json\>[], table<map\<json\>>)[] | application/json         |
| http:StatusCodeResponse                                               | application/json         |

##### 2.3.5.1. Status Code Response

The status code response records are defined in the HTTP module for every HTTP status code.

```ballerina
type Person record {
   string name;
};
resource function put person(string name) returns record {|*http:Created; Person body;|} {
   Person person = {name:name};
   return {
       mediaType: "application/person+json",
       headers: {
           "X-Server": "myServer"
       },
       body: person
   };
}
```

Following is the `http:Ok` definition. Likewise, all the status codes are provided.

```ballerina
public type Ok record {
   readonly StatusOk status;
   string mediaType;
   map<string|string[]> headers?;
   anydata body?;
};

resource function get greeting() returns http:Ok|http:InternalServerError {
   http:Ok ok = { body: "hello world", headers: { xtest: "foo"} };
   return ok;
}
```

##### 2.3.5.2. Return nil


The return nil from the resource will return 202 ACCEPTED response.
```ballerina
resource function post person(@http:Payload Person p) {
    int age = p.age;
    io:println(string `Age is: ${age}`);
}
```

##### 2.3.5.3. Default response status codes

To improve the developer experience for RESTful API development, following default status codes will be used in outbound
response when returning `anydata` directly from a resource function.

| Resource Accessor | Semantics                                                     | Status Code             |
|-------------------|---------------------------------------------------------------|-------------------------|
| GET               | Retrieve the resource                                         | 200 OK                  |
| POST              | Create a new resource                                         | 201 Created             |
| PUT               | Create a new resource or update an existing resource          | 200 OK                  |
| PATCH             | Partially update an existing resource                         | 200 OK                  |
| DELETE            | Delete an existing resource                                   | 200 OK                  |
| HEAD              | Retrieve headers                                              | 200 OK                  |
| OPTIONS           | Retrieve permitted communication options                      | 200 OK                  |

### Artifact generation
//TODO Fill
#### Function name Generation
//TODO Fill

## Queue

 ```bal
@af:QueueTrigger {
   queueName: "queue2"
}
listener af:QueueListener queueListener = new af:QueueListener();
service "queue" on queueListener {
   remote function onMessage (@af:Payload string inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
               return "helloo "+ inMsg;
   }
}
```


## CosmosDB

 ```bal
@af:CosmosDBTrigger {connectionStringSetting: "CosmosDBConnection", databaseName: "db1", collectionName: "c2"}
listener af:CosmosDBListener cosmosEp = new ();
 
service "cosmos" on cosmosEp {
   remote function onUpdated (@af:Payload DBEntry[] inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
       string id = inMsg[0].id;
       return "helloo "+ id;
   }
}
```


## Blob

 ```bal
@af:BlobTrigger { path: "bpath1/{name}" }
listener af:BlobListener blobEp = new ();
 
service "blob" on blobEp {
   remote function onUpdated (@af:Payload byte[] inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
       string id = inMsg[0].id;
       return "helloo "+ id;
   }
}
```

## Timer

 ```bal
@af:TimerTrigger { schedule: "*/10 * * * * *" }
listener af:TimerListener timerEp = new ();
 
service "timer" on timerEp {
   remote function onTriggered (@af:Payload json inMsg) returns @af:QueueOutput {queueName: "queue3"} string|error {
       string id = inMsg[0].id;
       return "helloo "+ id;
   }
}
```


## Twilio


 ```bal
@af:TimerTrigger { schedule: "*/10 * * * * *" }
listener af:TimerListener timerEp = new ();
 
service "timer" on timerEp {
   remote function onTriggered (@af:Payload json inMsg) returns @af:QueueOutput @af:TwilioSmsOutput { fromNumber: "+12069845840" }  string|error {
       string id = inMsg[0].id;
       return "helloo "+ id;
   }
}
```
