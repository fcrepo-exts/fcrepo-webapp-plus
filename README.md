fcrepo-webapp-plus
==================

Fcrepo4 webapp plus optional fcrepo dependencies.  This project builds custom-configured
fcrepo4 webapp war files that include extra dependencies and configuration options.  An
integration test exists to perform a basic deployment test only and may be useful just for
identifying syntax errors in configuration file updates or third party library version
incompatibilities.

# Authentication Packages

Basic Authentication is configured for both profiles at this time.  To choose a different
method, update the web.xml deployment descriptor for the webapp in question, being aware
that this may break the single integration test.

## Role-Base Access Control Lists

The default maven build profile, these configuration files are found in src/rbacl.
```
mvn install
```

## XACML-based Access Control
An alternative maven build profile, these configuration files are found in src/xacml.

Default policy sets and root policy are extracted into target/policies for the integration
tests, but when you create a custom war file, you should update the repo.xml Spring
configuration to point to your own policy directories.

```
mvn install -P xacml
```

# Audit Capability Package
This profile builds webapp that includes the [fcrepo-audit](https://github.com/fcrepo4-labs/fcrepo-audit) module that provides internal auditing capability.

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

