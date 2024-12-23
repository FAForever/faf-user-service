# FAF User Service

This service aims to cover the domain of login and account management in FAForever:
* OAuth 2.0 / OIDC login backend for Ory Hydra
* IRC login for Ergochat
* User Control Panel with manual login

## Developer information

### Technology stack

- Kotlin
- Quarkus
- Hibernate (Panache)
- Vaadin

### Dependencies

The FAF user service requires
* A [MariaDB](https://mariadb.org/) server running a matching version of the [faf-db](https://github.com/FAForever/db). (Latest develop should always work)
* An [Ory Hydra](https://github.com/ory/hydra) server running in a matching version

### Developer setup

For local development you need
* a working Kotlin setup (e.g. install IntelliJ Community and download the required JDK from IntellIJ directly)
* Docker Compose

Setup all docker depencies along with test data by simply running:
```shell
docker compose up -d
```
This will insert the faf db testdata into the faf-db and a faf-client OAuhth client into Ory Hydra.


Hint: The docker compose stack is **not** persisting to disk. Deleting containers manually or via `docker compose down` will wipe all data.

## Architecture considerations

### Yet another FAF API?

1. **Architecture perspective:** The faf-java-api is the FAF swiss army knife. It basically bundles every feature 
   outside of the lobby server protocol. This makes it very complex to maintain and configure. It also causes very high 
   startup times causing unnecessary downtimes on deployments. This does not match our desired architecture.
   A new microservice focussing on one particular topic (and security is a very important topic which is also hard to get 
   right) simplifies that.
1. **GDPR and DevOps implications:** Currently FAF runs almost all applications on one server. An admin on that server 
   has access to all personal data. Adding new admins is a large hassle due to GDPR requirements. Due to this many
   FAF maintainers have no access to their application logs and configuration, which makes fixing bugs etc. much more 
   complicated and adds additional work onto the few admins. This new service might
   be a first step into moving the whole account management out of the main server.
1. **Long running perspective:** In a perfect world we would migrate all authorization related stuff into a dedicated 
   (trusted) 3rd party software, so we can't mess up on security.

### Additional goals

Goal | Status
---- | ------
Usability improvements by serving translated web pages | :heavy_check_mark:
Massively reduced startup times and smaller resource footprint by using Quarkus | :heavy_check_mark:	
Even less startup times and smaller resource footprint by compiling to native images with GraalVM | :hourglass:	
