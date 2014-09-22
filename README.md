fcrepo-webapp-plus
==================

Fcrepo4 webapp plus optional fcrepo dependencies.  This project builds custom-configured
fcrepo4 webapp war files that include extra dependencies and configuration options.  An
integration test exists to perform a basic deployment test only and may be useful just for
identifying syntax errors in configuration file updates or third party library version
incompatibilities.

# Role-Base Access Control Lists

The default maven build profile, these configuration files are found in src/rbacl.
```
mvn install
```

# XACMLE-based Access Control
An alternative maven build profile, these configuration files are found in src/xacml.
```
mvn install -P xacml
```
