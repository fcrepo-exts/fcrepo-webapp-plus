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

The fcrepo-webapp-plus includes a single spring XML configuration file `configuration.xml`, it is suggested to make a copy of this file and use the system property `fcrepo.spring.configuration` to point to your customized version.

You must also specify the `fcrepo.modeshape.configuration` system property to point to a valid respository configuration file. You can find several example [repository.json files here](https://github.com/fcrepo4/fcrepo4/tree/master/fcrepo-configs/src/main/resources/config)

# Authentication Packages

Basic Authentication is configured for the **webac** profile only at this time.  To choose a different
method, update the same web.xml deployment descriptor for the webapp in question, being aware
that this may break the single integration test.

You must also configure the authorization package as described below.

## Role-Based Access Control Lists

Ensure you have the basic authentication enabled in the web.xml. 

Then comment out the WebAC beans in the configuration.xml and un-comment the RbAcl beans in your `configuration.xml` file.

```
    <!-- **** WebAC Authentication **** -->
    <!--
      <bean name="fad" class="org.fcrepo.auth.webac.WebACAuthorizationDelegate"/>
      <bean name="accessRolesProvider" class="org.fcrepo.auth.webac.WebACRolesProvider"/>
    -->
    <!-- **** Roles Based Authentication **** -->
      <bean name="accessRolesResources" class="org.fcrepo.auth.roles.common.AccessRolesResources"/>
      <bean name="fad" class="org.fcrepo.auth.roles.basic.BasicRolesAuthorizationDelegate"/>

```

You will also need to include/un-comment the `fcrepo-module-auth-rbacl` artifact dependency in the pom.xml.

## XACML-based Access Control

Ensure you have the basic authentication enabled in the web.xml. 

Default policy sets and root policy are extracted into target/policies for the integration
tests, but when you create a custom war file, you should update the repo.xml Spring
configuration to point to your own policy directories.

You must also comment out the WebAC beans and un-comment the XACML ones.

```
    <!-- **** WebAC Authentication **** -->
    <!--
      <bean name="fad" class="org.fcrepo.auth.webac.WebACAuthorizationDelegate"/>
      <bean name="accessRolesProvider" class="org.fcrepo.auth.webac.WebACRolesProvider"/>
    -->
    <!-- **** XACML Authentication **** -->
      <bean name="accessRolesResources" class="org.fcrepo.auth.roles.common.AccessRolesResources"/>
      <bean class="org.fcrepo.auth.xacml.XACMLWorkspaceInitializer" init-method="initTest">
        <constructor-arg value="WEB-INF/classes/policies"/>
        <constructor-arg value="WEB-INF/classes/policies/GlobalRolesPolicySet.xml"/>
      </bean>

```

You will also need to include/un-comment the `fcrepo-module-auth-xacml` artifact dependency in the pom.xml.

# Audit Capability Package
The [fcrepo-audit](https://github.com/fcrepo4-exts/fcrepo-audit) capability is included in fcrepo-webapp-plus by default.

You must enable it (un-comment it) in your `configuration.xml` file.

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

To achieve this functionality, simply enable the form of authorization you prefer and also include the audit capability in the same `configuration.xml`.

Audit capability can be packaged with any of the authentication options.


## Maintainers

* [Andrew Woods](https://github.com/awoods)
