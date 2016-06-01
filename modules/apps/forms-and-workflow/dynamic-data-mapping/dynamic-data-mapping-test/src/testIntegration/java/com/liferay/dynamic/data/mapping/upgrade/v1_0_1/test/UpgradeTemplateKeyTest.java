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

package com.liferay.dynamic.data.mapping.upgrade.v1_0_1.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.dynamic.data.mapping.upgrade.v1_0_1.UpgradeTemplateKey;
import com.liferay.dynamic.data.mapping.upgrade.v1_0_1.util.DDMTemplateTable;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;
import com.liferay.portal.kernel.upgrade.util.UpgradeColumn;
import com.liferay.portal.test.log.CaptureAppender;
import com.liferay.portal.test.log.Log4JLoggerTestUtil;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Eric Yan
 */
@RunWith(Arquillian.class)
public class UpgradeTemplateKeyTest extends UpgradeTemplateKey {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_isSetUpRunning = true;

		try (Connection con = DataAccess.getUpgradeOptimizedConnection()) {
			connection = con;

			try (CaptureAppender captureAppender =
					 Log4JLoggerTestUtil.configureLog4JLogger(
						 UpgradeProcess.class.getName(), Level.WARN)) {
				alter(
					DDMTemplateTable.class,
					new AlterColumnType("templateKey", "STRING"));

				List<LoggingEvent> loggingEvents =
					captureAppender.getLoggingEvents();

				Assert.assertEquals(1, loggingEvents.size());

				LoggingEvent loggingEvent = loggingEvents.get(0);

				Assert.assertEquals(
					"Fallback to recreating the table",
					loggingEvent.getMessage());
			}
		}
		finally {
			_isSetUpRunning = false;
		}
	}

	@Test
	public void testUpgrade() throws Exception {
		try (Connection con = DataAccess.getUpgradeOptimizedConnection()) {
			DatabaseMetaData metadata = con.getMetaData();

			try (ResultSet templateKeyColumnResultSet = metadata.getColumns(
					null, null, normalizeName("ddmtemplate", metadata),
					normalizeName("templateKey", metadata))) {

				Assert.assertTrue(templateKeyColumnResultSet.next());

				int columnDataType = templateKeyColumnResultSet.getInt(
					"DATA_TYPE");

				Assert.assertNotEquals(Types.VARCHAR, columnDataType);
			}

			upgrade();

			metadata = con.getMetaData();

			try (ResultSet templateKeyColumnResultSet = metadata.getColumns(
					null, null, normalizeName("ddmtemplate", metadata),
					normalizeName("templateKey", metadata))) {

				Assert.assertTrue(templateKeyColumnResultSet.next());

				int columnDataType = templateKeyColumnResultSet.getInt(
					"DATA_TYPE");

				Assert.assertEquals(Types.VARCHAR, columnDataType);
			}
		}
	}

	@Override
	protected void upgradeTable(
			String tableName, Object[][] tableColumns, String createSQL,
			String[] indexesSQL, UpgradeColumn... upgradeColumns)
		throws Exception {

		if (_isSetUpRunning) {
			return;
		}

		DB db = DBManagerUtil.getDB();

		Assert.assertEquals(DBType.DB2, db.getDBType());

		super.upgradeTable(
			tableName, tableColumns, createSQL, indexesSQL, upgradeColumns);
	}

	private boolean _isSetUpRunning;

}