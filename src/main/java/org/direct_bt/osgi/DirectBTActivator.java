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

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * OSGi {@link BundleActivator} for the re-manifested Direct-BT fat jar.
 * <p>
 * The native libraries are carried in this bundle under {@code natives/<arch>/} and declared in the
 * {@code Bundle-NativeCode} manifest header. The OSGi framework unpacks the matching library from the
 * bundle and serves it to {@code System.loadLibrary(basename)} via the bundle classloader's
 * {@code findLibrary} hook — so this activator does no native extraction itself.
 * <p>
 * It only disables jaulib's {@code TempJarCache} loader (which cannot locate its jar in OSGi, where there
 * is no flat classpath). With it off, Direct-BT's {@code JNILibrary.loadLibraryImpl} uses plain
 * {@code System.loadLibrary(name)}, the mode {@code Bundle-NativeCode} services. The actual load is
 * triggered later by Direct-BT's {@code BTFactory.initLibrary} through {@code BTFactory.class}'s
 * classloader — this (lib) bundle — so the JNI stays bound to a bundle that is never hot-swapped,
 * preserving hot-swap of the openHAB binding bundle.
 * <p>
 * A {@link DirectBTNativeLibraryProvider} marker service is published once this activator has run, so the
 * binding (which {@code @Reference}s it) does not activate before the loader is configured.
 */
public final class DirectBTActivator implements BundleActivator {

    private ServiceRegistration<DirectBTNativeLibraryProvider> nativeLibraryProvider;

    @Override
    public void start(BundleContext context) throws Exception {
        // No flat classpath in OSGi, so jaulib's TempJarCache can't find its jar; disable it. Direct-BT
        // then loads via System.loadLibrary(name), which the framework satisfies from Bundle-NativeCode.
        System.setProperty("jau.pkg.UseTempJarCache", "false");
        nativeLibraryProvider = context.registerService(DirectBTNativeLibraryProvider.class,
                new DirectBTNativeLibraryProvider() {
                }, new Hashtable<>());
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        ServiceRegistration<DirectBTNativeLibraryProvider> registration = nativeLibraryProvider;
        if (registration != null) {
            registration.unregister();
            nativeLibraryProvider = null;
        }
        // The native BTManager is a process singleton and the libraries cannot be unloaded; a binding
        // refresh re-acquires them.
    }
}
