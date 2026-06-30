# Direct-BT OSGi wrapper

Turns the published Direct-BT Java library into an OSGi bundle for openHAB (and other OSGi runtimes).

This is layer 2 of a two-layer setup:

1. Upstream (sgothel/direct_bt) publishes the plain library `org.direct_bt:direct-bt` to a Maven
   repository (the fat jar with the Java API and platform natives). See the `maven/` module in that
   repo.
2. This wrapper depends on that artifact, embeds it, and adds the OSGi manifest plus a
   `BundleActivator` that extracts the natives onto `java.library.path` at start. Output is
   `org.direct_bt:direct-bt-osgi`.

The wrapper also adds the native SONAME filenames required by the dynamic linker:
`libjaulib.so.1` and `libdirect_bt.so.3`. They are generated during the Maven build from the
plain `org.direct_bt:direct-bt` artifact, so no runtime symlink rule or manual post-processing is
needed.

The openHAB Direct-BT binding references the wrapper output as
`mvn:org.direct_bt/direct-bt-osgi/<version>`.

## Build

Once `org.direct_bt:direct-bt` is available (published, or installed locally):

```
mvn -f pom.xml clean install
```

Until upstream publishes it, install the dependency locally first from the direct_bt repo:

```
# in the direct_bt checkout, after building the fat jar target:
maven/publish.sh install
```

For the current openHAB Direct-BT work, that locally installed `org.direct_bt:direct-bt` artifact
should be built from the patched native fork until the corresponding upstream PRs are merged.

## Why a separate wrapper

Keeping the OSGi packaging here, separate from the upstream library, means upstream only has to
publish a normal Maven jar. The OSGi specifics (activator, manifest, native extraction) live with the
consumer that needs them.
