Jsons2xsd
=========
Json schema (http://json-schema.org/) to XML schema (XSD) converter.

# Features
* Single purpose library
* Fast
* Minimal dependencies

# Features supported
TBD

Feedback and pull requests greatly appreciated.

# Build status

[![Build Status](https://travis-ci.org/ethlo/jsons2xsd.png?branch=master)](https://travis-ci.org/ethlo/jsons2xsd)

# Maven repository
http://ethlo.com/maven

# Maven artifact
```xml
<dependency>
  <groupId>com.ethlo.schematools</groupId>
  <artifactId>jsons2xsd</artifactId>
  <version>0.2-SNAPSHOT</version>
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

# Examples

Input Json schema:
```javascript
{
    "$schema": "http://json-schema.org/draft-03/schema#",
    "description": "A representation of a person, company, organization, or place",
    "type": "object",
    "properties": {
        "fn": {
            "description": "Formatted Name",
            "type": "string"
        },
        "familyName": { "type": "string", "required": true },
        "givenName": { "type": "string", "required": true },
        "additionalName": { "type": "array", "items": { "type": "string" } },
        "honorificPrefix": { "type": "array", "minItems":1, "maxItems":10, "items": { "type": "string", "maxLength":17} },
        "honorificSuffix": { "type": "array", "items": { "type": "string" } },
        "nickname": { "type": "string" },
        "age": { "type": "integer", "required":true, "minimum":18 },
        "ssn": { "type": "string", "required":false, "pattern":"[0-9]{3}-[0-9]{2}-[0-9]{4}", "minLength":5},
        "url": { "type": "string", "format": "uri" },
        "preferred_format": {"enum":["plaintext", "html", "pdf", 123], "default":"plaintext"},
        "email": {
            "type": "object",
            "properties": {
                "type": { "type": "string" },
                "value": { "type": "string", "format": "email" }
            }
        },
        "tel": {
	    "required": true,
            "type": "object",
            "properties": {
                "type": { "type": "string" },
                "value": { "type": "string", "format": "phone" }
            }
        },
        "tz": { "type": "string" },
        "photo": { "type": "string" },
        "logo": { "type": "string" },
        "sound": { "type": "string" },
        "bday": { "type": "string", "format": "date" },
        "title": { "type": "string" },
        "role": { "type": "string" },
        "org": {
            "type": "object",
            "properties": {
                "organizationName": { "type": "string" },
                "organizationUnit": { "type": "string" }
            }
        }
    }
}
```

Result of conversion:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<schema xmlns:xs="http://www.w3.org/2001/XMLSchema" attributeFormDefault="qualified" elementFormDefault="qualified" targetNamespace="http://ethlo.com/schema/contacts-1.0.xsd" xmlns="http://www.w3.org/2001/XMLSchema">
  <complexType name="mySpecialType">
    <sequence>
      <element minOccurs="0" name="fn" type="string"/>
      <element name="familyName" type="string"/>
      <element name="givenName" type="string"/>
      <element minOccurs="0" name="additionalName">
        <complexType>
          <sequence>
            <element maxOccurs="unbounded" minOccurs="0" name="item" type="string"/>
          </sequence>
        </complexType>
      </element>
      <element minOccurs="0" name="honorificPrefix">
        <complexType>
          <sequence>
            <element maxOccurs="10" minOccurs="1" name="item" type="string"/>
          </sequence>
        </complexType>
      </element>
      <element minOccurs="0" name="honorificSuffix">
        <complexType>
          <sequence>
            <element maxOccurs="unbounded" minOccurs="0" name="item" type="string"/>
          </sequence>
        </complexType>
      </element>
      <element minOccurs="0" name="nickname" type="string"/>
      <element name="age">
        <simpleType>
          <restriction base="int">
            <minInclusive value="18"/>
          </restriction>
        </simpleType>
      </element>
      <element minOccurs="0" name="ssn">
        <simpleType>
          <restriction base="string">
            <minLength value="5"/>
            <pattern value="[0-9]{3}-[0-9]{2}-[0-9]{4}"/>
          </restriction>
        </simpleType>
      </element>
      <element minOccurs="0" name="url" type="anyURI"/>
      <element minOccurs="0" name="preferred_format">
        <simpleType>
          <restriction base="string">
            <enumeration value="plaintext"/>
            <enumeration value="html"/>
            <enumeration value="pdf"/>
            <enumeration value="123"/>
          </restriction>
        </simpleType>
      </element>
      <element minOccurs="0" name="email">
        <complexType>
          <sequence>
            <element minOccurs="0" name="type" type="string"/>
            <element minOccurs="0" name="value" type="string"/>
          </sequence>
        </complexType>
      </element>
      <element name="tel">
        <complexType>
          <sequence>
            <element minOccurs="0" name="type" type="string"/>
            <element minOccurs="0" name="value" type="string"/>
          </sequence>
        </complexType>
      </element>
      <element minOccurs="0" name="tz" type="string"/>
      <element minOccurs="0" name="photo" type="string"/>
      <element minOccurs="0" name="logo" type="string"/>
      <element minOccurs="0" name="sound" type="string"/>
      <element minOccurs="0" name="bday" type="date"/>
      <element minOccurs="0" name="title" type="string"/>
      <element minOccurs="0" name="role" type="string"/>
      <element minOccurs="0" name="org">
        <complexType>
          <sequence>
            <element minOccurs="0" name="organizationName" type="string"/>
            <element minOccurs="0" name="organizationUnit" type="string"/>
          </sequence>
        </complexType>
      </element>
    </sequence>
  </complexType>
</schema>


```
