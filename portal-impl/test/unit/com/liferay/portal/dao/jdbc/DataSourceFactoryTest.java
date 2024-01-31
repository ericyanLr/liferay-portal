/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.dao.jdbc;

import com.liferay.portal.dao.jdbc.util.DataSourceWrapper;
import com.liferay.portal.kernel.dao.jdbc.DataSourceFactory;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import com.zaxxer.hikari.HikariDataSource;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.spi.NamingManager;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Eric Yan
 */
public class DataSourceFactoryTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testDataSourceCloseable() throws Exception {
		Properties properties = new Properties();

		properties.setProperty("driverClassName", "com.mysql.cj.jdbc.Driver");

		_testDataSourceCloseable(true, properties);
	}

	@Test
	public void testDataSourceCloseableWithJNDI() throws Exception {
		NamingManager.setInitialContextFactoryBuilder(
			environment -> environment1 -> new InitialContext() {

				@Override
				public Object lookup(String name) {
					return new HikariDataSource();
				}

			});

		Properties properties = new Properties();

		properties.setProperty("jndi.name", "jdbc/test");

		_testDataSourceCloseable(false, properties);
	}

	private void _testDataSourceCloseable(
			boolean expectedClosed, Properties properties)
		throws Exception {

		DataSource dataSource = _dataSourceFactory.initDataSource(properties);

		_dataSourceFactory.destroyDataSource(dataSource);

		while (dataSource instanceof DataSourceWrapper) {
			DataSourceWrapper dataSourceWrapper = (DataSourceWrapper)dataSource;

			dataSource = dataSourceWrapper.getWrappedDataSource();
		}

		HikariDataSource hikariDataSource = (HikariDataSource)dataSource;

		Assert.assertEquals(expectedClosed, hikariDataSource.isClosed());
	}

	private final DataSourceFactory _dataSourceFactory =
		new DataSourceFactoryImpl();

}