/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portlet.internal;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.portal.kernel.encryptor.EncryptorException;
import com.liferay.portal.kernel.encryptor.EncryptorUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.servlet.EncryptedServletRequest;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Key;

import java.util.List;

/**
 * @author Eric Yan
 */
public class PortletURLEncryptionUtil {

	public static List<KeyValuePair> encryptParameters(
		long companyId, List<KeyValuePair> parameters) {

		Key key = _getKey(companyId);

		List<KeyValuePair> encryptedParameters = TransformUtil.transform(
			parameters,
			keyValuePair -> new KeyValuePair(
				keyValuePair.getKey(),
				_encryptValue(key, keyValuePair.getValue())));

		encryptedParameters.add(new KeyValuePair(WebKeys.ENCRYPT, "1"));

		return encryptedParameters;
	}

	public static HttpServletRequest getDecryptedHttpServletRequest(
		long companyId, HttpServletRequest httpServletRequest) {

		boolean encryptRequest = ParamUtil.getBoolean(
			httpServletRequest, WebKeys.ENCRYPT);

		if (!encryptRequest) {
			return httpServletRequest;
		}

		try {
			Company company = CompanyLocalServiceUtil.getCompanyById(companyId);

			httpServletRequest = new EncryptedServletRequest(
				httpServletRequest, company.getKeyObj());
		}
		catch (Exception exception) {
			if (_log.isDebugEnabled()) {
				_log.debug(exception);
			}
		}

		return httpServletRequest;
	}

	private static String _encryptValue(Key key, String value) {
		if (key == null) {
			return value;
		}

		try {
			return EncryptorUtil.encrypt(key, value);
		}
		catch (EncryptorException encryptorException) {
			if (_log.isDebugEnabled()) {
				_log.debug(encryptorException);
			}

			return value;
		}
	}

	private static Key _getKey(long companyId) {
		try {
			Company company = CompanyLocalServiceUtil.getCompanyById(companyId);

			return company.getKeyObj();
		}
		catch (Exception exception) {
			_log.error("Unable to get company key", exception);
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PortletURLEncryptionUtil.class);

}