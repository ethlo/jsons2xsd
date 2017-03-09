Jsons2xsd
=========
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.schematools/jsons2xsd.svg)]()
[![Hex.pm](https://img.shields.io/hexpm/l/plug.svg)]()
[![Build Status](https://travis-ci.org/ethlo/jsons2xsd.svg?branch=master)](https://travis-ci.org/ethlo/jsons2xsd)

Json schema (http://json-schema.org/) to XML schema (XSD) converter.

# Features
* Single purpose library
* Fast
* Minimal dependencies (Jackson)

# Build status

[![Build Status](https://travis-ci.org/ethlo/jsons2xsd.png?branch=master)](https://travis-ci.org/ethlo/jsons2xsd)

# Maven repository
http://ethlo.com/maven

# Maven artifact
```xml
<dependency>
  <groupId>com.ethlo.schematools</groupId>
  <artifactId>jsons2xsd</artifactId>
  <version>${jsons2xsd.version}</version>
</dependency>
```

# Usage

```java
final Reader jsonSchema = ...;
final String targetNameSpaceUri = "http://my.example.com/ns";
final OuterWrapping wrapping = OuterWrapping.TYPE;
final String name = "mySpecialType";
final Document xsdDocument = Jsons2Xsd.convert(jsonSchema, targetNameSpaceUri, wrapping, name);
```
