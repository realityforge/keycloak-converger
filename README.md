# keycloak-converger

[![Build Status](https://secure.travis-ci.org/realityforge/keycloak-converger.svg?branch=master)](http://travis-ci.org/realityforge/keycloak-converger)
[<img src="https://img.shields.io/maven-central/v/org.realityforge.keycloak.converger/keycloak-converger.svg?label=latest%20release"/>](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.realityforge.keycloak.converger%22%20a%3A%22keycloak-converger%22)

This command line tool converges a keycloak realm to ensure it contains the required clients.
The set of clients are based on set of keycloak json configuration files contained in a
directory. The keycloak configuration may have handlebars replacement sections inside it to
make it easy to customize the templates for the environment.

All clients that appear in the directory will be created (if not present in realm) or updated
(if alreayd in the realm). If a client does not exist in client directory and has not been
specified as an "unmanaged-client" then it will be deleted.

The easiest way to see the options for the command is to pass the `--help` parameter to the
command via:

    java -jar keycloak-converger.jar --help

However, a typical command that converges a default looking keycloak realm

    java -jar keycloak-converger.jar \
              -d config \
              --admin-password=secret \
              --server-url=https://id.example.com/ \
              --realm-name=MyRealm \
              --unmanaged-client=admin-cli \
              --unmanaged-client=account \
              --unmanaged-client=broker \
              --unmanaged-client=realm-management \
              --unmanaged-client=security-admin-console
