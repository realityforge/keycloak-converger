## 1.4:

* Add verbose info message indicating whether unknown clients are deleted.
* Cleanup default parameters.
* Replace all UUIDs during upload to avoid constraint violations in keycloak server.

## 1.3:

* Add `--standard-unmanaged-clients` to add the standard set of unmaanged clients.
* Require explicit argument to delete unmatched clients.

## 1.2:

* Add `Main-Class` to manifest.
* Fix the merging of dependencies to ensure the jar runs. This
  required the removal of class signatures and explicit merging
  of some common service files.
* Remove unnecessary dependencies.

## 1.1:

* Initial release
