/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.direct_bt.osgi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * OSGi {@link BundleActivator} for the re-manifested Direct-BT fat jar.
 * <p>
 * On bundle start it extracts the Direct-BT / jaulib native libraries that are bundled in THIS jar (under
 * {@code natives/<arch>/}) onto the JVM's {@code java.library.path}, so Direct-BT's own loader
 * ({@code org.jau.sys.JNILibrary} / {@code org.direct_bt.PlatformToolkit}) finds and {@code System.load}s
 * them by basename. It disables the jau {@code TempJarCache} loader (unreliable in OSGi).
 * <p>
 * Owning native extraction here (in the lib bundle that is never hot-swapped) instead of in the binding
 * bundle is what makes the binding hot-swappable: the JNI library is bound to the classloader of the class
 * that calls {@code System.load} — always a lib-jar class — so it survives a binding-bundle refresh. We do
 * NOT {@code System.load} by absolute path here (that would not satisfy Direct-BT's later load-by-basename
 * and yields {@code UnsatisfiedLinkError}); we only place the files where Direct-BT's loader looks.
 */
public final class DirectBTActivator implements BundleActivator {

    // Dependency order: base lib and SONAME alias first, then JNI shims, direct_bt and its alias, then JNI binding.
    private static final String[] LIBS = { "libjaulib.so", "libjaulib.so.1", "libjaulib_pkg_jni.so",
            "libjaulib_jni_jni.so", "libdirect_bt.so", "libdirect_bt.so.3", "libjavadirect_bt.so" };

    @Override
    public void start(BundleContext context) throws Exception {
        // Tell jau's loader not to use its TempJarCache (can't locate its jar in OSGi); load from
        // java.library.path instead, where we extract below.
        System.setProperty("jau.pkg.UseTempJarCache", "false");
        String arch = getNativeArch();
        Path libDir = resolveLibraryPathDir();
        Files.createDirectories(libDir);
        ClassLoader cl = DirectBTActivator.class.getClassLoader();
        for (String lib : LIBS) {
            String resource = "natives/" + arch + "/" + lib;
            try (InputStream in = cl.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IOException("Bundled native library not found in lib jar: " + resource);
                }
                Path target = libDir.resolve(lib);
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[direct-bt] Extracted native library " + target);
            }
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Leave the extracted libs + loaded JNI in place: the native BTManager is a process singleton and the
        // libraries cannot be unloaded; a binding refresh re-acquires them.
    }

    /**
     * @return a WRITABLE directory on {@code java.library.path}; the first entry may be a non-writable system
     *         dir, so pick the first writable one, fall back to {@code java.io.tmpdir}.
     */
    private static Path resolveLibraryPathDir() {
        String libPath = System.getProperty("java.library.path");
        if (libPath != null) {
            for (String entry : libPath.split(File.pathSeparator)) {
                if (!entry.isBlank()) {
                    File dir = new File(entry);
                    if ((dir.isDirectory() && dir.canWrite()) || (!dir.exists() && canCreate(dir))) {
                        return dir.toPath();
                    }
                }
            }
        }
        String tmp = System.getProperty("java.io.tmpdir");
        return new File(tmp != null ? tmp : "/tmp").toPath();
    }

    private static boolean canCreate(File dir) {
        File parent = dir.getParentFile();
        return parent != null && parent.isDirectory() && parent.canWrite();
    }

    private static String getNativeArch() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (!os.startsWith("linux")) {
            throw new UnsupportedOperationException("Direct-BT supports Linux only, found: " + os);
        }
        if (arch.equals("amd64") || arch.equals("x86_64")) {
            return "linux-amd64";
        }
        throw new UnsupportedOperationException(
                "No bundled Direct-BT natives for architecture '" + arch + "' (only linux-amd64 is bundled)");
    }
}
