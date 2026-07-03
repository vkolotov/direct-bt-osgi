# Direct-BT OSGi wrapper

Turns the published Direct-BT Java library into an OSGi bundle for openHAB (and other OSGi runtimes).

This is layer 2 of a two-layer setup:

1. Upstream (sgothel/direct_bt) publishes the plain library `org.direct_bt:direct-bt` to a Maven
   repository (the fat jar with the Java API and platform natives). See the `maven/` module in that
   repo.
2. This wrapper depends on that artifact, embeds it (Java classes + `natives/linux-amd64/*.so`), and
   adds the OSGi manifest — including a `Bundle-NativeCode` header — plus a small `BundleActivator`.
   Output is `org.direct_bt:direct-bt-osgi`.

The openHAB Direct-BT binding references the wrapper output as
`mvn:org.direct_bt/direct-bt-osgi/<version>`.

## How the natives are loaded

The native `.so`s are carried in the bundle and declared in the `Bundle-NativeCode` manifest header.
The OSGi framework handles extraction: on `System.loadLibrary(basename)` it unpacks the matching library
from the bundle to its private cache and serves it via the bundle classloader's `findLibrary` hook. There
is no runtime hand-extraction and no build-time repacking of the natives.

The `BundleActivator` (`DirectBTActivator`) does two small things:

- sets `jau.pkg.UseTempJarCache=false`. jaulib's default loader (`TempJarCache`) locates its natives by
  scanning a flat classpath, which does not exist in OSGi (each bundle is its own classloader). With it
  off, Direct-BT's `JNILibrary.loadLibraryImpl` uses plain `System.loadLibrary(name)` — the mode
  `Bundle-NativeCode` services.
- publishes a `DirectBTNativeLibraryProvider` marker service once it has run, so the binding (which
  `@Reference`s it) does not activate before the loader is configured.

The actual load is triggered later by Direct-BT's `BTFactory.initLibrary`, through `BTFactory.class`'s
classloader — this (lib) bundle — so the JNI stays bound to a bundle that is never hot-swapped. That is
what lets the openHAB binding bundle be refreshed (hot-swapped) without a full restart.

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
