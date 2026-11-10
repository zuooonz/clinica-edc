# Using Containers for Development

It is possible to locally run a LibreClinica instance for development using [Docker Compose].

## Requirements

- [Docker] or [Podman]
- [Docker Compose]

## Preparation
The web module sets a stable WAR name for the Docker build. Dependency repositories
must be reachable from the build environment.

## Starting LibreClinica

Running the following command will start a LibreClinica instance:

```sh
docker compose up --build
```

If you use a `docker compose` version >= 2.25.0, you can use the [watch](https://docs.docker.com/compose/file-watch/) feature. This will rebuild and restart the container when you change the LibreClinica source or configuration files.

```sh
docker compose up --build --watch
```

When the container has been started, LibreClinica will be available at <http://localhost:8080>.

## Emails

LibreClinica is configured so that it does not actually send emails to the recipients. Instead, the emails are accessible at <http://localhost:1080>.

[Docker]: https://docs.docker.com/
[Docker Compose]: https://docs.docker.com/compose/
[Podman]: https://podman.io/

## Clinica EDC local data volumes

The Compose configuration uses project-scoped named volumes for PostgreSQL and
`/usr/local/tomcat/libreclinica.data`. This is a synthetic-data development setup,
not a production deployment: the sample database credentials are public and the
image tags are mutable. The ports bind to loopback; keep it isolated.

Existing installations that used anonymous volumes are **not migrated** by this
change. Before recreating any existing container, inspect its mounts and back up
both its database and application files. Do not run `docker compose down -v`
unless you intend to delete the project's named volumes. External paths configured
for attachments must also be included in backups.

For a consistent backup, stop application writes and background jobs, then take a
PostgreSQL dump and a matching copy of the application data and configuration.
Store backups outside the repository, protect them as sensitive data, and verify a
restore in a separate isolated project. A database dump alone is not a complete
EDC backup. Upgrades and rollback require a validated, matching database and file
snapshot; no upgrade or restore procedure has yet been validated for this setup.
