require 'buildr/git_auto_version'
require 'buildr/gpg'

KEYCLOAK = %w(
    org.keycloak:keycloak-admin-client:jar:11.0.0
    org.keycloak:keycloak-core:jar:11.0.0
    org.keycloak:keycloak-common:jar:11.0.0
    org.jboss.resteasy:resteasy-client:jar:3.9.1.Final
    org.jboss.spec.javax.ws.rs:jboss-jaxrs-api_2.1_spec:jar:2.0.1.Final
    org.jboss.resteasy:resteasy-jaxrs:jar:3.12.1.Final
    org.reactivestreams:reactive-streams:jar:1.0.3
    jakarta.validation:jakarta.validation-api:jar:2.0.2
    org.jboss.spec.javax.annotation:jboss-annotations-api_1.3_spec:jar:2.0.1.Final
    com.sun.activation:jakarta.activation:jar:1.2.1
    commons-io:commons-io:jar:2.6
    com.github.stephenc.jcip:jcip-annotations:jar:1.0-1
    org.jboss.logging:jboss-logging:jar:3.4.1.Final
    org.apache.httpcomponents:httpclient:jar:4.5.12
    org.apache.httpcomponents:httpcore:jar:4.4.13
    commons-logging:commons-logging:jar:1.2
    commons-codec:commons-codec:jar:1.11
    org.jboss.resteasy:resteasy-multipart-provider:jar:3.9.1.Final
    com.sun.mail:javax.mail:jar:1.6.2
    javax.activation:activation:jar:1.1
    org.apache.james:apache-mime4j:jar:0.6
    org.jboss.resteasy:resteasy-jackson2-provider:jar:3.9.1.Final
    com.fasterxml.jackson.core:jackson-core:jar:2.10.4
    com.fasterxml.jackson.core:jackson-databind:jar:2.10.4
    com.fasterxml.jackson.core:jackson-annotations:jar:2.10.4
    com.fasterxml.jackson.jaxrs:jackson-jaxrs-json-provider:jar:2.10.4
    com.fasterxml.jackson.jaxrs:jackson-jaxrs-base:jar:2.10.4
    com.fasterxml.jackson.module:jackson-module-jaxb-annotations:jar:2.10.4
    jakarta.xml.bind:jakarta.xml.bind-api:jar:2.3.2
    jakarta.activation:jakarta.activation-api:jar:1.2.1
    com.github.fge:json-patch:jar:1.9
    com.github.fge:jackson-coreutils:jar:1.6
    com.github.fge:msg-simple:jar:1.1
    com.github.fge:btf:jar:1.2
    com.google.guava:guava:jar:25.0-jre
    org.jboss.resteasy:resteasy-jaxb-provider:jar:3.9.1.Final
    org.jboss.spec.javax.xml.bind:jboss-jaxb-api_2.3_spec:jar:2.0.0.Final
    org.glassfish.jaxb:jaxb-runtime:jar:2.3.1
    org.glassfish.jaxb:txw2:jar:2.3.1
    com.sun.istack:istack-commons-runtime:jar:3.0.10
    org.jvnet.staxex:stax-ex:jar:1.8
    com.sun.xml.fastinfoset:FastInfoset:jar:1.2.15
 )

PACKAGED_DEPS = KEYCLOAK + [:getopt4j, :javax_json_impl]

desc 'keycloak-converger: Converge the state of a keycloak realm'
define 'keycloak-converger' do
  project.group = 'org.realityforge.keycloak.converger'

  compile.options.source = '1.8'
  compile.options.target = '1.8'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/keycloak-converger')
  pom.add_developer('realityforge', 'Peter Donald')

  compile.with PACKAGED_DEPS

  package(:jar).tap do |jar|
    jar.manifest['Main-Class'] = 'org.realityforge.keycloak.converger.Main'
    PACKAGED_DEPS.each do |dep|
      jar.merge(artifact(dep)).
        exclude('META-INF/MANIFEST.MF').
        exclude('META-INF/services/javax.ws.rs.ext.Providers').
        exclude('META-INF/maven/*').
        exclude('META-INF/INDEX.LIST').
        exclude('META-INF/LICENSE*').
        exclude('META-INF/DEPENDENCIES').
        exclude('META-INF/NOTICE*').
        exclude('META-INF/BC*')
    end
  end
  package(:sources)
  package(:javadoc)
end
