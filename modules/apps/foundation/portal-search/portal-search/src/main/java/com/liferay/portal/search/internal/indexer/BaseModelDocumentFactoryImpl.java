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

package com.liferay.portal.search.internal.indexer;

import com.liferay.portal.kernel.model.BaseModel;
import com.liferay.portal.kernel.model.ResourcedModel;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.DocumentHelper;
import com.liferay.portal.kernel.search.DocumentImpl;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.Indexer;
import com.liferay.portal.kernel.search.IndexerRegistry;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.kernel.util.Tuple;
import com.liferay.portal.search.indexer.BaseModelDocumentFactory;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Michael C. Han
 */
@Component(immediate = true, service = BaseModelDocumentFactory.class)
public class BaseModelDocumentFactoryImpl implements BaseModelDocumentFactory {

	@Override
	public Document createDocument(BaseModel<?> baseModel) {
		Document document = (Document)_document.clone();

		Indexer<BaseModel<?>> indexer = indexerRegistry.getIndexer(
			baseModel.getModelClassName());

		if (indexer != null) {
			try {
				Document indexerDocument = indexer.getDocument(baseModel);

				if (indexerDocument != null) {
					document.addKeyword(
						Field.ENTRY_CLASS_NAME,
						indexerDocument.get(Field.ENTRY_CLASS_NAME));
					document.addKeyword(
						Field.ENTRY_CLASS_PK,
						indexerDocument.get(Field.ENTRY_CLASS_PK));
					document.addKeyword(
						Field.GROUP_ID, indexerDocument.get(Field.GROUP_ID));
					document.addKeyword(
						Field.UID, indexerDocument.get(Field.UID));

					if (indexerDocument.hasField(Field.ROOT_ENTRY_CLASS_PK)) {
						document.addKeyword(
							Field.ROOT_ENTRY_CLASS_PK,
							indexerDocument.get(Field.ROOT_ENTRY_CLASS_PK));
					}

					return document;
				}
			}
			catch (SearchException se) {
			}
		}

		String className = baseModel.getModelClassName();

		Tuple classPKResourcePrimKeyTuple = getClassPKResourcePrimKey(
			baseModel);

		long classPK = (Long)classPKResourcePrimKeyTuple.getObject(0);

		String uid = getDocumentUID(className, classPK);

		document.addKeyword(Field.UID, uid);

		DocumentHelper documentHelper = new DocumentHelper(document);

		long resourcePrimKey = (Long)classPKResourcePrimKeyTuple.getObject(1);

		documentHelper.setEntryKey(className, classPK);

		if (resourcePrimKey > 0) {
			document.addKeyword(Field.ROOT_ENTRY_CLASS_PK, resourcePrimKey);
		}

		return document;
	}

	protected Tuple getClassPKResourcePrimKey(BaseModel<?> baseModel) {
		long classPK = 0;
		long resourcePrimKey = 0;

		if (baseModel instanceof ResourcedModel) {
			ResourcedModel resourcedModel = (ResourcedModel)baseModel;

			classPK = resourcedModel.getResourcePrimKey();
			resourcePrimKey = resourcedModel.getResourcePrimKey();
		}
		else {
			classPK = (Long)baseModel.getPrimaryKeyObj();
		}

		Tuple tuple = new Tuple(classPK, resourcePrimKey);

		return tuple;
	}

	protected String getDocumentUID(String className, long classPK) {
		String uid = Field.getUID(className, String.valueOf(classPK));

		return uid;
	}

	@Reference
	protected IndexerRegistry indexerRegistry;

	private final Document _document = new DocumentImpl();

}