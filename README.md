# lib-ussd
A lightweight USSD application framework.

*lib-ussd* takes simple declarative UI concepts to USSD application developments.
UI behavior is defined in a XML file as `<screen>` elements while handlers for screen actions are defined as methods in an injectable component (e.g CDI bean or EJB).

Getting Started
===============
Maven
-----
The easiest way to get started using lib-ussd is by using the [ussd-app-archetype](http://search.maven.org/#artifactdetails%7Ccom.hextremelabs.ussd%7Cussd-app-archetype%7C1.0.1%7Cmaven-archetype "Maven archetype") Maven archetype.
Make sure you have Maven installed and type the following at the terminal.

```mvn archetype:generate -DarchetypeGroupId=com.hextremelabs.ussd -DarchetypeArtifactId=ussd-app-archetype```

This generates a sample Maven USSD project. Build the project and deploy the generated `war` file on a Java EE application server.
As this is a Maven project, you can open it in your favorite IDE to start crafting your USSD app!

IDE
---
Alternatively, you can create the sample project from within your favorite IDE. 
On NetBeans for example, click `File` -> `New Project` -> `Maven` -> `Project from Archetype`. Then search for `ussd-app-archetype`.

<img src="http://dev.hextremelabs.net/lib-ussd_mvn_1.png" alt="File -> New Project" style="width: 200px;"/>
<img src="http://dev.hextremelabs.net/lib-ussd_mvn_2.png" alt="Maven -> Project from Archetype" style="width: 300px;"/>
<img src="http://dev.hextremelabs.net/lib-ussd_mvn_3.png" alt="Search for ussd-app-archetype" style="width: 300px;"/>

Starting From Scratch
---------------------
If however you would like to start your USSD application from scratch, you will need to create a Java EE Web Application with the following dependencies:

```
<dependency>
  <groupId>javax</groupId>
  <artifactId>javaee-api</artifactId>
  <version>7.0</version>
  <scope>provided</scope>
</dependency>
<dependency>
  <groupId>com.hextremelabs.ussd</groupId>
  <artifactId>lib-ussd</artifactId>
  <version>1.0.1</version>
</dependency>
```

If you are using a build system that cannot resolve Maven dependencies (e.g Ant) you can grab the latest JAR dependency [here](http://central.maven.org/maven2/com/hextremelabs/ussd/lib-ussd/1.0.1/).

Anatomy of a USSD Project
=========================
Endpoint
-----------
If you build and deploy the genrated project to a Java EE server, publishes a HTTP endpoint at ```http://{server_address}:{port}/{context_root}/ussd/endpoint```
This endpoint is able to receive HTTP GET and POST request. The endpoint extracts the following parameters from HTTP requests (as telcos typically send):

1. `sessionid` (the telco-generated session ID)
2. `msisdn` (e.g `2348021234567`)
3. `provider` (e.g `airtel`)
4. `message` (e.g `*559*3*4#`)

You don't need to create this endpoint, `lib-ussd` automatically publishes it for you. 
If you want to customize how these parameters are being extracted from the HTTP request please override `UssdHandler`'s `parseRequest` method (see below).

Project Structure
-----------------
There are two important files in the generated project:

1. `src/main/resources/ussd-app.xml`
2. `src/main/java/path/to/your/package/Handler.java`

The `ussd-app.xml` file (which is backed by [this schema](http://dev.hextremelabs.net/ussd-app.xsd)) defines all screen interactions.
Most popular IDEs should offer you editing support (suggestions/autocomplete) for this XML file.
The `Handler` class (which extends `UssdHandler`) is where you define screen handlers. 
For every `<screen>` you create in the XML which has a `callback` attribute, there should be a method in `Handler` whose name matches the value of the `callback`.
Such callback methods must have the following signature:
```
public String[] callbackName(Map<String, Object> data) {
  // Do stuff and return string array result.
}
```
If no such method exists, an `InvocationException` is thrown which results in the value of the `errorMessage` attribute defined in the `ussd-app` XML root tag being returned to the user.
If `errorMessage` is not defined a default error message is returned. 
If you want to customize how the handler dispatches calls to callbacks you can override `UssdHandler`'s invoke method.

The elements of the `String[]` returned by callbacks are used to inflate the template of the `nextScreen` when rendering.
If a callback returns `["Sayo", "Oladeji"]` and `nextScreen` targets a screen with content `I am %s, son of %s`, the screen will be rendered as `I am Sayo, son of Oladeji.`
Rendered screens are cached and returned whenever the user reques to go back to the previous screen using the `#` key.

`<screen>`s with attribute `type="textInput` (see below) accept input from users (e.g "Please enter your account number").
All inputs received throughout a session are mapped as `"screenId" -> input`. All results of callbacks are mapped as `"screenId$Result" -> result`. Everytime a callback is invoked, the entire mapping is passed to it. It is the responsibility of the callback to pick the data it needs from this mapping (see the `Handler.greetMe()` method for example).

UI Elements
-----------
The UI framework is extremely simple. It's highly recommended that you play around with the XML in your IDE by invoking its autocomplete/intellisense to see the things it suggests.
Even better is it that you read [the well-documented schema](http://dev.hextremelabs.net/ussd-app.xsd) backing the XML.

Anyway, if you've got no chill here's the gist:

* `ussd-app`: this is the root tag and it has the following attributes:
  * `name`: the name of this USSD application. Think of it as Angular's `ng-app` :)
  * `lineSeparator`: the end-of-line character or character sequence used by the telco. Default is `~`.
  * `errorMessage`: the response returned to the user if an error occurs while performing an operation. Such errors terminate the user's session. Default is `Operation failed. Please try again later.`
  * `maxTextLength`: the maximum length of a USSD response supported by the telco. Default is 140 characters.
* `screen`: defines a screen as seen by the user. This element can have the following attributes:
  * `id`: the identifier of the screen.
  * `callback`: the handler method invoked when this screen is submitted.
  * `nextScreen`: the next screen displayed after this screen is submitted.
  * `type`: the screen type. Acceptable values are:
    * `options`: a list of options presented to the user. Usually this is a numbered list.
    * `textInput`: a screen that prompts the user for input.
    * `display`: a screen that displays a message to the user without expecting any input. This screen implicitly marks the end of a flow and therefore terminates the user session.
  * `validation`: for `textInput` screens; the constraint imposed on the input text. Acceptable values are:
    * `free`: no validation is performed.
    * `numeric`: only numeric values (0 to 9) are allowed.
    * `alphanumeric`: only alphanumeric values (a to z, A to Z, 0 to 9) are allowed.
    * `regex`: the input is valudated against a specified regular expression.
    Default is `free`
  * `regex`: for screens with `validation="regex"`, the regular expression against with the input is validated.
  
  A screen can have the following child elements:
  * `header`: text rendered at the top of the screen.
  * `body`: text rendered at the middle of the screen.
  * `footer`: text rendered at the bottom of the screen.
  * `option`: for screens with `type="options"`. One or more instances of this element denoting each option.
  The values of every of these elements can carry a placeholder `%s`. An attempt is made to sequentially inflate these placeholders using the result of the callback of the previous screen.
* `option`: an option in an enumerated list of options. This element has the following attributes:
  * `trigger`: the character that selects this option. This should be unique in the list (it is recommended to be a sequence of numbers starting from 1).
  * `nextScreen`: the screen that selecting this option leads to.
  
  Runtime Behavior
  ----------------
  * USSD session resumption is supported using the user's `msisdn`.
  * If the user doesn't reach the end of a flow, the session is invalidated after 5 minutes of inactivity.
  * *lib-ussd* is just 53kb and only expects `javaee-web-api`, `javax.enterprise.concurrent-api` and `slf4j-api` to be available on the classpath at runtime. An application server like WildFly supplies these out of the box.
