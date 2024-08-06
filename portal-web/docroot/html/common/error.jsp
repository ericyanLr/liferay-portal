<%--
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/html/common/init.jsp" %>

<%@ page isErrorPage="true" %>

<%
String message = null;

StringBundler sb = new StringBundler(9);

sb.append("User ID ");
sb.append(request.getRemoteUser());
sb.append(", current URL ");
sb.append(PortalUtil.getCurrentURL(request));
sb.append(", referer ");
sb.append(request.getHeader("Referer"));
sb.append(", remote address ");
sb.append(request.getRemoteAddr());

Exception strutsUtilException = (Exception)request.getAttribute(StrutsUtil.EXCEPTION);

if (strutsUtilException == null) {
	sb.append(", null exception");
}

if (strutsUtilException != null) {
	message = strutsUtilException.getMessage();
}

if (strutsUtilException instanceof PrincipalException) {
	if (strutsUtilException != null) {
		_log.warn(strutsUtilException);
	}
	else {
		_log.warn(sb.toString());
	}
}
else {
	if (strutsUtilException != null) {
		_log.error(strutsUtilException);
	}
	else {
		_log.error(sb.toString());
	}
}
%>

<center>
	<br />

	<table border="0" cellpadding="0" cellspacing="0" width="95%">
		<tr>
			<td>
				<font color="#FF0000" face="Verdana, Tahoma, Arial" size="2">
					<c:choose>
						<c:when test="<%= strutsUtilException instanceof PrincipalException %>">
							<liferay-ui:message key="you-do-not-have-permission-to-view-this-page" />
						</c:when>
						<c:otherwise>
							<liferay-ui:message key="an-unexpected-system-error-occurred" />
						</c:otherwise>
					</c:choose>

					<br />
				</font>

				<c:if test="<%= message != null %>">
					<br />

					<%= HtmlUtil.escape(message) %>
				</c:if>
			</td>
		</tr>
	</table>

	<br />
</center>

<%!
private static final Log _log = LogFactoryUtil.getLog("portal_web.docroot.html.common.error_jsp");
%>