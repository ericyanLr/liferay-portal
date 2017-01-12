<%@ page
	import="com.liferay.portal.kernel.servlet.PersistentHttpServletRequestWrapper" %><%--
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
--%>

<%@ include file="/init.jsp" %>

<c:if test="<%= LayoutPermissionUtil.contains(permissionChecker, layout, ActionKeys.UPDATE) %>">
	<div class="alert alert-info hide" id="<portlet:namespace />nested-portlets-msg">
		<liferay-ui:message key="drag-portlets-below-to-nest-them" />
	</div>

	<aui:script sandbox="<%= true %>">
		var portletWrapper = $('#p_p_id_<%= portletDisplay.getId() %>_');

		var nestedPortlet = portletWrapper.find('.portlet-boundary, .portlet-borderless-container');

		if (!nestedPortlet.length) {
			portletWrapper.find('#<portlet:namespace />nested-portlets-msg').first().removeClass('hide');
		}
	</aui:script>
</c:if>

<%
try {
	String templateId = (String)request.getAttribute(NestedPortletsWebKeys.TEMPLATE_ID + portletDisplay.getId());
	String templateContent = (String)request.getAttribute(NestedPortletsWebKeys.TEMPLATE_CONTENT + portletDisplay.getId());

	if (Validator.isNotNull(templateId) && Validator.isNotNull(templateContent)) {
		HttpServletRequest originalRequest = null;

		HttpServletRequestWrapper currentRequestWrapper = null;

		HttpServletRequest currentRequest = request;
		HttpServletRequest nextRequest = null;

		while (currentRequest instanceof HttpServletRequestWrapper) {
			if (currentRequest instanceof
				PersistentHttpServletRequestWrapper) {

				PersistentHttpServletRequestWrapper
					persistentHttpServletRequestWrapper =
					(PersistentHttpServletRequestWrapper) currentRequest;

				persistentHttpServletRequestWrapper =
					persistentHttpServletRequestWrapper.clone();

				if (originalRequest == null) {
					originalRequest = persistentHttpServletRequestWrapper;
				}

				if (currentRequestWrapper != null) {
					currentRequestWrapper.setRequest(
						persistentHttpServletRequestWrapper);
				}

				currentRequestWrapper = persistentHttpServletRequestWrapper;
			}

			// Get original request so that portlets inside portlets render
			// properly

			HttpServletRequestWrapper httpServletRequestWrapper =
				(HttpServletRequestWrapper) currentRequest;

			nextRequest =
				(HttpServletRequest) httpServletRequestWrapper.getRequest();

			if ((currentRequest.getDispatcherType() != DispatcherType.INCLUDE) &&
				(currentRequest.getDispatcherType() != nextRequest.getDispatcherType())) {

				break;
			}

			currentRequest = nextRequest;
		}

		if (currentRequestWrapper != null) {
			currentRequestWrapper.setRequest(currentRequest);
		}

		if (originalRequest == null) {
			originalRequest = currentRequest;
		}

		RuntimePageUtil.processTemplate(originalRequest, response, new StringTemplateResource(templateId, templateContent));
	}
}
catch (Exception e) {
	_log.error("Cannot render Nested Portlets portlet", e);
}
finally {
	liferayPortletRequest.defineObjects(portletConfig, renderResponse);
}
%>

<%!
private static Log _log = LogFactoryUtil.getLog("com_liferay_nested_portlets_web.view_jsp");
%>