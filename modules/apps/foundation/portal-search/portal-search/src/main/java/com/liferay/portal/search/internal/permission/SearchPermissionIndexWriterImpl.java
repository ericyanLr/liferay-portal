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

import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerList;
import com.liferay.osgi.service.tracker.collections.list.ServiceTrackerListFactory;
import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.search.index.UpdateDocumentIndexWriter;
import com.liferay.portal.search.indexer.BaseModelDocumentFactory;
import com.liferay.portal.search.permission.SearchPermissionDocumentContributor;
import com.liferay.portal.search.permission.SearchPermissionIndexWriter;
import com.liferay.portal.search.spi.model.index.contributor.SearchPermissionModelDocumentContributor;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael C. Han
 */
@Component(immediate = true, service = SearchPermissionIndexWriter.class)
public class SearchPermissionIndexWriterImpl
	implements SearchPermissionIndexWriter {

	@Override
	public void updatePermissionFields(
		BaseModel<?> baseModel, long companyId, String searchEngineId,
		boolean commitImmediately) {

		Document document = baseModelDocumentFactory.createDocument(baseModel);

		ServiceTrackerList
			<SearchPermissionModelDocumentContributor,
				SearchPermissionModelDocumentContributor>
					searchPermissionModelDocumentContributors =
						getSearchPermissionModelDocumentContributors(baseModel);

		searchPermissionModelDocumentContributors.forEach(
			(SearchPermissionModelDocumentContributor
				permissionModelDocumentContributor) ->
				permissionModelDocumentContributor.contribute(
					document, baseModel));

		searchPermissionModelDocumentContributors.close();

		searchPermissionDocumentContributor.addPermissionFields(
			companyId, document);

		updateDocumentIndexWriter.updateDocumentPartially(
			searchEngineId, companyId, document, commitImmediately);
	}

	protected ServiceTrackerList
		<SearchPermissionModelDocumentContributor,
			SearchPermissionModelDocumentContributor>
				getSearchPermissionModelDocumentContributors(
					BaseModel<?> baseModel) {

		Bundle bundle = FrameworkUtil.getBundle(getClass());

		BundleContext bundleContext = bundle.getBundleContext();

		return ServiceTrackerListFactory.open(
			bundleContext, SearchPermissionModelDocumentContributor.class,
			StringBundler.concat(
				"(indexer.class.name=", baseModel.getModelClassName(), ")"));
	}

	@Reference
	protected BaseModelDocumentFactory baseModelDocumentFactory;

	@Reference
	protected SearchPermissionDocumentContributor
		searchPermissionDocumentContributor;

	@Reference
	protected UpdateDocumentIndexWriter updateDocumentIndexWriter;

}