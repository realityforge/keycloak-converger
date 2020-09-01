# Change Log

### [v1.10](https://github.com/realityforge/keycloak-converger/tree/v1.10) (2020-09-01) · [Full Changelog](https://github.com/realityforge/keycloak-converger/compare/v1.9...v1.10)

* Save the client secrets as part of the converge process.

### [v1.9](https://github.com/realityforge/keycloak-converger/tree/v1.9) (2020-08-18) · [Full Changelog](https://github.com/realityforge/keycloak-converger/compare/v1.8...v1.9)

* Upgrade the `org.realityforge.getopt4j` artifact to version `1.3`.
* Automate the release process.
* Upgrade keycloak libraries to `11.0.0`

### [v1.8](https://github.com/realityforge/keycloak-converger/tree/v1.8) · [Full Changelog](https://github.com/realityforge/keycloak-converger/compare/v1.7...v1.8)

* Upgrade keycloak libraries to `5.0.0`

### [v1.7](https://github.com/realityforge/keycloak-converger/tree/v1.7) · [Full Changelog](https://github.com/realityforge/keycloak-converger/compare/v1.4...v1.7)

* Upgrade keycloak libraries to 3.4.3-final

### [v1.4](https://github.com/realityforge/keycloak-converger/tree/v1.4) · [Full Changelog](https://github.com/realityforge/keycloak-converger/compare/v1.3...v1.4)

* Add verbose info message indicating whether unknown clients are deleted.
* Cleanup default parameters.
* Replace all UUIDs during upload to avoid constraint violations in keycloak server.

### [v1.3](https://github.com/realityforge/keycloak-converger/tree/v1.3) · [Full Changelog](https://github.com/realityforge/keycloak-converger/compare/v1.2...v1.3)

* Add `--standard-unmanaged-clients` to add the standard set of unmaanged clients.
* Require explicit argument to delete unmatched clients.

### [v1.2](https://github.com/realityforge/keycloak-converger/tree/v1.2) · [Full Changelog](https://github.com/realityforge/keycloak-converger/compare/v1.1...v1.2)

* Add `Main-Class` to manifest.
* Fix the merging of dependencies to ensure the jar runs. This
  required the removal of class signatures and explicit merging
  of some common service files.
* Remove unnecessary dependencies.

### [v1.1](https://github.com/realityforge/keycloak-converger/tree/v1.1) · [Full Changelog](https://github.com/realityforge/keycloak-converger/compare/ebccfb241af122a25ab18f9e8fd759da4feb47ab...v1.1)

* Initial release
