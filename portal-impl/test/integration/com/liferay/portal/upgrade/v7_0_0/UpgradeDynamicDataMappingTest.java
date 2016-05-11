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

package com.liferay.portal.upgrade.v7_0_0;

import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.upgrade.util.UpgradeColumn;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.upgrade.v6_2_0.util.DDMTemplateTable;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Eric Yan
 */
public class UpgradeDynamicDataMappingTest extends UpgradeDynamicDataMapping {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		try (Connection con = DataAccess.getUpgradeOptimizedConnection()) {
			connection = con;

			alter(
				DDMTemplateTable.class,
				new AlterColumnType("templateKey", "STRING"));
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

		DB db = DBManagerUtil.getDB();

		Assert.assertEquals(DBType.DB2, db.getDBType());

		super.upgradeTable(
			tableName, tableColumns, createSQL, indexesSQL, upgradeColumns);
	}

}