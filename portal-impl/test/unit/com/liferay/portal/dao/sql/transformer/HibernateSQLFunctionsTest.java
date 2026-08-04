/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.dao.sql.transformer;

import com.liferay.portal.kernel.dao.db.DBType;
import com.liferay.portal.test.rule.LiferayUnitTestRule;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Eric Yan
 */
public class HibernateSQLFunctionsTest {

	@ClassRule
	@Rule
	public static final LiferayUnitTestRule liferayUnitTestRule =
		LiferayUnitTestRule.INSTANCE;

	@Test
	public void testGetCastClobTextSQL() {
		Assert.assertEquals(
			"CAST(?1 AS VARCHAR(2000))",
			HibernateSQLFunctions.getCastClobTextSQL(DBType.DB2));
		Assert.assertEquals(
			"CONVERT(?1, SQL_VARCHAR)",
			HibernateSQLFunctions.getCastClobTextSQL(DBType.HYPERSONIC));
		Assert.assertEquals(
			"?1", HibernateSQLFunctions.getCastClobTextSQL(DBType.MARIADB));
		Assert.assertEquals(
			"?1", HibernateSQLFunctions.getCastClobTextSQL(DBType.MYSQL));
		Assert.assertEquals(
			"DBMS_LOB.SUBSTR(?1, 4000, 1)",
			HibernateSQLFunctions.getCastClobTextSQL(DBType.ORACLE));
		Assert.assertEquals(
			"CAST(?1 AS TEXT)",
			HibernateSQLFunctions.getCastClobTextSQL(DBType.POSTGRESQL));
		Assert.assertEquals(
			"CAST(?1 AS NVARCHAR(MAX))",
			HibernateSQLFunctions.getCastClobTextSQL(DBType.SQLSERVER));
	}

}