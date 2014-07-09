nuxeo-scim-server
=================

# About

This Nuxeo plugin provides an implementation of the [SCIM](http://www.simplecloud.info/) 1.1 API.

The goal is to allow a third party IDM (ex: Okta, OneLogin, PingIdentity ...) to provision Users and Groups directly inside Nuxeo.

# Implementation

The implemenation is based on [SCIMSDK](https://code.google.com/p/scimsdk/) but the JAX-RS part relies on WebEngine/JAX-RS stack that is integrated inside Nuxeo Platform rather than on Apache Wink.

# Building

    mvn clean install

# Status

WIP


