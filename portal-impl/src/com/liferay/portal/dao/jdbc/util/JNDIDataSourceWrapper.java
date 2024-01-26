/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.dao.jdbc.util;

import javax.sql.DataSource;

/**
 * @author Eric Yan
 */
public class JNDIDataSourceWrapper extends DataSourceWrapper {

	public JNDIDataSourceWrapper(DataSource dataSource) {
		super(dataSource);
	}

}