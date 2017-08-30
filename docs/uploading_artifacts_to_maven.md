# Uploading artifacts to maven

1. On a clean machine follow [the guide](http://blog.sonatype.com/2010/01/how-to-generate-pgp-signatures-with-maven/) to setup GPG and the key.
2. Download a version of jcodec you want to build

```bash
    git clone https://github.com/jcodec/jcodec.git
```
3. Go inside jcodec folder and execute:

```bash
    mvn clean install
    mvn javadoc:jar gpg:sign
    mvn source:jar gpg:sign
```
4. Go to https://oss.sonatype.org/
4.1 Log in.
4.2 Go to 'Staging Upload'.
4.3  Under Upload Mode select 'Artifact(s) with a POM'.
4.4  Select 'pom' from jcodec/target.
4.5  In Select Artifacts to upload upload the artifacts ( press Add Artifact for each file).
4.6 Fill in 'Description' field.
4.7 Then press 'Upload artifacts'.
4.8 Click on 'Staging Repositories' and find jcodec, it's normally the last one in the list.
4.9 Verify that everything is fine and that the repository is closed ( no errors found ),  check the 'Activity' tab. If something is wrong, fix it ( jcodecproject@gmail.com ).
4.10 Click on 'Release' on the top of the page.
4.11 Fill in the drescription, press the button, it should say 'Repository is released'.
4.12 These are the artifacts:

```
  jcodec-<version>.pom.asc
  jcodec-<version>.jar
  jcodec-<version>.jar.asc
  jcodec-<version>-javadoc.jar
  jcodec-<version>-javadoc.jar.asc
  jcodec-<version>-sources.jar
  jcodec-<version>-sources.jar.asc
```
5. The changes will be synced to main maven within a day (most likely immediately).