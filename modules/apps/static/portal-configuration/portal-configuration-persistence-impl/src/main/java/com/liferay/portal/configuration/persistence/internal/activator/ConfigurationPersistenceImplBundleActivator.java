/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.configuration.persistence.internal.activator;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.configuration.persistence.ReloadablePersistenceManager;
import com.liferay.portal.configuration.persistence.internal.ConfigurationPersistenceManager;
import com.liferay.portal.configuration.persistence.internal.configuration.persistence.listener.ConfigurationImportGlobalConfigurationModelListener;
import com.liferay.portal.configuration.persistence.internal.upgrade.ConfigurationUpgradeStepFactoryImpl;
import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListener;
import com.liferay.portal.configuration.persistence.upgrade.ConfigurationUpgradeStepFactory;
import com.liferay.portal.kernel.util.HashMapDictionaryBuilder;
import com.liferay.portal.kernel.util.InfrastructureUtil;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.cm.PersistenceManager;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * @author Carlos Sierra Andrés
 */
public class ConfigurationPersistenceImplBundleActivator
	implements BundleActivator {

	@Override
	public void start(BundleContext bundleContext) {
		_configurationModelListenerServiceRegistrations.add(
			bundleContext.registerService(
				ConfigurationModelListener.class,
				new ConfigurationImportGlobalConfigurationModelListener(),
				HashMapDictionaryBuilder.<String, Object>put(
					"model.class.name", StringPool.STAR
				).build()));

		_configurationPersistenceManager = new ConfigurationPersistenceManager(
			bundleContext, InfrastructureUtil.getDataSource());

		_configurationPersistenceManager.start();

		_configurationPersistenceManagerServiceRegistration =
			bundleContext.registerService(
				new String[] {
					PersistenceManager.class.getName(),
					ReloadablePersistenceManager.class.getName()
				},
				_configurationPersistenceManager,
				HashMapDictionaryBuilder.<String, Object>put(
					Constants.SERVICE_RANKING, Integer.MAX_VALUE - 1000
				).put(
					PersistenceManager.PROPERTY_NAME,
					ConfigurationPersistenceManager.class.getName()
				).build());

		_configurationUpgradeStepFactoryServiceRegistration =
			bundleContext.registerService(
				ConfigurationUpgradeStepFactory.class,
				new ConfigurationUpgradeStepFactoryImpl(
					_configurationPersistenceManager),
				null);
	}

	@Override
	public void stop(BundleContext bundleContext) {
		if (_configurationUpgradeStepFactoryServiceRegistration != null) {
			_configurationUpgradeStepFactoryServiceRegistration.unregister();
		}

		if (_configurationPersistenceManagerServiceRegistration != null) {
			_configurationPersistenceManagerServiceRegistration.unregister();
		}

		for (ServiceRegistration<ConfigurationModelListener>
				serviceRegistration :
					_configurationModelListenerServiceRegistrations) {

			serviceRegistration.unregister();
		}

		_configurationPersistenceManager.stop();
	}

	private final List<ServiceRegistration<ConfigurationModelListener>>
		_configurationModelListenerServiceRegistrations = new ArrayList<>();
	private ConfigurationPersistenceManager _configurationPersistenceManager;
	private ServiceRegistration<?>
		_configurationPersistenceManagerServiceRegistration;
	private ServiceRegistration<ConfigurationUpgradeStepFactory>
		_configurationUpgradeStepFactoryServiceRegistration;

}