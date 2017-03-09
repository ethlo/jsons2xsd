jsons2xsd
=========
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.schematools/jsons2xsd.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.ethlo.persistence.tools%22)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Build Status](https://travis-ci.org/ethlo/jsons2xsd.svg?branch=master)](https://travis-ci.org/ethlo/jsons2xsd)

Json schema (http://json-schema.org/) to XML schema (XSD) converter.

## Features
* Single purpose library
* Fast
* Minimal dependencies (Jackson)

## Usage

```java
final Reader jsonSchema = ...;
final String targetNameSpaceUri = "http://my.example.com/ns";
final OuterWrapping wrapping = OuterWrapping.TYPE;
final String name = "mySpecialType";
final Document xsdDocument = Jsons2Xsd.convert(jsonSchema, targetNameSpaceUri, wrapping, name);
```
