/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.app.license.resolver.hook;

import com.liferay.portal.app.license.AppLicenseVerifier;
import com.liferay.portal.kernel.util.ModuleFrameworkPropsValues;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.hooks.resolver.ResolverHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.FrameworkWiring;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Amos Fong
 */
public class AppResolverHookFactory implements ResolverHookFactory {

	public AppResolverHookFactory(BundleContext bundleContext) {
		_bundleContext = bundleContext;

		_frameworkListener = new FrameworkListener() {

			@Override
			public void frameworkEvent(FrameworkEvent frameworkEvent) {
				if ((frameworkEvent.getType() ==
						FrameworkEvent.STARTLEVEL_CHANGED) &&
					!_readyToResolve.get()) {

					Bundle bundle = frameworkEvent.getBundle();

					FrameworkStartLevel frameworkStartLevel = bundle.adapt(
						FrameworkStartLevel.class);

					if (frameworkStartLevel.getStartLevel() >=
							ModuleFrameworkPropsValues.
								MODULE_FRAMEWORK_DYNAMIC_INSTALL_START_LEVEL) {

						_readyToResolve.set(true);

						FrameworkWiring frameworkWiring = bundle.adapt(
							FrameworkWiring.class);

						frameworkWiring.resolveBundles(_resolveBundles);

						_resolveBundles.clear();

						_bundleContext.removeFrameworkListener(
							_frameworkListener);
					}
				}
			}

		};

		bundleContext.addFrameworkListener(_frameworkListener);

		_serviceTracker = new ServiceTracker<>(
			bundleContext, AppLicenseVerifier.class, null);

		_serviceTracker.open();
	}

	@Override
	public ResolverHook begin(Collection<BundleRevision> triggers) {
		return new AppResolverHook(
			_serviceTracker, _filteredBundleSymbolicNames, _filteredProductIds,
			_readyToResolve, _resolveBundles);
	}

	public void close() {
		_serviceTracker.close();

		_bundleContext.removeFrameworkListener(_frameworkListener);
	}

	private final BundleContext _bundleContext;
	private final Set<String> _filteredBundleSymbolicNames =
		Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Set<String> _filteredProductIds = Collections.newSetFromMap(
		new ConcurrentHashMap<>());
	private final FrameworkListener _frameworkListener;
	private final AtomicBoolean _readyToResolve = new AtomicBoolean();
	private final Set<Bundle> _resolveBundles = Collections.newSetFromMap(
		new ConcurrentHashMap<>());
	private final ServiceTracker<AppLicenseVerifier, AppLicenseVerifier>
		_serviceTracker;

}