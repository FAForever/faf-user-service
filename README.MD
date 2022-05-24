# FAF User Service

This service aims to cover the domain of login and account management in FAForever.

## Technology stack

- Kotlin
- Micronaut
- R2DBC
- Thymeleaf


## Motivation and architecture considerations

### Yet another FAF API?

> The faf-java-api already offers OAuth2 login and user management. Why do we need yet another service?

There are multiple reasons that led to this decision:

1. **Application perspective:** The faf-java-api uses the 
   [Spring Security OAuth](https://spring.io/projects/spring-security-oauth) library.
   1. With the release of Spring 5 this got deprecated in favour of a newer one. Unfortunately with this transition
   the support for OAuth2 identity server was dropped completely. It receives no more updates.
   1. Since the library is deprecated we lack support for the improved OAuth2 PKCE login flow, which improves overall
    security compared to the previous implicit flow.
   1. The current library seems to have issues when trying to login with certified libraries from the Angular ecosystem.
    It just doesn't work in some cases, where we need it to work. (But nobody will fix it since it's deprecated.)
2. **Architecture perspective:** The faf-java-api is the FAF swiss army knife. It basically bundles every feature 
   outside of the lobby server protocol. This makes it very complex to maintain and configure. It also causes very high 
   startup times causing unnecessary downtimes on deployments. This does not match our desired architecture.
   A new microservice focussing on one particular topic (and security is a very important topic which is also hard to get 
   right) simplifies that.
3. **GDPR and DevOps implications:** Currently FAF runs almost all applications on one server. An admin on that server 
   has access to all personal data. Adding new admins is a large hassle due to GDPR requirements. Due to this many
   FAF maintainers have no access to their application logs and configuration, which makes fixing bugs etc. much more 
   complicated and adds additional work onto the few admins. This new service might
   be a first step into moving the whole account management out of the main server.
4. **Long running perspective:** In a perfect world we would migrate all authorization related stuff into a dedicated 
   (trusted) 3rd party software, so we can't mess up on security.

### Additional goals

Goal | Status
---- | ------
Usability improvements by serving translated web pages | :heavy_check_mark:
Improved performance by using reactive stack | :heavy_check_mark:
Massively reduced startup times and smaller resource footprint by using Micronaut | :heavy_check_mark:	
Even less startup times and smaller resource footprint by compiling to native images with GraalVM | :hourglass:	
