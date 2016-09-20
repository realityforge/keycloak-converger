require 'buildr/git_auto_version'
require 'buildr/custom_pom'
require 'buildr/gpg'

KEYCLOAK = %w(org.keycloak:keycloak-client-registration-api:jar:2.2.0.Final
    org.keycloak:keycloak-core:jar:2.2.0.Final
    org.keycloak:keycloak-common:jar:2.2.0.Final
    org.bouncycastle:bcprov-jdk15on:jar:1.52
    org.bouncycastle:bcpkix-jdk15on:jar:1.52
    com.fasterxml.jackson.core:jackson-core:jar:2.5.4
    com.fasterxml.jackson.core:jackson-databind:jar:2.5.4
    com.fasterxml.jackson.core:jackson-annotations:jar:2.5.0
    org.apache.httpcomponents:httpclient:jar:4.5
    org.apache.httpcomponents:httpcore:jar:4.4.1
    commons-logging:commons-logging:jar:1.2
    commons-codec:commons-codec:jar:1.9
  )

PACKAGED_DEPS = KEYCLOAK + [:getopt4j]

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

  compile.with :getopt4j, KEYCLOAK

  package(:jar).tap do |jar|
    PACKAGED_DEPS.each do |dep|
      jar.merge artifact(dep)
    end
  end
  package(:sources)
  package(:javadoc)
end
