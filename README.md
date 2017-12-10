jsons2xsd
=========
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.schematools/jsons2xsd.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.ethlo.schematools%22)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Coverage Status](https://coveralls.io/repos/github/ethlo/jsons2xsd/badge.svg?branch=v2.0)](https://coveralls.io/github/ethlo/jsons2xsd?branch=v2.0)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/b60e8e4fd0d541c5ac669c971850316f)](https://www.codacy.com/app/ethlo/jsons2xsd?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ethlo/jsons2xsd&amp;utm_campaign=Badge_Grade)
[![Build Status](https://travis-ci.org/ethlo/jsons2xsd.svg?branch=v2.0)](https://travis-ci.org/ethlo/jsons2xsd)

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
