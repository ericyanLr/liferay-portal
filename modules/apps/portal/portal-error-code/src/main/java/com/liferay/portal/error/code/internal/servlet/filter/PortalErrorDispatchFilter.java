/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.error.code.internal.servlet.filter;

import com.liferay.portal.kernel.db.partition.DBPartition;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.CompanyConstants;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.servlet.BaseFilter;
import com.liferay.portal.kernel.util.Portal;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eric Yan
 */
@Component(
	property = {
		"dispatcher=ERROR", "servlet-context-name=",
		"servlet-filter-name=Portal Error Dispatch Filter", "url-pattern=/*"
	},
	service = Filter.class
)
public class PortalErrorDispatchFilter extends BaseFilter {

	@Override
	protected Log getLog() {
		return _log;
	}

	@Override
	protected void processFilter(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, FilterChain filterChain)
		throws Exception {

		if (DBPartition.isPartitionEnabled() &&
			(CompanyThreadLocal.getCompanyId() == CompanyConstants.SYSTEM)) {

			CompanyThreadLocal.setCompanyId(
				_portal.getCompanyId(httpServletRequest));
		}

		filterChain.doFilter(httpServletRequest, httpServletResponse);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PortalErrorDispatchFilter.class);

	@Reference
	private Portal _portal;

}