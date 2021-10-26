/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.fragment.bundle.watcher.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.util.PropsValues;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Eric Yan
 */
@RunWith(Arquillian.class)
public class PortalFragmentBundleWatcherTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() {
		Bundle bundle = FrameworkUtil.getBundle(
			PortalFragmentBundleWatcherTest.class);

		_bundleContext = bundle.getBundleContext();
	}

	@Test
	public void testDeployFragment() throws Exception {
		String hostSymbolicName = _PACKAGE_NAME.concat(".host");

		Path hostJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			hostSymbolicName.concat(".jar"));

		String fragmentSymbolicName = hostSymbolicName.concat(".fragment");

		Path fragmentJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentSymbolicName.concat(".jar"));

		AtomicInteger actualHostRefreshCount = new AtomicInteger();
		CountDownLatch fragmentResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch hostStartedCountDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = bundleEvent -> {
			Bundle bundle = bundleEvent.getBundle();

			String symbolicName = bundle.getSymbolicName();

			int type = bundleEvent.getType();

			if (Objects.equals(symbolicName, fragmentSymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, hostSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					hostStartedCountDownLatch.countDown();
				}
				else if (type == BundleEvent.STOPPED) {
					actualHostRefreshCount.incrementAndGet();
				}
			}
		};

		_bundleContext.addBundleListener(bundleListener);

		try {
			_createAndDeployBundle(hostSymbolicName, hostJarPath);

			hostStartedCountDownLatch.await();

			_createAndDeployFragmentBundle(
				fragmentSymbolicName, fragmentJarPath, hostSymbolicName);

			boolean fragmentResolved = fragmentResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment could not be resolved", fragmentResolved);

			//Add additional delay for PortalFragmentBundleWatcher refreshes
			Thread.sleep(3000);

			int expectedHostRefreshCount = 1;

			Assert.assertEquals(
				expectedHostRefreshCount, actualHostRefreshCount.intValue());
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);
			_uninstallBundle(hostSymbolicName, hostJarPath);

			_uninstallBundle(fragmentSymbolicName, fragmentJarPath);
		}
	}

	@Test
	public void testDeployFragmentWithDependency() throws Exception {
		String hostSymbolicName = _PACKAGE_NAME.concat(".host");

		Path hostJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			hostSymbolicName.concat(".jar"));

		String fragmentSymbolicName = hostSymbolicName.concat(".fragment");

		Path fragmentJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentSymbolicName.concat(".jar"));

		String dependencySymbolicName = _PACKAGE_NAME.concat(".dependency");

		Path dependencyJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			dependencySymbolicName.concat(".jar"));

		AtomicInteger actualHostRefreshCount = new AtomicInteger();
		CountDownLatch dependencyStartedCountDownLatch = new CountDownLatch(1);
		CountDownLatch fragmentResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch hostStartedCountDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = bundleEvent -> {
			Bundle bundle = bundleEvent.getBundle();

			String symbolicName = bundle.getSymbolicName();

			int type = bundleEvent.getType();

			if (Objects.equals(symbolicName, dependencySymbolicName)) {
				if (type == BundleEvent.STARTED) {
					dependencyStartedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, fragmentSymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, hostSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					hostStartedCountDownLatch.countDown();
				}
				else if (type == BundleEvent.STOPPED) {
					actualHostRefreshCount.incrementAndGet();
				}
			}
		};

		_bundleContext.addBundleListener(bundleListener);

		try {
			_createAndDeployBundle(hostSymbolicName, hostJarPath);

			hostStartedCountDownLatch.await();

			_createAndDeployDependencyBundle(
				dependencySymbolicName, dependencyJarPath);

			dependencyStartedCountDownLatch.await();

			_createAndDeployFragmentBundleWithDependency(
				fragmentSymbolicName, fragmentJarPath, hostSymbolicName,
				dependencySymbolicName);

			boolean fragmentResolved = fragmentResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment could not be resolved", fragmentResolved);

			//Add additional delay for PortalFragmentBundleWatcher refreshes
			Thread.sleep(3000);

			int expectedHostRefreshCount = 1;

			Assert.assertEquals(
				expectedHostRefreshCount, actualHostRefreshCount.intValue());
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);
			_uninstallBundle(hostSymbolicName, hostJarPath);

			_uninstallBundle(dependencySymbolicName, dependencyJarPath);
			_uninstallBundle(fragmentSymbolicName, fragmentJarPath);
		}
	}

	@Test
	public void testDeployFragmentWithMissingDependency() throws Exception {
		String hostSymbolicName = _PACKAGE_NAME.concat(".host");

		Path hostJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			hostSymbolicName.concat(".jar"));

		String fragmentSymbolicName = hostSymbolicName.concat(".fragment");

		Path fragmentJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentSymbolicName.concat(".jar"));

		String dependencySymbolicName = _PACKAGE_NAME.concat(".dependency");

		AtomicInteger actualHostRefreshCount = new AtomicInteger();
		CountDownLatch fragmentResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch hostStartedCountDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = bundleEvent -> {
			Bundle bundle = bundleEvent.getBundle();

			String symbolicName = bundle.getSymbolicName();

			int type = bundleEvent.getType();

			if (Objects.equals(symbolicName, fragmentSymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, hostSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					hostStartedCountDownLatch.countDown();
				}
				else if (type == BundleEvent.STOPPED) {
					actualHostRefreshCount.incrementAndGet();
				}
			}
		};

		_bundleContext.addBundleListener(bundleListener);

		try {
			_createAndDeployBundle(hostSymbolicName, hostJarPath);

			hostStartedCountDownLatch.await();

			_createAndDeployFragmentBundleWithDependency(
				fragmentSymbolicName, fragmentJarPath, hostSymbolicName,
				dependencySymbolicName);

			boolean fragmentResolved = fragmentResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertFalse(
				"Fragment was resolved, but should only be installed, since " +
					"it has a missing dependency",
				fragmentResolved);

			//Add additional delay for PortalFragmentBundleWatcher refreshes
			Thread.sleep(3000);

			int expectedHostRefreshCount = 0;

			Assert.assertEquals(
				expectedHostRefreshCount, actualHostRefreshCount.intValue());
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);
			_uninstallBundle(hostSymbolicName, hostJarPath);

			_uninstallBundle(fragmentSymbolicName, fragmentJarPath);
		}
	}

	@Test
	public void testDeployMultipleFragmentsAndUnrelatedBundlesSimultaneously()
		throws Exception {

		String hostSymbolicName = _PACKAGE_NAME.concat(".host");

		Path hostJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			hostSymbolicName.concat(".jar"));

		String fragmentASymbolicName = hostSymbolicName.concat(".fragment.a");

		Path fragmentAJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentASymbolicName.concat(".jar"));

		String fragmentBSymbolicName = hostSymbolicName.concat(".fragment.b");

		Path fragmentBJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentBSymbolicName.concat(".jar"));

		String fragmentCSymbolicName = hostSymbolicName.concat(".fragment.c");

		Path fragmentCJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentCSymbolicName.concat(".jar"));

		String unrelatedASymbolicName = _PACKAGE_NAME.concat(".unrelated.a");

		Path unrelatedAJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			unrelatedASymbolicName.concat(".jar"));

		String unrelatedBSymbolicName = _PACKAGE_NAME.concat(".unrelated.b");

		Path unrelatedBJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			unrelatedBSymbolicName.concat(".jar"));

		String unrelatedCSymbolicName = _PACKAGE_NAME.concat(".unrelated.c");

		Path unrelatedCJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			unrelatedCSymbolicName.concat(".jar"));

		AtomicInteger actualHostRefreshCount = new AtomicInteger();
		CountDownLatch fragmentAResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch fragmentBResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch fragmentCResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch hostStartedCountDownLatch = new CountDownLatch(1);
		CountDownLatch unrelatedAStartedCountDownLatch = new CountDownLatch(1);
		CountDownLatch unrelatedBStartedCountDownLatch = new CountDownLatch(1);
		CountDownLatch unrelatedCStartedCountDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = bundleEvent -> {
			Bundle bundle = bundleEvent.getBundle();

			String symbolicName = bundle.getSymbolicName();

			int type = bundleEvent.getType();

			if (Objects.equals(symbolicName, fragmentASymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentAResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, fragmentBSymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentBResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, fragmentCSymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentCResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, hostSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					hostStartedCountDownLatch.countDown();
				}
				else if (type == BundleEvent.STOPPED) {
					actualHostRefreshCount.incrementAndGet();
				}
			}
			else if (Objects.equals(symbolicName, unrelatedASymbolicName)) {
				if (type == BundleEvent.STARTED) {
					unrelatedAStartedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, unrelatedBSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					unrelatedBStartedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, unrelatedCSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					unrelatedCStartedCountDownLatch.countDown();
				}
			}
		};

		_bundleContext.addBundleListener(bundleListener);

		try {
			_createAndDeployBundle(hostSymbolicName, hostJarPath);

			hostStartedCountDownLatch.await();

			_createAndDeployBundle(unrelatedASymbolicName, unrelatedAJarPath);
			_createAndDeployFragmentBundle(
				fragmentASymbolicName, fragmentAJarPath, hostSymbolicName);

			_createAndDeployBundle(unrelatedBSymbolicName, unrelatedBJarPath);
			_createAndDeployFragmentBundle(
				fragmentBSymbolicName, fragmentBJarPath, hostSymbolicName);

			_createAndDeployBundle(unrelatedCSymbolicName, unrelatedCJarPath);
			_createAndDeployFragmentBundle(
				fragmentCSymbolicName, fragmentCJarPath, hostSymbolicName);

			boolean fragmentAResolved = fragmentAResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment A could not be resolved", fragmentAResolved);

			boolean fragmentBResolved = fragmentBResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment B could not be resolved", fragmentBResolved);

			boolean fragmentCResolved = fragmentCResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment C could not be resolved", fragmentCResolved);

			boolean unrelatedAStarted = unrelatedAStartedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Unrelated Bundle A could not be started", unrelatedAStarted);

			boolean unrelatedBStarted = unrelatedBStartedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Unrelated Bundle B could not be started", unrelatedBStarted);

			boolean unrelatedCStarted = unrelatedCStartedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Unrelated Bundle C could not be started", unrelatedCStarted);

			//Add additional delay for PortalFragmentBundleWatcher refreshes
			Thread.sleep(3000);

			int expectedReasonableMaxHostRefreshCount = 3;

			Assert.assertTrue(
				StringBundler.concat(
					"Expected host to refresh a reasonable amount of times, ",
					"like at most ", expectedReasonableMaxHostRefreshCount,
					" times, but was refreshed ",
					actualHostRefreshCount.intValue(), " times instead."),
				actualHostRefreshCount.intValue() <=
					expectedReasonableMaxHostRefreshCount);
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);
			_uninstallBundle(hostSymbolicName, hostJarPath);

			_uninstallBundle(fragmentASymbolicName, fragmentAJarPath);
			_uninstallBundle(fragmentBSymbolicName, fragmentBJarPath);
			_uninstallBundle(fragmentCSymbolicName, fragmentCJarPath);
			_uninstallBundle(unrelatedASymbolicName, unrelatedAJarPath);
			_uninstallBundle(unrelatedBSymbolicName, unrelatedBJarPath);
			_uninstallBundle(unrelatedCSymbolicName, unrelatedCJarPath);
		}
	}

	@Test
	public void testDeployMultipleFragmentsWithDependencies1()
		throws Exception {

		String hostSymbolicName = _PACKAGE_NAME.concat(".host");

		Path hostJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			hostSymbolicName.concat(".jar"));

		String fragmentASymbolicName = hostSymbolicName.concat(".fragment.a");

		Path fragmentAJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentASymbolicName.concat(".jar"));

		String dependencyASymbolicName = _PACKAGE_NAME.concat(".dependency.a");

		Path dependencyAJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			dependencyASymbolicName.concat(".jar"));

		String fragmentBSymbolicName = hostSymbolicName.concat(".fragment.b");

		Path fragmentBJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentBSymbolicName.concat(".jar"));

		String dependencyBSymbolicName = _PACKAGE_NAME.concat(".dependency.b");

		Path dependencyBJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			dependencyBSymbolicName.concat(".jar"));

		AtomicInteger actualHostRefreshCount = new AtomicInteger();
		CountDownLatch dependencyAStartedCountDownLatch = new CountDownLatch(1);
		CountDownLatch dependencyBStartedCountDownLatch = new CountDownLatch(1);
		CountDownLatch fragmentAResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch fragmentBResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch hostStartedCountDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = bundleEvent -> {
			Bundle bundle = bundleEvent.getBundle();

			String symbolicName = bundle.getSymbolicName();

			int type = bundleEvent.getType();

			if (Objects.equals(symbolicName, dependencyASymbolicName)) {
				if (type == BundleEvent.STARTED) {
					dependencyAStartedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, dependencyBSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					dependencyBStartedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, fragmentASymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentAResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, fragmentBSymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentBResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, hostSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					hostStartedCountDownLatch.countDown();
				}
				else if (type == BundleEvent.STOPPED) {
					actualHostRefreshCount.incrementAndGet();
				}
			}
		};

		_bundleContext.addBundleListener(bundleListener);

		try {
			_createAndDeployBundle(hostSymbolicName, hostJarPath);

			hostStartedCountDownLatch.await();

			_createAndDeployDependencyBundle(
				dependencyASymbolicName, dependencyAJarPath);

			dependencyAStartedCountDownLatch.await();

			_createAndDeployFragmentBundleWithDependency(
				fragmentASymbolicName, fragmentAJarPath, hostSymbolicName,
				dependencyASymbolicName);

			boolean fragmentResolved = fragmentAResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment A could not be resolved", fragmentResolved);

			_createAndDeployDependencyBundle(
				dependencyBSymbolicName, dependencyBJarPath);

			dependencyBStartedCountDownLatch.await();

			_createAndDeployFragmentBundleWithDependency(
				fragmentBSymbolicName, fragmentBJarPath, hostSymbolicName,
				dependencyBSymbolicName);

			fragmentResolved = fragmentBResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment B could not be resolved", fragmentResolved);

			//Add additional delay for PortalFragmentBundleWatcher refreshes
			Thread.sleep(3000);

			int expectedMaxHostRefreshCount = 2;

			Assert.assertTrue(
				StringBundler.concat(
					"Expected host to refresh at most ",
					expectedMaxHostRefreshCount, " times, but refreshed ",
					actualHostRefreshCount.intValue(), " times instead."),
				actualHostRefreshCount.intValue() <=
					expectedMaxHostRefreshCount);
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);
			_uninstallBundle(hostSymbolicName, hostJarPath);

			_uninstallBundle(dependencyASymbolicName, dependencyAJarPath);
			_uninstallBundle(dependencyBSymbolicName, dependencyBJarPath);
			_uninstallBundle(fragmentASymbolicName, fragmentAJarPath);
			_uninstallBundle(fragmentBSymbolicName, fragmentBJarPath);
		}
	}

	@Test
	public void testDeployMultipleFragmentsWithDependencies2()
		throws Exception {

		String hostSymbolicName = _PACKAGE_NAME.concat(".host");

		Path hostJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			hostSymbolicName.concat(".jar"));

		String fragmentASymbolicName = hostSymbolicName.concat(".fragment.a");

		Path fragmentAJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentASymbolicName.concat(".jar"));

		String dependencyASymbolicName = _PACKAGE_NAME.concat(".dependency.a");

		Path dependencyAJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			dependencyASymbolicName.concat(".jar"));

		String fragmentBSymbolicName = hostSymbolicName.concat(".fragment.b");

		Path fragmentBJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			fragmentBSymbolicName.concat(".jar"));

		String dependencyBSymbolicName = _PACKAGE_NAME.concat(".dependency.b");

		Path dependencyBJarPath = Paths.get(
			PropsValues.MODULE_FRAMEWORK_MODULES_DIR,
			dependencyBSymbolicName.concat(".jar"));

		AtomicInteger actualHostRefreshCount = new AtomicInteger();
		CountDownLatch dependencyAStartedCountDownLatch = new CountDownLatch(1);
		CountDownLatch dependencyBStartedCountDownLatch = new CountDownLatch(1);
		CountDownLatch fragmentAResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch fragmentBInstalledCountDownLatch = new CountDownLatch(1);
		CountDownLatch fragmentBResolvedCountDownLatch = new CountDownLatch(1);
		CountDownLatch hostStartedCountDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = bundleEvent -> {
			Bundle bundle = bundleEvent.getBundle();

			String symbolicName = bundle.getSymbolicName();

			int type = bundleEvent.getType();

			if (Objects.equals(symbolicName, dependencyASymbolicName)) {
				if (type == BundleEvent.STARTED) {
					dependencyAStartedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, dependencyBSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					dependencyBStartedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, fragmentASymbolicName)) {
				if (type == BundleEvent.RESOLVED) {
					fragmentAResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, fragmentBSymbolicName)) {
				if (type == BundleEvent.INSTALLED) {
					fragmentBInstalledCountDownLatch.countDown();
				}
				else if (type == BundleEvent.RESOLVED) {
					fragmentBResolvedCountDownLatch.countDown();
				}
			}
			else if (Objects.equals(symbolicName, hostSymbolicName)) {
				if (type == BundleEvent.STARTED) {
					hostStartedCountDownLatch.countDown();
				}
				else if (type == BundleEvent.STOPPED) {
					actualHostRefreshCount.incrementAndGet();
				}
			}
		};

		_bundleContext.addBundleListener(bundleListener);

		try {
			_createAndDeployBundle(hostSymbolicName, hostJarPath);

			hostStartedCountDownLatch.await();

			_createAndDeployDependencyBundle(
				dependencyASymbolicName, dependencyAJarPath);

			dependencyAStartedCountDownLatch.await();

			_createAndDeployFragmentBundleWithDependency(
				fragmentASymbolicName, fragmentAJarPath, hostSymbolicName,
				dependencyASymbolicName);

			boolean fragmentResolved = fragmentAResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment A could not be resolved", fragmentResolved);

			_createAndDeployFragmentBundleWithDependency(
				fragmentBSymbolicName, fragmentBJarPath, hostSymbolicName,
				dependencyBSymbolicName);

			fragmentBInstalledCountDownLatch.await();

			_createAndDeployDependencyBundle(
				dependencyBSymbolicName, dependencyBJarPath);

			dependencyBStartedCountDownLatch.await();

			fragmentResolved = fragmentBResolvedCountDownLatch.await(
				20, TimeUnit.SECONDS);

			Assert.assertTrue(
				"Fragment B could not be resolved", fragmentResolved);

			//Add additional delay for PortalFragmentBundleWatcher refreshes
			Thread.sleep(3000);

			int expectedMaxHostRefreshCount = 2;

			Assert.assertTrue(
				StringBundler.concat(
					"Expected host to refresh at most ",
					expectedMaxHostRefreshCount, " times, but refreshed ",
					actualHostRefreshCount.intValue(), " times instead."),
				actualHostRefreshCount.intValue() <=
					expectedMaxHostRefreshCount);
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);
			_uninstallBundle(hostSymbolicName, hostJarPath);

			_uninstallBundle(dependencyASymbolicName, dependencyAJarPath);
			_uninstallBundle(dependencyBSymbolicName, dependencyBJarPath);
			_uninstallBundle(fragmentASymbolicName, fragmentAJarPath);
			_uninstallBundle(fragmentBSymbolicName, fragmentBJarPath);
		}
	}

	private void _createAndDeployBundle(String symbolicName, Path jarPath)
		throws IOException {

		_createAndDeployBundle(symbolicName, jarPath, symbolicName, null, null);
	}

	private void _createAndDeployBundle(
			String symbolicName, Path jarPath, String exports, String imports,
			String fragmentHost)
		throws IOException {

		try (OutputStream outputStream = Files.newOutputStream(jarPath);
			JarOutputStream jarOutputStream = new JarOutputStream(
				outputStream)) {

			Manifest manifest = new Manifest();

			Attributes attributes = manifest.getMainAttributes();

			attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
			attributes.putValue(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
			attributes.putValue(Constants.BUNDLE_VERSION, "1.0.0");

			if (exports != null) {
				attributes.putValue(Constants.EXPORT_PACKAGE, exports);
			}

			if (fragmentHost != null) {
				attributes.putValue(Constants.FRAGMENT_HOST, fragmentHost);
			}

			if (imports != null) {
				attributes.putValue(Constants.IMPORT_PACKAGE, imports);
			}

			attributes.putValue("Manifest-Version", "2");

			jarOutputStream.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));

			manifest.write(jarOutputStream);

			jarOutputStream.closeEntry();
		}
	}

	private void _createAndDeployDependencyBundle(
			String dependencySymbolicName, Path dependencyJarPath)
		throws IOException {

		_createAndDeployBundle(
			dependencySymbolicName, dependencyJarPath, dependencySymbolicName,
			null, null);
	}

	private void _createAndDeployFragmentBundle(
			String fragmentSymbolicName, Path fragmentJarPath,
			String hostSymbolicName)
		throws IOException {

		_createAndDeployBundle(
			fragmentSymbolicName, fragmentJarPath, null, null,
			hostSymbolicName);
	}

	private void _createAndDeployFragmentBundleWithDependency(
			String fragmentSymbolicName, Path fragmentJarPath,
			String hostSymbolicName, String dependencySymbolicName)
		throws IOException {

		_createAndDeployBundle(
			fragmentSymbolicName, fragmentJarPath, null, dependencySymbolicName,
			hostSymbolicName);
	}

	private void _uninstallBundle(String symbolicName, Path jarPath)
		throws Exception {

		if (!Files.exists(jarPath)) {
			return;
		}

		CountDownLatch countDownLatch = new CountDownLatch(1);

		BundleListener bundleListener = bundleEvent -> {
			Bundle bundle = bundleEvent.getBundle();

			if (!Objects.equals(bundle.getSymbolicName(), symbolicName)) {
				return;
			}

			int type = bundleEvent.getType();

			if (type == BundleEvent.UNINSTALLED) {
				countDownLatch.countDown();
			}
		};

		_bundleContext.addBundleListener(bundleListener);

		try {
			Files.deleteIfExists(jarPath);

			countDownLatch.await();
		}
		finally {
			_bundleContext.removeBundleListener(bundleListener);
		}
	}

	private static final String _PACKAGE_NAME;

	static {
		Package pkg = PortalFragmentBundleWatcherTest.class.getPackage();

		_PACKAGE_NAME = pkg.getName();
	}

	private BundleContext _bundleContext;

}