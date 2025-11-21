/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.spring.extender.internal.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.portal.kernel.concurrent.DefaultNoticeableFuture;
import com.liferay.portal.kernel.concurrent.FutureListener;
import com.liferay.portal.kernel.dependency.manager.DependencyManagerSyncUtil;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayOutputStream;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
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
public class LiferayServiceExtenderTest {

	@ClassRule
	@Rule
	public static final LiferayIntegrationTestRule liferayIntegrationTestRule =
		new LiferayIntegrationTestRule();

	@Test
	public void testLiferayServiceEdgeCaseResultingInBlockedThread()
		throws Exception {

		Bundle bundle = FrameworkUtil.getBundle(
			LiferayServiceExtenderTest.class);

		/**
		 * Note: The blocked thread is not being caused by ServiceLatch logic.
		 * Its being caused by the complex logic in ServiceConfigurationExtender
		 * and ServiceConfigurationInitializer.
		 *
		 * In ServiceConfigurationInitializer.stop(), _futureTask.get() is
		 * called, resulting in the blocked thread.
		 *
		 * The test case sets a strange edge-case requireSchemaVersion, which
		 * causes the ServiceLatch in ServiceConfigurationExtender to not
		 * satisfy it's waitFor, resulting in the openOn to not be executed.
		 *
		 * Since the openOn: serviceConfigurationInitializer::start is not
		 * executed, _futureTask.run() is never called. This results in the
		 * blocked thread.
		 */
		_testServiceTrackerCleanUpWithStoppedBundle(
			null, bundle.getBundleContext(),
			_createLiferayServiceBundle("[1.0.0,1.0.0]"));
	}

	private InputStream _createLiferayServiceBundle(String requireSchemaVersion)
		throws Exception {

		try (UnsyncByteArrayOutputStream unsyncByteArrayOutputStream =
				new UnsyncByteArrayOutputStream()) {

			try (JarOutputStream jarOutputStream = new JarOutputStream(
					unsyncByteArrayOutputStream)) {

				Manifest manifest = new Manifest();

				Attributes attributes = manifest.getMainAttributes();

				attributes.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
				attributes.putValue(
					Constants.BUNDLE_SYMBOLICNAME,
					LiferayServiceExtenderTest.class.getName());
				attributes.putValue(Constants.BUNDLE_VERSION, "1.0.0");
				attributes.putValue(
					"Liferay-Require-SchemaVersion", requireSchemaVersion);
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