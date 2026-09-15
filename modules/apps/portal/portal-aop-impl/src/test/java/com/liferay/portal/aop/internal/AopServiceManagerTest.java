/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.aop.internal;

import com.liferay.portal.aop.AopService;
import com.liferay.portal.events.StartupHelperUtil;
import com.liferay.portal.kernel.concurrent.SystemExecutorServiceUtil;
import com.liferay.portal.kernel.dependency.manager.DependencyManagerSyncUtil;
import com.liferay.portal.kernel.module.util.SystemBundleUtil;
import com.liferay.portal.kernel.test.ReflectionTestUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.spring.transaction.TransactionExecutor;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Eric Yan
 */
public class AopServiceManagerTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void test() throws Exception {
		AopServiceManager aopServiceManager = new AopServiceManager();

		ReflectionTestUtil.setFieldValue(
			aopServiceManager, "_portalTransactionExecutor",
			ProxyUtil.newProxyInstance(
				TransactionExecutor.class.getClassLoader(),
				new Class<?>[] {TransactionExecutor.class},
				(proxy, method, args) -> null));

		ReflectionTestUtil.setFieldValue(
			ReflectionTestUtil.<Object>getFieldValue(
				StartupHelperUtil.class, "_dbWarmedSCLSingleton"),
			"_singleton", Boolean.TRUE);

		BundleContext bundleContext = SystemBundleUtil.getBundleContext();

		aopServiceManager.activate(bundleContext);

		CountDownLatch countDownLatch = new CountDownLatch(1);

		ServiceRegistration<AopService> serviceRegistration = null;

		try {
			_blockSystemExecutorService(countDownLatch);

			serviceRegistration = bundleContext.registerService(
				AopService.class,
				(AopService)ProxyUtil.newProxyInstance(
					TestService.class.getClassLoader(),
					new Class<?>[] {AopService.class, TestService.class},
					(proxy, method, args) -> null),
				null);

			Assert.assertNull(
				bundleContext.getServiceReference(TestService.class));

			DependencyManagerSyncUtil.registerSyncFutureTask(
				new FutureTask<>(
					() -> {
						countDownLatch.countDown();

						return null;
					}),
				AopServiceManagerTest.class.getName());

			DependencyManagerSyncUtil.sync();

			Assert.assertNotNull(
				bundleContext.getServiceReference(TestService.class));
		}
		finally {
			countDownLatch.countDown();

			if (serviceRegistration != null) {
				serviceRegistration.unregister();
			}

			aopServiceManager.deactivate();
		}
	}

	private void _blockSystemExecutorService(CountDownLatch countDownLatch) {
		ThreadPoolExecutor threadPoolExecutor =
			(ThreadPoolExecutor)SystemExecutorServiceUtil.getExecutorService();

		for (int i = 0; i < threadPoolExecutor.getMaximumPoolSize(); i++) {
			threadPoolExecutor.submit(
				() -> {
					countDownLatch.await();

					return null;
				});
		}
	}

	private interface TestService {
	}

}