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

package com.liferay.journal.internal.servlet.filter;

import com.liferay.journal.model.JournalArticleImage;
import com.liferay.journal.service.JournalArticleImageLocalService;
import com.liferay.journal.service.permission.JournalArticlePermission;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.security.permission.PermissionCheckerFactory;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.servlet.BaseFilter;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Portal;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Balázs Sáfrány-Kovalik
 */
@Component(
	immediate = true,
	property = {
		"servlet-context-name=",
		"servlet-filter-name=Journal Article Image Filter",
		"url-pattern=/image/journal/article/*"
	},
	service = Filter.class
)
public class JournalArticleImageFilter extends BaseFilter {

	@Override
	protected Log getLog() {
		return _log;
	}

	@Override
	protected void processFilter(
			HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain)
		throws Exception {

		long imageId = ParamUtil.getLong(request, "img_id");

		if (imageId > 0) {
			JournalArticleImage journalArticleImage =
				_journalArticleImageLocalService.fetchJournalArticleImage(
					imageId);

			if (journalArticleImage != null) {
				User user = _portal.getUser(request);

				if (user == null) {
					long companyId = _portal.getCompanyId(request);

					user = _userLocalService.getDefaultUser(companyId);
				}

				try {
					PermissionChecker permissionChecker =
						_permissionCheckerFactory.create(user);

					JournalArticlePermission.check(
						permissionChecker, journalArticleImage.getGroupId(),
						journalArticleImage.getArticleId(), ActionKeys.VIEW);
				}
				catch (PrincipalException pe) {
					_processPrincipalException(pe, user, request, response);
				}
				catch (Exception e) {
					_portal.sendError(e, request, response);
				}
			}
		}

		processFilter(
			JournalArticleImage.class.getName(), request, response,
			filterChain);
	}

	private void _processPrincipalException(
			Throwable t, User user, HttpServletRequest request,
			HttpServletResponse response)
		throws IOException, ServletException {

		if (!user.isDefaultUser()) {
			_portal.sendError(
				HttpServletResponse.SC_UNAUTHORIZED, (Exception)t, request,
				response);

			return;
		}

		String redirect = _portal.getPathMain() + "/portal/login";

		String currentURL = _portal.getCurrentURL(request);

		redirect = _http.addParameter(redirect, "redirect", currentURL);

		response.sendRedirect(redirect);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		JournalArticleImageFilter.class);

	@Reference
	private Http _http;

	@Reference
	private JournalArticleImageLocalService _journalArticleImageLocalService;

	@Reference
	private PermissionCheckerFactory _permissionCheckerFactory;

	@Reference
	private Portal _portal;

	@Reference
	private UserLocalService _userLocalService;

}