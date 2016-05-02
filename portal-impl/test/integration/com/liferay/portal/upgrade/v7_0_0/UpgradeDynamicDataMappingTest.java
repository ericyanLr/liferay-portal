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

import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Eric Yan
 */
public class UpgradeDynamicDataMappingTest extends UpgradeKernelPackage {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		connection = DataAccess.getUpgradeOptimizedConnection();
	}

	@After
	public void tearDown() throws Exception {
		connection.close();
	}

	@Test
	public void testUpgrade() throws Exception {
		DatabaseMetaData metadata = connection.getMetaData();

		String tableName = "ddmtemplate";
		String columnName = "templateKey";

		ResultSet columnResultSet = metadata.getColumns(
			null, null, tableName, columnName);

		if (columnResultSet.next()) {
			testTableName(tableName, columnResultSet);

			testColumnName(columnName, columnResultSet);

			testColumnDataType(Types.VARCHAR, columnResultSet);
		}
		else {
			Assert.fail("Could not retrieve metadata for table: " + tableName);
		}
	}

	public void testColumnName(String expectedColumnName, ResultSet resultSet)
		throws SQLException {

		String resultColumnName = resultSet.getString("COLUMN_NAME");

		StringBundler sb = new StringBundler(4);

		sb.append(
			"Retrieved metadata does not match the expected column name: ");
		sb.append(expectedColumnName);
		sb.append(". Instead, it has a column name of: ");
		sb.append(resultColumnName);

		Assert.assertEquals(sb.toString(), expectedColumnName,
			resultColumnName);
	}

	public void testTableName(String expectedTableName, ResultSet resultSet)
		throws SQLException{

		String resultTableName = resultSet.getString("TABLE_NAME");

		StringBundler sb = new StringBundler(4);

		sb.append(
			"Retrieved metadata does not match the expected table name: ");
		sb.append(expectedTableName);
		sb.append(". Instead, it has a table name of:");
		sb.append(resultTableName);

		Assert.assertEquals(sb.toString(), expectedTableName, resultTableName);
	}

	public void testColumnDataType(int expectedColumnDataType,
								   ResultSet resultSet) throws SQLException {

		String resultColumnName = resultSet.getString("COLUMN_NAME");
		int resultDataType = resultSet.getInt("DATA_TYPE");
		String resultDataTypeName = resultSet.getString("TYPE_NAME");

		StringBundler sb = new StringBundler(7);

		sb.append("Column ");
		sb.append(resultColumnName);
		sb.append(" does not have the expected data type VARCHAR.");
		sb.append(" Instead, it has a data type: ");
		sb.append(resultDataTypeName);

		Assert.assertEquals(sb.toString(), resultDataType,
			expectedColumnDataType);
	}

}