/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.dao.orm.hibernate;

import java.io.Serializable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

import java.util.Date;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 * @author Tina Tian
 */
public class DateType implements Serializable, UserType<Date> {

	@Override
	public Date assemble(Serializable cached, Object owner) {
		return deepCopy((Date)cached);
	}

	@Override
	public Date deepCopy(Date object) {
		if (object == null) {
			return null;
		}

		return new Date(object.getTime());
	}

	@Override
	public Serializable disassemble(Date value) {
		return deepCopy(value);
	}

	@Override
	public boolean equals(Date x, Date y) {
		if (x == y) {
			return true;
		}
		else if ((x == null) || (y == null)) {
			return false;
		}

		if (x.getTime() == y.getTime()) {
			return true;
		}

		return false;
	}

	@Override
	public int getSqlType() {
		return Types.TIMESTAMP;
	}

	@Override
	public int hashCode(Date x) {
		return x.hashCode();
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Date nullSafeGet(
			ResultSet resultSet, int index, WrapperOptions wrapperOptions)
		throws SQLException {

		Timestamp timestamp = resultSet.getTimestamp(index);

		if (resultSet.wasNull()) {
			return null;
		}

		return new Date(timestamp.getTime());
	}

	@Override
	public void nullSafeSet(
			PreparedStatement preparedStatement, Date target, int index,
			WrapperOptions wrapperOptions)
		throws SQLException {

		if (target == null) {
			preparedStatement.setTimestamp(index, null);
		}
		else {
			Timestamp timestamp = new Timestamp(target.getTime());

			preparedStatement.setTimestamp(index, timestamp);
		}
	}

	@Override
	public Date replace(Date original, Date target, Object owner) {
		return deepCopy(original);
	}

	@Override
	public Class<Date> returnedClass() {
		return Date.class;
	}

}