/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.kernel.module.util.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.concurrent.DefaultNoticeableFuture;
import com.liferay.portal.kernel.concurrent.FutureListener;
import com.liferay.portal.kernel.dependency.manager.DependencyManagerSyncUtil;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayOutputStream;
import com.liferay.portal.kernel.module.util.ServiceLatch;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.test.log.LogCapture;
import com.liferay.portal.test.log.LogEntry;
import com.liferay.portal.test.log.LoggerTestUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.io.InputStream;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Eric Yan
 */
@RunWith(Arquillian.class)
public class ServiceLatchTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testServiceTrackerCleanUpWithStoppedBundle() throws Exception {
		Bundle bundle = FrameworkUtil.getBundle(ServiceLatchTest.class);

		BundleContext bundleContext = bundle.getBundleContext();

		_testServiceTrackerCleanUpWithStoppedBundle(
			null, bundleContext, _createLiferayServiceBundle());
		_testServiceTrackerCleanUpWithStoppedBundle(
			testBundle -> {
				BundleContext testBundleContext = testBundle.getBundleContext();

				ServiceLatch serviceLatch = new ServiceLatch(testBundleContext);

				serviceLatch.waitFor(
					StringBundler.concat(
						"(&(objectClass=", Object.class.getName(),
						")(test.bundle.symbolic.name=",
						testBundle.getSymbolicName(), "))"));

				serviceLatch.openOn(
					() -> {
					});

				testBundleContext.registerService(
					Object.class, new Object(),
					MapUtil.singletonDictionary(
						"test.bundle.symbolic.name",
						testBundle.getSymbolicName()));

				serviceLatch.close();
			},
			bundleContext, _createBundle());
	}

	private InputStream _createBundle() throws Exception {
		try (UnsyncByteArrayOutputStream unsyncByteArrayOutputStream =
				new UnsyncByteArrayOutputStream()) {

			try (JarOutputStream jarOutputStream = new JarOutputStream(
					unsyncByteArrayOutputStream)) {

				Manifest manifest = new Manifest();

				Attributes attributes = manifest.getMainAttributes();

				attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
				attributes.putValue(
					Constants.BUNDLE_SYMBOLICNAME,
					ServiceLatchTest.class.getName());
				attributes.putValue(Constants.BUNDLE_VERSION, "1.0.0");
				attributes.putValue("Manifest-Version", "1.0");

				jarOutputStream.putNextEntry(
					new ZipEntry(JarFile.MANIFEST_NAME));

				manifest.write(jarOutputStream);

				jarOutputStream.closeEntry();
			}

			return new UnsyncByteArrayInputStream(
				unsyncByteArrayOutputStream.unsafeGetByteArray(), 0,
				unsyncByteArrayOutputStream.size());
		}
	}

	private InputStream _createLiferayServiceBundle() throws Exception {
		try (UnsyncByteArrayOutputStream unsyncByteArrayOutputStream =
				new UnsyncByteArrayOutputStream()) {

			try (JarOutputStream jarOutputStream = new JarOutputStream(
					unsyncByteArrayOutputStream)) {

				Manifest manifest = new Manifest();

				Attributes attributes = manifest.getMainAttributes();

				attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
				attributes.putValue(
					Constants.BUNDLE_SYMBOLICNAME,
					ServiceLatchTest.class.getName());
				attributes.putValue(Constants.BUNDLE_VERSION, "1.0.0");
				attributes.putValue("Liferay-Require-SchemaVersion", "1.0.0");
				attributes.putValue("Liferay-Service", Boolean.TRUE.toString());
				attributes.putValue("Manifest-Version", "1.0");

				jarOutputStream.putNextEntry(
					new ZipEntry(JarFile.MANIFEST_NAME));

				manifest.write(jarOutputStream);

				jarOutputStream.closeEntry();

				jarOutputStream.putNextEntry(
					new JarEntry("service.properties"));

				jarOutputStream.closeEntry();
			}

			return new UnsyncByteArrayInputStream(
				unsyncByteArrayOutputStream.unsafeGetByteArray(), 0,
				unsyncByteArrayOutputStream.size());
		}
	}

	private void _testServiceTrackerCleanUpWithStoppedBundle(
			Consumer<Bundle> bundleConsumer, BundleContext bundleContext,
			InputStream inputStream)
		throws Exception {

		Bundle bundle = bundleContext.installBundle("location", inputStream);

		DefaultNoticeableFuture<Void> syncCallableDefaultNoticeableFuture =
			ReflectionTestUtil.getFieldValue(
				DependencyManagerSyncUtil.class,
				"_syncCallableDefaultNoticeableFuture");

		try (AutoCloseable autoCloseable1 =
				ReflectionTestUtil.setFieldValueWithAutoCloseable(
					syncCallableDefaultNoticeableFuture, "callable",
					(Callable<Object>)() -> null);
			AutoCloseable autoCloseable2 =
				ReflectionTestUtil.setFieldValueWithAutoCloseable(
					syncCallableDefaultNoticeableFuture, "state", 0);
			LogCapture logCapture = LoggerTestUtil.configureLog4JLogger(
				DependencyManagerSyncUtil.class.getName(),
				LoggerTestUtil.ERROR)) {

			Set<FutureListener<?>> futureListeners =
				ReflectionTestUtil.getFieldValue(
					syncCallableDefaultNoticeableFuture, "_futureListeners");

			int initialSize = futureListeners.size();

			bundle.start();

			if (bundleConsumer != null) {
				bundleConsumer.accept(bundle);
			}

			bundle.stop();

			boolean listenerAdded = false;

			if (futureListeners.size() > initialSize) {
				listenerAdded = true;
			}

			DependencyManagerSyncUtil.sync();

			Assert.assertTrue(
				"Expected listener to be added by ServiceLatch", listenerAdded);

			List<LogEntry> logEntries = logCapture.getLogEntries();

			Assert.assertEquals(logEntries.toString(), 0, logEntries.size());
		}
		finally {
			bundle.uninstall();
		}
	}

}