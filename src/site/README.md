jsons2xsd
=========
[![Maven Central](https://img.shields.io/maven-central/v/com.ethlo.jsons2xsd/jsons2xsd.svg)](https://search.maven.org/artifact/com.ethlo.jsons2xsd/jsons2xsd)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://github.com/ethlo/jsons2xsd/actions/workflows/build.yml/badge.svg)](https://github.com/ethlo/jsons2xsd/actions/workflows/build.yml)
[![GitHub issues open](https://img.shields.io/github/issues/ethlo/jsons2xsd.svg)](https://github.com/ethlo/jsons2xsd/issues)

[JSON-schema](http://json-schema.org/) to [XML schema](https://www.w3.org/TR/xmlschema11-1/) converter written in Java.

Reusable types can be in `definitions` or `$defs` (JSON Schema 2019-09 and later), and are referenced with local `$ref`s like `#/$defs/Address`.

## Dependency
```xml
<dependency>
  <groupId>com.ethlo.jsons2xsd</groupId>
  <artifactId>jsons2xsd</artifactId>
  <version>2.3.0</version>
</dependency>
```
## Usage

The examples below are the tests in [`src/test/java/samples`](src/test/java/samples), copied into this file by the
build.

${samples.basic}
### Example input
```json
{
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "price": {
                "type": "number",
                "minimum": 0
            },
            "name": {
                "type": "string",
                "minLength": 5,
                "maxLength": 32
            },
            "isExpired": {
                "default": false,
                "type": "boolean"
            },
            "manufactured": {
            	"type": "string",
            	"format": "date-time"
           	}
       	},
       	"required": ["price", "name", "manufactured"]
    }
}
```

### Example output
```xml
<?xml version="1.0" encoding="UTF-8"?>
<schema elementFormDefault="qualified"
	targetNamespace="http://ethlo.com/schema/array-test-1.0.xsd" xmlns="http://www.w3.org/2001/XMLSchema"
	xmlns:x="http://ethlo.com/schema/array-test-1.0.xsd">
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

${samples.formats}
