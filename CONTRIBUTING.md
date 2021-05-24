# Contributing

## Develop

#### 1. Fork it

#### 2. Build the project

```shell

mvn compile

```

#### 3. Make a change

fix a bug, add a feature, update the doc, etc

#### 4. Run the Tests

```shell

mvn test

```

#### 5. Create a PR

## Misc

#### Add yourself as a contributor

After your PR has been merged, add yourself as a contributor.

To do sob, create a comment like the following on your PR:

@all-contributors please add @username for code and test!

Replace code with doc or test or infra or some combination depending on your contribution.

#### Package

Contributors are not responsible for packaging, however should check that it succeeds

```shell

mvn clean package dokka:javadocJar

```

### Deploy

Contributors are not responsible for deploying to mavencentral.

```shell

mvn clean dokka:javadocJar deploy

```

**Maven Central**

- Publish with Maven - https://central.sonatype.org/publish/publish-maven/
- GPG Setup - https://central.sonatype.org/publish/requirements/gpg/

To publish a gpg key:

```shell
gpg --send-keys 5BE1414D5EAF81B48F2E77E1999F818C080AF9C1
````

where `5BE1414D5EAF81B48F2E77E1999F818C080AF9C1` is the public key


```shell
mvn clean dokka:javadocJar deploy
```
