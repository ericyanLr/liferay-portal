/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.configuration.persistence.internal.configuration.persistence.listener;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;
import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListener;
import com.liferay.portal.configuration.persistence.listener.ConfigurationModelListenerException;
import com.liferay.portal.kernel.dao.jdbc.DataAccess;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.Serializable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.Dictionary;

/**
 * @author Drew Brokke
 */
public class ConfigurationImportGlobalConfigurationModelListener
	implements ConfigurationModelListener {

	@Override
	public void onBeforeSave(String pid, Dictionary<String, Object> properties)
		throws ConfigurationModelListenerException {

		try {
			for (ExtendedObjectClassDefinition.Scope scope :
					ExtendedObjectClassDefinition.Scope.values()) {

				String portablePropertyKey = scope.getPortablePropertyKey();

				if (portablePropertyKey == null) {
					continue;
				}

				Object portableIdentifier = properties.remove(
					portablePropertyKey);

				if (portableIdentifier == null) {
					continue;
				}

				Serializable internalIdentifier = _getInternalIdentifier(
					scope, (Serializable)portableIdentifier);

				if (internalIdentifier != null) {
					if (_log.isInfoEnabled()) {
						_log.info(
							String.format(
								"For pid %s: replacing portable identifier " +
									"%s with internal identifier %s",
								pid, portableIdentifier, internalIdentifier));
					}

					properties.put(scope.getPropertyKey(), internalIdentifier);

					break;
				}
			}
		}
		catch (Exception exception) {
			throw new ConfigurationModelListenerException(
				exception, Object.class,
				ConfigurationImportGlobalConfigurationModelListener.class,
				properties);
		}
	}

	private Serializable _getInternalIdentifier(
			ExtendedObjectClassDefinition.Scope scope,
			Serializable portableIdentifier)
		throws Exception {

		if (scope.equals(ExtendedObjectClassDefinition.Scope.COMPANY)) {
			try (Connection connection = DataAccess.getConnection();
				PreparedStatement preparedStatement =
					connection.prepareStatement(
						"SELECT companyId FROM Company WHERE webId = ?")) {

				preparedStatement.setString(1, (String)portableIdentifier);

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getLong("companyId");
					}
				}
			}

			return null;
		}

		if (scope.equals(ExtendedObjectClassDefinition.Scope.GROUP)) {
			String[] parts = StringUtil.split(
				(String)portableIdentifier, _SEPARATOR);

			String webId = parts[0];

			long companyId = GetterUtil.getLong(
				_getInternalIdentifier(
					ExtendedObjectClassDefinition.Scope.COMPANY, webId));

			if (companyId == 0L) {
				return null;
			}

			String groupKey = parts[1];

			try (Connection connection = DataAccess.getConnection();
				PreparedStatement preparedStatement =
					connection.prepareStatement(
						"SELECT groupId FROM Group_ WHERE companyId = ? AND " +
							"groupKey = ?")) {

				preparedStatement.setString(1, String.valueOf(companyId));
				preparedStatement.setString(2, groupKey);

				try (ResultSet resultSet = preparedStatement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getLong("groupId");
					}
				}
			}

			return null;
		}

		return null;
	}

	private static final String _SEPARATOR = "--";

	private static final Log _log = LogFactoryUtil.getLog(
		ConfigurationImportGlobalConfigurationModelListener.class);

}