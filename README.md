fcrepo-webapp-plus
==================


[![Build Status](https://travis-ci.org/fcrepo4-exts/fcrepo-webapp-plus.png?branch=master)](https://travis-ci.org/fcrepo4-exts/fcrepo-webapp-plus)

Fcrepo4 webapp plus optional fcrepo dependencies.  This project builds a custom-configured
fcrepo4 webapp war file that includes extra dependencies and configuration options.  An
integration test exists to perform a basic deployment test only and may be useful just for
identifying syntax errors in configuration file updates or third party library version
incompatibilities.

# Authentication Packages

Basic Authentication is configured for both profiles at this time.  To choose a different
method, update the web.xml deployment descriptor for the webapp in question, being aware
that this may break the single integration test.

## Default Maven Build
The default maven build profile is Audit capability with WebAC
```
mvn install
```

## Role-Based Access Control Lists

This maven build profile bundles the Role-Based access control module to the fcrepo webapp. The configuration files are found in src/rbacl.
```
mvn install -P rbacl
```

## Web Access Control

This maven build profile bundles WebAC authorization module to the fcrepo webapp. The configuration files are found in src/webac.
```
mvn install -P webac
```

There is also a [Quick Start with WebAC guide](https://wiki.duraspace.org/display/FEDORA4x/Quick+Start+with+WebAC) on the Fedora 4 wiki that guides you through the basic steps of creating and updating WebAC access control lists, and protecting resources with those ACLs.

## XACML-based Access Control
An alternative maven build profile, these configuration files are found in src/xacml.

Default policy sets and root policy are extracted into target/policies for the integration
tests, but when you create a custom war file, you should update the repo.xml Spring
configuration to point to your own policy directories.

```
mvn install -P xacml
```

# Audit Capability Package
This profile builds webapp that includes the [fcrepo-audit](https://github.com/fcrepo4-exts/fcrepo-audit) module that provides internal auditing capability.

```
mvn install -P audit
```



## Audit capability with Authentication
Audit capability can be packaged with either of the authentication options by using the ```audit``` profile in conjunction with ```rbacl``` or ```xacml``` profiles.

#### Audit capability with RBACL

```
mvn install -P audit,rbacl
```


#### Audit capability with XACML

```
mvn install -P audit,xacml
```



#### Audit capability with WebAC

```
mvn install -P audit,webac
```

## Filesystem Federation (projection) Capability Package
This profile builds the webapp that includes the [fcrepo-connector-file](https://github.com/fcrepo4-exts/fcrepo-connector-file) module that provides the ability to serve static files from a mapped filesystem through the Fedora repository.

```
mvn install -P connector
```

#### Filesystem federation capability with WebAC
```
mvn install -P webac,connector
```

#### Filesystem federation capability with Audit
```
mvn install -P audit,connector
```

#### Filesystem federation capability with WebAC and Audit
```
mvn install -P webac,audit,connector
```

## Maintainers

* [Andrew Woods](https://github.com/awoods)
