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

package com.liferay.portal.tools.service.builder.test.model;

import com.liferay.portal.kernel.model.ModelWrapper;
import com.liferay.portal.kernel.model.wrapper.BaseModelWrapper;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class is a wrapper for {@link NullConvertibleEntry}.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see NullConvertibleEntry
 * @generated
 */
public class NullConvertibleEntryWrapper
	extends BaseModelWrapper<NullConvertibleEntry>
	implements ModelWrapper<NullConvertibleEntry>, NullConvertibleEntry {

	public NullConvertibleEntryWrapper(
		NullConvertibleEntry nullConvertibleEntry) {

		super(nullConvertibleEntry);
	}

	@Override
	public Map<String, Object> getModelAttributes() {
		Map<String, Object> attributes = new HashMap<String, Object>();

		attributes.put("nullConvertibleEntryId", getNullConvertibleEntryId());
		attributes.put("convertedValue", getConvertedValue());
		attributes.put("nonconvertedValue", getNonconvertedValue());

		return attributes;
	}

	@Override
	public void setModelAttributes(Map<String, Object> attributes) {
		Long nullConvertibleEntryId = (Long)attributes.get(
			"nullConvertibleEntryId");

		if (nullConvertibleEntryId != null) {
			setNullConvertibleEntryId(nullConvertibleEntryId);
		}

		String convertedValue = (String)attributes.get("convertedValue");

		if (convertedValue != null) {
			setConvertedValue(convertedValue);
		}

		String nonconvertedValue = (String)attributes.get("nonconvertedValue");

		if (nonconvertedValue != null) {
			setNonconvertedValue(nonconvertedValue);
		}
	}

	@Override
	public NullConvertibleEntry cloneWithOriginalValues() {
		return wrap(model.cloneWithOriginalValues());
	}

	/**
	 * Returns the converted value of this null convertible entry.
	 *
	 * @return the converted value of this null convertible entry
	 */
	@Override
	public String getConvertedValue() {
		return model.getConvertedValue();
	}

	/**
	 * Returns the nonconverted value of this null convertible entry.
	 *
	 * @return the nonconverted value of this null convertible entry
	 */
	@Override
	public String getNonconvertedValue() {
		return model.getNonconvertedValue();
	}

	/**
	 * Returns the null convertible entry ID of this null convertible entry.
	 *
	 * @return the null convertible entry ID of this null convertible entry
	 */
	@Override
	public long getNullConvertibleEntryId() {
		return model.getNullConvertibleEntryId();
	}

	/**
	 * Returns the primary key of this null convertible entry.
	 *
	 * @return the primary key of this null convertible entry
	 */
	@Override
	public long getPrimaryKey() {
		return model.getPrimaryKey();
	}

	@Override
	public void persist() {
		model.persist();
	}

	/**
	 * Sets the converted value of this null convertible entry.
	 *
	 * @param convertedValue the converted value of this null convertible entry
	 */
	@Override
	public void setConvertedValue(String convertedValue) {
		model.setConvertedValue(convertedValue);
	}

	/**
	 * Sets the nonconverted value of this null convertible entry.
	 *
	 * @param nonconvertedValue the nonconverted value of this null convertible entry
	 */
	@Override
	public void setNonconvertedValue(String nonconvertedValue) {
		model.setNonconvertedValue(nonconvertedValue);
	}

	/**
	 * Sets the null convertible entry ID of this null convertible entry.
	 *
	 * @param nullConvertibleEntryId the null convertible entry ID of this null convertible entry
	 */
	@Override
	public void setNullConvertibleEntryId(long nullConvertibleEntryId) {
		model.setNullConvertibleEntryId(nullConvertibleEntryId);
	}

	/**
	 * Sets the primary key of this null convertible entry.
	 *
	 * @param primaryKey the primary key of this null convertible entry
	 */
	@Override
	public void setPrimaryKey(long primaryKey) {
		model.setPrimaryKey(primaryKey);
	}

	@Override
	public String toXmlString() {
		return model.toXmlString();
	}

	@Override
	protected NullConvertibleEntryWrapper wrap(
		NullConvertibleEntry nullConvertibleEntry) {

		return new NullConvertibleEntryWrapper(nullConvertibleEntry);
	}

}