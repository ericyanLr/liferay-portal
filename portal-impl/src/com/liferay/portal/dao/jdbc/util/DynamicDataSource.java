/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.dao.jdbc.util;

import com.liferay.petra.lang.CentralizedThreadLocal;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.ProxyUtil;
import com.liferay.portal.spring.hibernate.SpringHibernateThreadLocalUtil;

import java.io.PrintWriter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

import java.util.Objects;
import java.util.logging.Logger;

import javax.sql.DataSource;

/**
 * @author Dante Wang
 */
public class DynamicDataSource implements DataSource {

	public DynamicDataSource(
		DataSource readDataSource, DataSource writeDataSource) {

		_readDataSource = readDataSource;
		_writeDataSource = writeDataSource;
	}

	@Override
	public Connection getConnection() throws SQLException {
		DataSource dataSource = _getDataSource();

		Connection connection = dataSource.getConnection();

		if (dataSource == _writeDataSource) {
			return (Connection)ProxyUtil.newProxyInstance(
				DynamicDataSource.class.getClassLoader(),
				new Class<?>[] {Connection.class},
				new WriteDataSourceInvocationHandler(connection));
		}

		return connection;
	}

	@Override
	public Connection getConnection(String userName, String password)
		throws SQLException {

		DataSource dataSource = _getDataSource();

		Connection connection = dataSource.getConnection(userName, password);

		if (dataSource == _writeDataSource) {
			return (Connection)ProxyUtil.newProxyInstance(
				DynamicDataSource.class.getClassLoader(),
				new Class<?>[] {Connection.class},
				new WriteDataSourceInvocationHandler(connection));
		}

		return connection;
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		DataSource dataSource = _getDataSource();

		return dataSource.getLoginTimeout();
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		DataSource dataSource = _getDataSource();

		return dataSource.getLogWriter();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		DataSource dataSource = _getDataSource();

		return dataSource.getParentLogger();
	}

	public DataSource getReadDataSource() {
		return _readDataSource;
	}

	public DataSource getWriteDataSource() {
		return _writeDataSource;
	}

	@Override
	public boolean isWrapperFor(Class<?> clazz) throws SQLException {
		DataSource dataSource = _getDataSource();

		return dataSource.isWrapperFor(clazz);
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		DataSource dataSource = _getDataSource();

		dataSource.setLoginTimeout(seconds);
	}

	@Override
	public void setLogWriter(PrintWriter printWriter) throws SQLException {
		DataSource dataSource = _getDataSource();

		dataSource.setLogWriter(printWriter);
	}

	@Override
	public <T> T unwrap(Class<T> clazz) throws SQLException {
		DataSource dataSource = _getDataSource();

		return dataSource.unwrap(clazz);
	}

	private boolean _delayProvidingReadDataSource() {
		long elapsedTime =
			System.currentTimeMillis() - _writeDataSourceLastConnectionTime;

		if (elapsedTime < 1000) {
			return true;
		}

		return false;
	}

	private DataSource _getDataSource() {
		if (!_writeDataSourceThreadLocal.get() &&
			SpringHibernateThreadLocalUtil.isCurrentTransactionReadOnly() &&
			!_delayProvidingReadDataSource()) {

			if (_log.isTraceEnabled()) {
				_log.trace("Returning read data source");
			}

			return _readDataSource;
		}

		if (_log.isTraceEnabled()) {
			_log.trace("Returning write data source");
		}

		_writeDataSourceThreadLocal.set(true);

		return _writeDataSource;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DynamicDataSource.class);

	private static volatile long _writeDataSourceLastConnectionTime;
	private static final ThreadLocal<Boolean> _writeDataSourceThreadLocal =
		new CentralizedThreadLocal<>(
			DynamicDataSource.class + "._writeDataSourceThreadLocal",
			() -> false);

	private final DataSource _readDataSource;
	private final DataSource _writeDataSource;

	private static class WriteDataSourceInvocationHandler
		implements InvocationHandler {

		@Override
		public Object invoke(Object object, Method method, Object[] args)
			throws Throwable {

			String methodName = method.getName();

			if (Objects.equals(methodName, "close") && _hasCommits) {
				_writeDataSourceLastConnectionTime = System.currentTimeMillis();
			}
			else if (Objects.equals(methodName, "commit") && !_hasCommits) {
				_hasCommits = true;
			}

			return method.invoke(_target, args);
		}

		private WriteDataSourceInvocationHandler(Object target) {
			_target = target;
		}

		private boolean _hasCommits;
		private final Object _target;

	}

}