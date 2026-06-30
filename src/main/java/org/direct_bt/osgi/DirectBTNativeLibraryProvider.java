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

import java.nio.file.Path;

/**
 * Marker service published by the Direct-BT OSGi wrapper only after all bundled native libraries have been
 * extracted onto {@code java.library.path}.
 */
public interface DirectBTNativeLibraryProvider {

    Path getLibraryDirectory();
}
