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
import com.liferay.portal.kernel.util.HttpComponentsUtil;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.URLCodec;
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
			keyValuePair -> {
				String name = keyValuePair.getKey();
				String value = keyValuePair.getValue();

				if (!_isExcludedParameter(name)) {
					value = _encryptValue(key, value);
				}

				return new KeyValuePair(name, value);
			});

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

			Map<String, String[]> parameterMap =
				HttpComponentsUtil.getParameterMap(
					HttpComponentsUtil.getQueryString(httpServletRequest));

			httpServletRequest = new DecryptedHttpServletRequest(
				httpServletRequest,
				_getDecryptedValuesMap(company.getKeyObj(), parameterMap));
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

	private static Map<String, Map<String, String>> _getDecryptedValuesMap(
		Key key, Map<String, String[]> parameterMap) {

		Map<String, Map<String, String>> decryptedValuesMap = new HashMap<>();

		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String name = null;

			try {
				name = URLCodec.decodeURL(entry.getKey());
			}
			catch (IllegalArgumentException illegalArgumentException) {
				if (_log.isDebugEnabled()) {
					_log.debug(illegalArgumentException);
				}

				continue;
			}

			if (_isExcludedParameter(name)) {
				continue;
			}

			Map<String, String> decryptedValues =
				decryptedValuesMap.computeIfAbsent(
					name, parameterName -> new HashMap<>());

			for (String value : entry.getValue()) {
				if (Validator.isNull(value) ||
					decryptedValues.containsKey(value)) {

					continue;
				}

				try {
					decryptedValues.put(
						value, EncryptorUtil.decrypt(key, value));
				}
				catch (EncryptorException encryptorException) {
					if (_log.isDebugEnabled()) {
						_log.debug(encryptorException);
					}

					decryptedValues.put(value, StringPool.BLANK);
				}
			}
		}

		return decryptedValuesMap;
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
		SetUtil.fromArray("p_auth", WebKeys.ENCRYPT);

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
			HttpServletRequest httpServletRequest,
			Map<String, Map<String, String>> decryptedValuesMap) {

			super(httpServletRequest);

			Map<String, String[]> parameterMap =
				httpServletRequest.getParameterMap();

			for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
				String name = entry.getKey();

				String[] values = entry.getValue();

				Map<String, String> decryptedValues = decryptedValuesMap.get(
					name);

				if (decryptedValues != null) {
					for (int i = 0; i < values.length; i++) {
						values[i] = decryptedValues.getOrDefault(
							values[i], values[i]);
					}
				}

				_params.put(name, values);
			}
		}

		private final Map<String, String[]> _params = new HashMap<>();

	}

}