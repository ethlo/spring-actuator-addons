Spring-Actuator-Config
======================

Endpoint that returns the configuration properties along with the [Spring meta-data](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html) to get nice descriptions and hints about the applications configuration parameters.

## How to use

Include the dependency:

```xml
<dependency>
  <groupId>com.ethlo.spring</groupId>
  <artifactId>spring-actuator-config</artifactId>
  <version>1.5.0</version>
</dependency>
```

Activate the actuator end-point:

```java
@Bean
public ConfigurationPropertiesEndpoint configEndpoint()
{
  return new ConfigurationPropertiesEndpoint("config");
}
```
## Example data
The following example data and screenshots are from the [geodata project](https://github.com/ethlo/geodata)

### Raw JSON data from actuator
![Raw JSON data from actuator](doc/actuator_json.png)

### Data as displayed in IDE (Spring STS)
![Raw JSON data from actuator](doc/eclipse_sts.png)

### Data as displayed in custom (very simple) web UI
![Raw JSON data from actuator](doc/web-ui.png)

This very [simple web-ui can be grabbed from here](doc/sample/config.html) and added as a static resource in your project as a starting point.
