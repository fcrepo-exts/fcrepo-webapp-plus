fcrepo-webapp-plus
==================


[![Build Status](https://travis-ci.org/fcrepo4-exts/fcrepo-webapp-plus.png?branch=master)](https://travis-ci.org/fcrepo4-exts/fcrepo-webapp-plus)

Fcrepo4 webapp plus optional fcrepo dependencies.  This project builds a custom-configured
fcrepo4 webapp war file that includes extra dependencies and configuration options.  An
integration test exists to perform a basic deployment test only and may be useful just for
identifying syntax errors in configuration file updates or third party library version
incompatibilities.

# Profiles

## Default Maven Build
The default maven build profile does not include audit or authorization support.

```
mvn install
```
## Web Access Control

This maven build profile bundles WebAC authorization module to the fcrepo webapp. The configuration files are found in src/webac.

```
mvn install -P webac
```

There is also a [Quick Start with WebAC guide](https://wiki.duraspace.org/display/FEDORA4x/Quick+Start+with+WebAC) on the Fedora 4 wiki that guides you through the basic steps of creating and updating WebAC access control lists, and protecting resources with those ACLs.

# Configuring for your use

The fcrepo-webapp-plus includes a single spring XML configuration file `fcrepo-config.xml`, it is suggested to make a copy of this file and use the system property `fcrepo.spring.configuration` to point to your customized version.

`JAVA_OPTS="${JAVA_OPTS} -Dfcrepo.spring.configuration=file:/path/to/fcrepo-config.xml"`

You must also specify the `fcrepo.modeshape.configuration` system property to point to a valid respository configuration file. You can find several example [repository.json files here](https://github.com/fcrepo4/fcrepo4/tree/master/fcrepo-configs/src/main/resources/config)

# Authentication Packages

Basic Authentication is configured for the **webac** profile only at this time.  To choose a different
method, update the same web.xml deployment descriptor for the webapp in question, being aware
that this may break the single integration test.

You must also configure the authorization package as described below.


# Audit Capability Package
The [fcrepo-audit](https://github.com/fcrepo4-exts/fcrepo-audit) capability is included in fcrepo-webapp-plus by default.

You must enable it (un-comment it) in your `fcrepo-config.xml` file.

```
    <!-- **************************
                 AUDIT
         publish audit events to JMS
         ************************** -->
    <!--
    <bean class="org.fcrepo.audit.InternalAuditor"/>
    -->
```

## Audit capability with Authentication

To achieve this functionality, simply enable the form of authorization you prefer and also include the audit capability in the same `fcrepo-config.xml`.

Audit capability can be packaged with any of the authentication options.


## Maintainers

* [Andrew Woods](https://github.com/awoods)
