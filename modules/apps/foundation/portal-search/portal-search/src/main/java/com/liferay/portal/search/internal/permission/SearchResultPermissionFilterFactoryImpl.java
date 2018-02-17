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

package com.liferay.portal.search.internal.permission;

import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.search.IndexerRegistry;
import com.liferay.portal.kernel.search.RelatedEntryIndexerRegistry;
import com.liferay.portal.kernel.search.SearchResultPermissionFilter;
import com.liferay.portal.kernel.search.SearchResultPermissionFilterFactory;
import com.liferay.portal.kernel.search.facet.FacetPostProcessor;
import com.liferay.portal.kernel.security.permission.PermissionChecker;
import com.liferay.portal.kernel.util.Props;
import com.liferay.portal.search.configuration.SearchResultPermissionFilterFactoryConfiguration;

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Eric Yan
 */
@Component(
	configurationPid = "com.liferay.portal.search.configuration.SearchResultPermissionFilterFactoryConfiguration",
	immediate = true, service = SearchResultPermissionFilterFactory.class
)
public class SearchResultPermissionFilterFactoryImpl
	implements SearchResultPermissionFilterFactory {

	@Override
	public SearchResultPermissionFilter create(
		SearchExecutor searchExecutor, PermissionChecker permissionChecker) {

		return new DefaultSearchResultPermissionFilter(
			_facetPostProcessor, _indexerRegistry, permissionChecker, _props,
			_relatedEntryIndexerRegistry, searchExecutor,
			_searchResultPermissionFilterFactoryConfiguration);
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_searchResultPermissionFilterFactoryConfiguration =
			ConfigurableUtil.createConfigurable(
				SearchResultPermissionFilterFactoryConfiguration.class,
				properties);
	}

	@Reference
	private FacetPostProcessor _facetPostProcessor;

	@Reference
	private IndexerRegistry _indexerRegistry;

	@Reference
	private Props _props;

	@Reference
	private RelatedEntryIndexerRegistry _relatedEntryIndexerRegistry;

	private volatile SearchResultPermissionFilterFactoryConfiguration
		_searchResultPermissionFilterFactoryConfiguration;

}