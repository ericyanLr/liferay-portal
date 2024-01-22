/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.http.internal.configuration;

import aQute.bnd.annotation.metatype.Meta;

import com.liferay.portal.configuration.metatype.annotations.ExtendedObjectClassDefinition;

/**
 * @author Eric Yan
 */
@ExtendedObjectClassDefinition(category = "infrastructure")
@Meta.OCD(
	id = "com.liferay.portal.http.internal.configuration.HttpConfiguration",
	localization = "content/Language", name = "http-configuration-name"
)
public interface HttpConfiguration {

	@Meta.AD(
		deflt = "2", description = "default-max-connections-per-host-help",
		name = "default-max-connections-per-host", required = false
	)
	public int defaultMaxConnectionsPerHost();

	@Meta.AD(
		deflt = "", description = "max-connections-per-host-help",
		name = "max-connections-per-host", required = false
	)
	public String[] maxConnectionsPerHost();

	@Meta.AD(
		deflt = "20", description = "max-total-connections-help",
		name = "max-total-connections", required = false
	)
	public int maxTotalConnections();

	@Meta.AD(
		description = "proxy-authentication-type-help",
		name = "proxy-authentication-type",
		optionLabels = {
			"proxy-authentication-type-username-password",
			"proxy-authentication-type-ntlm"
		},
		optionValues = {"username-password", "ntlm"}, required = false
	)
	public String proxyAuthenticationType();

	@Meta.AD(
		deflt = "0", description = "keep-alive-timeout-help",
		name = "keep-alive-timeout", required = false
	)
	public int keepAliveTimeout();

	@Meta.AD(
		deflt = "false", description = "tcp-keep-alive-enabled-help",
		name = "tcp-keep-alive-enabled", required = false
	)
	public boolean tcpKeepAliveEnabled();

}