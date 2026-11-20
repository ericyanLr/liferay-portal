/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portlet.internal;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.encryptor.EncryptorException;
import com.liferay.portal.kernel.encryptor.EncryptorUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalServiceUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.security.Key;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

			httpServletRequest = new DecryptedHttpServletRequest(
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

	private static boolean _isExcludedParameter(String name) {
		return _excludedParameterNames.contains(name);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PortletURLEncryptionUtil.class);

	private static final Set<String> _excludedParameterNames =
		SetUtil.fromArray(WebKeys.ENCRYPT);

	private static class DecryptedHttpServletRequest
		extends HttpServletRequestWrapper {

		@Override
		public String getParameter(String name) {
			String[] values = _params.get(name);

			if (ArrayUtil.isNotEmpty(values)) {
				return values[0];
			}

			return null;
		}

		@Override
		public Map<String, String[]> getParameterMap() {
			return Collections.unmodifiableMap(_params);
		}

		@Override
		public String[] getParameterValues(String name) {
			return _params.get(name);
		}

		private DecryptedHttpServletRequest(
			HttpServletRequest httpServletRequest, Key key) {

			super(httpServletRequest);

			Map<String, String[]> parameters =
				httpServletRequest.getParameterMap();

			for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
				String name = entry.getKey();

				String[] values = entry.getValue();

				for (int i = 0; i < values.length; i++) {
					if (_isExcludedParameter(name) ||
						Validator.isNull(values[i])) {

						continue;
					}

					try {
						values[i] = EncryptorUtil.decrypt(key, values[i]);
					}
					catch (EncryptorException encryptorException) {
						if (_log.isDebugEnabled()) {
							_log.debug(encryptorException);
						}

						values[i] = StringPool.BLANK;
					}
				}

				_params.put(name, values);
			}
		}

		private final Map<String, String[]> _params = new HashMap<>();

	}

}