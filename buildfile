require 'buildr/git_auto_version'
require 'buildr/custom_pom'
require 'buildr/gpg'

KEYCLOAK = %w(
    com.fasterxml.jackson.core:jackson-annotations:jar:2.6.3
    com.fasterxml.jackson.core:jackson-core:jar:2.6.3
    com.fasterxml.jackson.core:jackson-databind:jar:2.5.4
    com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:jar:2.6.3
    com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:jar:2.6.3
    com.fasterxml.jackson.module:jackson-module-jaxb-annotations:jar:2.6.3
    commons-codec:commons-codec:jar:1.9
    commons-io:commons-io:jar:2.1
    commons-logging:commons-logging:jar:1.2
    javax.activation:activation:jar:1.1.1
    net.jcip:jcip-annotations:jar:1.0
    org.apache.httpcomponents:httpclient:jar:4.5
    org.apache.httpcomponents:httpcore:jar:4.4.1
    org.bouncycastle:bcpkix-jdk15on:jar:1.52
    org.bouncycastle:bcprov-jdk15on:jar:1.52
    org.jboss.logging:jboss-logging:jar:3.1.4.GA
    org.jboss.resteasy:resteasy-client:jar:3.0.14.Final
    org.jboss.resteasy:resteasy-jackson2-provider:jar:3.0.14.Final
    org.jboss.resteasy:resteasy-jaxrs:jar:3.0.14.Final
    org.jboss.spec.javax.annotation:jboss-annotations-api_1.2_spec:jar:1.0.0.Final
    org.jboss.spec.javax.servlet:jboss-servlet-api_3.1_spec:jar:1.0.0.Final
    org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.0_spec:jar:1.0.0.Final
    org.keycloak:keycloak-admin-client:jar:2.2.0.Final
    org.keycloak:keycloak-client-registration-api:jar:2.2.0.Final
    org.keycloak:keycloak-common:jar:2.2.0.Final
    org.keycloak:keycloak-core:jar:2.2.0.Final
 )

PACKAGED_DEPS = KEYCLOAK + [:getopt4j, :javax_json_api, :javax_json_impl]

desc 'keycloak-converger: Update a keycloak relams list of clients.'
define 'keycloak-converger' do
  project.group = 'org.realityforge.keycloak.converger'

  compile.options.source = '1.7'
  compile.options.target = '1.7'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/keycloak-converger')
  pom.add_developer('realityforge', 'Peter Donald')

  compile.with PACKAGED_DEPS

  package(:jar).tap do |jar|
    PACKAGED_DEPS.each do |dep|
      jar.merge artifact(dep)
    end
  end
  package(:sources)
  package(:javadoc)
end
