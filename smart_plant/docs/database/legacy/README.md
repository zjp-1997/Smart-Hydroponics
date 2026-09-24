# Legacy database scripts

These files are retained only as historical references for databases created before
Flyway adoption. They are outside `src/main/resources`, are not packaged into the
application, and must not be exposed by a web server.

New installations and upgrades must use the versioned migrations under
`src/main/resources/db/migration`. Do not execute these scripts as a complete setup
procedure. In particular, administrator accounts must be provisioned through an
approved operational process with a unique high-entropy password; no shared default
administrator credential is supplied by this repository.
