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

package com.liferay.portal.kernel.portlet;

import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.expando.kernel.model.CustomAttributesDisplay;
import com.liferay.exportimport.kernel.lar.PortletDataHandler;
import com.liferay.exportimport.kernel.lar.StagedModelDataHandler;
import com.liferay.portal.kernel.atom.AtomCollectionAdapter;
import com.liferay.portal.kernel.notifications.UserNotificationDefinition;
import com.liferay.portal.kernel.notifications.UserNotificationHandler;
import com.liferay.portal.kernel.poller.PollerProcessor;
import com.liferay.portal.kernel.pop.MessageListener;
import com.liferay.portal.kernel.scheduler.messaging.SchedulerEventMessageListener;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.OpenSearch;
import com.liferay.portal.kernel.security.permission.propagator.PermissionPropagator;
import com.liferay.portal.kernel.servlet.URLEncoder;
import com.liferay.portal.kernel.template.TemplateHandler;
import com.liferay.portal.kernel.trash.TrashHandler;
import com.liferay.portal.kernel.webdav.WebDAVStorage;
import com.liferay.portal.kernel.workflow.WorkflowHandler;
import com.liferay.portal.kernel.xmlrpc.Method;
import com.liferay.social.kernel.model.SocialActivityInterpreter;
import com.liferay.social.kernel.model.SocialRequestInterpreter;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.portlet.Portlet;
import javax.portlet.PreferencesValidator;

import javax.servlet.ServletContext;

/**
 * @author Eric Yan
 */
public class DummyPortletBag implements PortletBag {

	@Override
	public Object clone() {
		return new DummyPortletBag();
	}

	@Override
	public void destroy() {
	}

	@Override
	public List<AssetRendererFactory<?>> getAssetRendererFactoryInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<AtomCollectionAdapter<?>> getAtomCollectionAdapterInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<ConfigurationAction> getConfigurationActionInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<ControlPanelEntry> getControlPanelEntryInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<CustomAttributesDisplay> getCustomAttributesDisplayInstances() {
		return Collections.emptyList();
	}

	@Override
	public FriendlyURLMapperTracker getFriendlyURLMapperTracker() {
		return null;
	}

	@Override
	public List<Indexer<?>> getIndexerInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<OpenSearch> getOpenSearchInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<PermissionPropagator> getPermissionPropagatorInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<PollerProcessor> getPollerProcessorInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<MessageListener> getPopMessageListenerInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<PortletDataHandler> getPortletDataHandlerInstances() {
		return Collections.emptyList();
	}

	@Override
	public Portlet getPortletInstance() {
		return null;
	}

	@Override
	public List<PortletLayoutListener> getPortletLayoutListenerInstances() {
		return Collections.emptyList();
	}

	@Override
	public String getPortletName() {
		return null;
	}

	@Override
	public List<PreferencesValidator> getPreferencesValidatorInstances() {
		return Collections.emptyList();
	}

	@Override
	public ResourceBundle getResourceBundle(Locale locale) {
		return null;
	}

	@Override
	public String getResourceBundleBaseName() {
		return null;
	}

	@Override
	public List<SchedulerEventMessageListener>
		getSchedulerEventMessageListeners() {

		return Collections.emptyList();
	}

	@Override
	public ServletContext getServletContext() {
		return null;
	}

	@Override
	public List<SocialActivityInterpreter>
		getSocialActivityInterpreterInstances() {

		return Collections.emptyList();
	}

	@Override
	public List<SocialRequestInterpreter>
		getSocialRequestInterpreterInstances() {

		return Collections.emptyList();
	}

	@Override
	public List<StagedModelDataHandler<?>>
		getStagedModelDataHandlerInstances() {

		return Collections.emptyList();
	}

	@Override
	public List<TemplateHandler> getTemplateHandlerInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<TrashHandler> getTrashHandlerInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<URLEncoder> getURLEncoderInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<UserNotificationDefinition>
		getUserNotificationDefinitionInstances() {

		return Collections.emptyList();
	}

	@Override
	public List<UserNotificationHandler> getUserNotificationHandlerInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<WebDAVStorage> getWebDAVStorageInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<WorkflowHandler<?>> getWorkflowHandlerInstances() {
		return Collections.emptyList();
	}

	@Override
	public List<Method> getXmlRpcMethodInstances() {
		return Collections.emptyList();
	}

	@Override
	public void setPortletDataHandlerInstances(
		List<PortletDataHandler> portletDataHandlerInstances) {
	}

	@Override
	public void setPortletInstance(Portlet portletInstance) {
	}

	@Override
	public void setPortletName(String portletName) {
	}

}