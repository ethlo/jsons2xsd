jsons2xsd
=========
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.jsons2xsd/jsons2xsd.svg)](https://search.maven.org/artifact/com.ethlo.jsons2xsd/jsons2xsd)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Coverage Status](https://coveralls.io/repos/github/ethlo/jsons2xsd/badge.svg)](https://coveralls.io/github/ethlo/jsons2xsd)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b60e8e4fd0d541c5ac669c971850316f)](https://www.codacy.com/app/ethlo/jsons2xsd?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ethlo/jsons2xsd&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/ethlo/jsons2xsd.svg)](https://travis-ci.org/ethlo/jsons2xsd)
[![GitHub issues open](https://img.shields.io/github/issues/ethlo/jsons2xsd.svg)](https://github.com/ethlo/jsons2xsd/issues)

[JSON-schema](http://json-schema.org/) to [XML schema](https://www.w3.org/TR/xmlschema11-1/) converter written in Java.

## Dependency
```xml
<dependency>
  <groupId>com.ethlo.jsons2xsd</groupId>
  <artifactId>jsons2xsd</artifactId>
  <version>2.3.1-SNAPSHOT</version>
</dependency>
```
## Snapshots

```xml
<repositories>
  <repository>
    <id>sonatype-nexus-snapshots</id>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  </repository>
</repositories>
```

## Usage

```java
try (final Reader r = ...)
{
  final Config cfg = new Config.Builder()
    .targetNamespace("http://example.com/myschema.xsd")
    .name("array")
    .build();
  final Document doc = Jsons2Xsd.convert(r, cfg);
}
```

### Example input
```json
{
  "type":"array",
  "items":{
    "type":"object",
    "properties":{
      "price":{
        "type":"number",
        "minimum":0
      },
      "name":{
        "type":"string",
        "minLength":5,
        "maxLength":32
      },
      "isExpired":{
        "default":false,
        "type":"boolean"
      },
      "manufactured":{
        "type":"string",
        "format":"date-time"
      }
    },
    "required":[
      "price",
      "name",
      "manufactured"
    ]
  }
}
```

### Example output
```xml
<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns="http://www.w3.org/2001/XMLSchema" xmlns:x="http://example.com/myschema.xsd" elementFormDefault="qualified" targetNamespace="http://example.com/myschema.xsd">
  <complexType name="array">
    <sequence>
      <element name="price">
        <simpleType>
          <restriction base="decimal">
            <minInclusive value="0" />
          </restriction>
        </simpleType>
      </element>
      <element name="name">
        <simpleType>
          <restriction base="string">
            <minLength value="5" />
            <maxLength value="32" />
          </restriction>
        </simpleType>
      </element>
      <element minOccurs="0" name="isExpired" type="boolean" />
      <element name="manufactured" type="dateTime" />
    </sequence>
  </complexType>
</schema>
```

## Support for non-standard types and formats

### Ignore unknown JSON formats
```java
final Config cfg = new Config.Builder()
    .ignoreUnknownFormats(true)
    ...
    .build();
```      

### Register custom JSON formats
```java
final Config cfg = new Config.Builder()
    .customTypeMapping(JsonSimpleType.INTEGER, "int64", XsdSimpleType.LONG)
    .customTypeMapping(JsonSimpleType.INTEGER, "int32", XsdSimpleType.INT)
    .customTypeMapping(JsonSimpleType.STRING, "ext-ref", XsdSimpleType.STRING)
    ...
    .build();
```

### Register non-JSON types
```java
final Config cfg = new Config.Builder()
    .nonJsonTypeMapping("date-time", XsdSimpleType.DATE_TIME)
    .nonJsonTypeMapping("int", XsdSimpleType.INT)
    ...
    .build();
```
