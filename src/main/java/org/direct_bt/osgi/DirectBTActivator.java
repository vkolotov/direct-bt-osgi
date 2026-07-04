/*
 * Copyright (c) 2026 Vlad Kolotov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * See the NOTICE file distributed with this work for additional information,
 * including attribution for the embedded Direct-BT / jaulib libraries.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.direct_bt.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * OSGi {@link BundleActivator} for the re-manifested Direct-BT fat jar.
 * <p>
 * The native libraries are carried in this bundle under {@code natives/<arch>/} and declared in the
 * {@code Bundle-NativeCode} manifest header. The OSGi framework unpacks the matching library from the
 * bundle and serves it to {@code System.loadLibrary(basename)} via the bundle classloader's
 * {@code findLibrary} hook — so this activator does no native extraction itself.
 * <p>
 * Its only job is to disable jaulib's {@code TempJarCache} loader (which cannot locate its jar in OSGi,
 * where there is no flat classpath). With it off, Direct-BT's {@code JNILibrary.loadLibraryImpl} uses plain
 * {@code System.loadLibrary(name)}, the mode {@code Bundle-NativeCode} services. The actual load is
 * triggered later by Direct-BT's {@code BTFactory.initLibrary} through {@code BTFactory.class}'s
 * classloader — this (lib) bundle — so the JNI stays bound to a bundle that is never hot-swapped,
 * preserving hot-swap of the openHAB binding bundle.
 * <p>
 * This bundle starts before the binding bundle (lower start-level), so the property below is set before the
 * binding triggers the native load.
 */
public final class DirectBTActivator implements BundleActivator {

    @Override
    public void start(BundleContext context) throws Exception {
        // No flat classpath in OSGi, so jaulib's TempJarCache can't find its jar; disable it. Direct-BT
        // then loads via System.loadLibrary(name), which the framework satisfies from Bundle-NativeCode.
        System.setProperty("jau.pkg.UseTempJarCache", "false");
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // The native BTManager is a process singleton and the libraries cannot be unloaded; a binding
        // refresh re-acquires them.
    }
}
