Spring-Actuator-Config
======================

[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.ethlo.spring/spring-actuator-config/badge.svg)](http://repo1.maven.org/maven2/com/ethlo/spring/spring-actuator-config)
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)](LICENSE)
[![Build Status](https://travis-ci.org/ethlo/spring-actuator-addons.svg?branch=master)](https://travis-ci.org/ethlo/spring-actuator-addons)

Endpoint that returns the configuration properties along with the [Spring meta-data](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html) to get nice descriptions and hints about the application's configuration parameters.

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
  // Make available under /<management.context-path>/config
  return new ConfigurationPropertiesEndpoint("config");
}
```
* https://docs.spring.io/spring-boot/docs/current/reference/html/production-ready-monitoring.html#production-ready-customizing-management-server-context-path

## Example data
The following example data and screenshots are from the [geodata project](https://github.com/ethlo/geodata)

### Raw JSON data from actuator
![Raw JSON data from actuator](doc/actuator_json.png)

### Data as displayed in IDE (Spring STS)
![Raw JSON data from actuator](doc/eclipse_sts.png)

### Data as displayed in custom (very simple) web UI
![Raw JSON data from actuator](doc/web-ui.png)

This very [simple web-ui can be grabbed from here](doc/sample/config.html) and added as a static resource in your project as a starting point.
