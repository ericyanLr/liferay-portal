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

package com.liferay.configuration.admin.web.internal.exporter;

import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayOutputStream;
import com.liferay.portal.kernel.settings.LocationVariableResolver;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.cm.file.ConfigurationHandler;

/**
 * @author Pei-Jung Lan
 */
public class ConfigurationExporter {

	public static String escapeLocationVariable(String property) {
		String escapedProperty = StringUtil.replaceFirst(
			property, "${", "$\\{");

		return StringUtil.replaceLast(escapedProperty, "}", "\\}");
	}

	public static Object escapeProperties(
		Object properties, LocationVariableResolver locationVariableResolver) {

		if (properties instanceof String) {
			if (locationVariableResolver.isLocationVariable(
				String.valueOf(properties))) {

				return escapeLocationVariable(String.valueOf(properties));
			}

			return properties;
		}

		if (properties instanceof String[]) {
			List<String> escapedProperties = new ArrayList<>();

			for (String property :
				ArrayUtil.toStringArray((Object[])properties)) {

				if (locationVariableResolver.isLocationVariable(
					String.valueOf(properties))) {

					escapedProperties.add(property);
				}

				escapedProperties.add(property);
			}

			return ArrayUtil.toStringArray(escapedProperties);
		}

		return properties;
	}

	public static byte[] getPropertiesAsBytes(Dictionary<?, ?> properties)
		throws Exception {

		UnsyncByteArrayOutputStream unsyncByteArrayOutputStream =
			new UnsyncByteArrayOutputStream();

		ConfigurationHandler.write(unsyncByteArrayOutputStream, properties);

		return unsyncByteArrayOutputStream.toByteArray();
	}

}