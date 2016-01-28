# Developer Notes

## Prerequisites

- [Gradle build system version 2.10 or later](http://gradle.org/)

## Compile

To compile:

    gradle assemble

To build documentation:

    gradle javadoc

To build all artifacts and publish to a local maven repository:

    gradle install

To publish to maven repository `signing.keyId`, `signing.password`, `signing.secretKeyRingFile`,
`ossrhUsername`, and `ossrhPassword` variables need to be defined in project-specific or
user-specific `gradle.properties` file.  After the variables defined, run the following command
to build, sign, and upload archives to maven:

    gradle uploadArchives

To get list of other targets, use `gradle tasks`.

## Tests

The package contains two types of tests: unit and integration.  The integration tests require
NFD instance to be running locally.

### Unit Tests

To run unit tests:

    gradle test

To run a specific test, use `-Dtest.single=<test-name>` command-line option. For example,

    gradle -Dtest.single=ControlResponseTest test

### Integration Tests

To run integration tests

    gradle integrationTest
