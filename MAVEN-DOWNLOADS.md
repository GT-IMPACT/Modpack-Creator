# Maven downloads

The creator reads every Nexus search page using continuationToken, preserving
version-descending order when selecting the latest version of each configured
artifact. It requires an unclassified runtime JAR; dev, sources and javadoc
artifacts cannot replace the mod JAR.

All names in mavenMods must resolve. Search, download, incomplete transfer,
invalid JAR and checksum errors propagate and stop the build. External mods in
autoUpdateMods use the same checked download helper. Version generation and
client/server packaging start only after all downloads finish successfully.

Downloads use temporary .part files and replace the destination only after
validation. Existing cached JARs are checked before reuse. Nexus SHA-256 or
SHA-1 is verified when supplied, and JAR entries are read and CRC-checked.

Run offline/mock regression tests with `gradlew test`. Run the explicit real
Nexus smoke check with `gradlew test -PmavenSmoke=true`: it uses the configured
mavenBaseUrl and MAVEN_USER/MAVEN_PASSWORD, downloads only ImpactAPI into a
temporary directory and checks the alignment interface. It does not create or
modify a modpack. Credentials are never embedded in tests.

Validation: 15 tests passed, including the opt-in Nexus check that downloaded
ImpactAPI 0.0.5 and found
space/impact/api/multiblocks/alignment/IAlignmentProvider.class.
Build executable artifact with `gradlew shadowJar`.
