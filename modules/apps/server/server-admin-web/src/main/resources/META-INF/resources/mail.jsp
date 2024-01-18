<%--
/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<liferay-ui:error exception="<%= CaptchaConfigurationException.class %>" message="a-captcha-error-occurred-please-contact-an-administrator" />
<liferay-ui:error exception="<%= CaptchaException.class %>" message="captcha-verification-failed" />
<liferay-ui:error exception="<%= CaptchaTextException.class %>" message="text-verification-failed" />

<div class="sheet">
	<div class="panel-group panel-group-flush">
		<liferay-util:include page="/mail_fields.jsp" servletContext="<%= application %>">
			<liferay-util:param name="companyId" value="<%= String.valueOf(CompanyConstants.SYSTEM) %>" />
		</liferay-util:include>

		<c:if test="<%= captchaConfiguration.mailCaptchaEnabled() %>">
			<liferay-captcha:captcha />
		</c:if>

		<aui:button-row>
			<aui:button cssClass="save-server-button" data-cmd="updateMail" primary="<%= true %>" value="save" />
		</aui:button-row>
	</div>
</div>