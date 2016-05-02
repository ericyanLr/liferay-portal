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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
public class UpgradeDynamicDataMappingTest extends UpgradeDynamicDataMapping {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new LiferayIntegrationTestRule();

	@Before
	public void setUp() throws Exception {
		_connection = DataAccess.getUpgradeOptimizedConnection();
	}

	@After
	public void tearDown() throws Exception {
		_connection.close();
	}

	@Test
	public void testUpgrade() throws Exception {
		testUpgradeDDMTemplateTemplateKey();
	}

	protected void testUpgradeDDMTemplateTemplateKey() throws Exception{
		String tableName = "ddmtemplate";
		String columnName = "templateKey";

		ResultSet templateKeyColumnResultSet = getColumnResultSet(
			_connection, tableName, columnName);

		if(templateKeyColumnResultSet.next()) {
			int columnDataType = templateKeyColumnResultSet.getInt("DATA_TYPE");

			if (columnDataType != Types.VARCHAR) {
				upgrade();

				templateKeyColumnResultSet = getColumnResultSet(
					_connection, tableName, columnName);

				if(templateKeyColumnResultSet.next()) {
					columnDataType = templateKeyColumnResultSet.getInt(
						"DATA_TYPE");

					StringBundler sb = new StringBundler(5);

					sb.append("Column ");
					sb.append(columnName);
					sb.append(" does not have the expected data type: VARCHAR");
					sb.append(" Instead, it has a data type: ");
					sb.append(columnDataType);

					Assert.assertEquals(
						sb.toString(), columnDataType,
						Types.VARCHAR);
				}
			}
		}
		else {
			Assert.fail(
				"Could not retrieve metadata for column: " + columnName);
		}
	}

	private Connection _connection;
}