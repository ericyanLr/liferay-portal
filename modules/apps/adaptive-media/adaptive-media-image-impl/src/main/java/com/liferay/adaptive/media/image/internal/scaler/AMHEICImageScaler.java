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

package com.liferay.adaptive.media.image.internal.scaler;

import com.liferay.adaptive.media.exception.AMRuntimeException;
import com.liferay.adaptive.media.image.configuration.AMImageConfigurationEntry;
import com.liferay.adaptive.media.image.internal.configuration.AMImageConfiguration;
import com.liferay.adaptive.media.image.internal.util.RenderedImageUtil;
import com.liferay.adaptive.media.image.scaler.AMImageScaledImage;
import com.liferay.adaptive.media.image.scaler.AMImageScaler;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.configuration.metatype.bnd.util.ConfigurableUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.image.ImageBag;
import com.liferay.portal.kernel.image.ImageMagick;
import com.liferay.portal.kernel.image.ImageTool;
import com.liferay.portal.kernel.repository.model.FileVersion;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.GetterUtil;

import java.awt.image.RenderedImage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Sergio González
 */
@Component(
	configurationPid = "com.liferay.adaptive.media.image.internal.configuration.AMImageConfiguration",
	immediate = true, property = "mime.type=image/heic",
	service = AMImageScaler.class
)
public class AMHEICImageScaler implements AMImageScaler {

	@Override
	public boolean isEnabled() {
		if (_amImageConfiguration.imageMagickEnabled() &&
			_imageMagick.isEnabled()) {

			return true;
		}

		return false;
	}

	@Override
	public AMImageScaledImage scaleImage(
		FileVersion fileVersion,
		AMImageConfigurationEntry amImageConfigurationEntry) {

		File inputFile = null;
		File outputFile = null;

		try {
			inputFile = _getFile(fileVersion);
			outputFile = FileUtil.createTempFile(ImageTool.TYPE_PNG);

			_scaleAndConvertToPNG(
				inputFile, outputFile, amImageConfigurationEntry);

			ImageBag imageBag = _imageTool.read(outputFile);

			RenderedImage scaledRenderedImage = imageBag.getRenderedImage();

			String targetMimeType = ContentTypes.IMAGE_PNG;

			return new AMImageScaledImageImpl(
				RenderedImageUtil.getRenderedImageContentStream(
					scaledRenderedImage, targetMimeType),
				scaledRenderedImage.getHeight(), scaledRenderedImage.getWidth(),
				targetMimeType);
		}
		catch (Exception exception) {
			throw new AMRuntimeException.IOException(
				StringBundler.concat(
					"Unable to scale file entry ", fileVersion.getFileEntryId(),
					" to match adaptive media configuration ",
					amImageConfigurationEntry.getUUID()),
				exception);
		}
		finally {
			if (inputFile != null) {
				inputFile.delete();
			}

			if (outputFile != null) {
				outputFile.delete();
			}
		}
	}

	@Activate
	@Modified
	protected void activate(Map<String, Object> properties) {
		_amImageConfiguration = ConfigurableUtil.createConfigurable(
			AMImageConfiguration.class, properties);
	}

	private File _getFile(FileVersion fileVersion)
		throws IOException, PortalException {

		try (InputStream inputStream = fileVersion.getContentStream(false)) {
			return FileUtil.createTempFile(inputStream);
		}
	}

	private String _getResizeArg(
		AMImageConfigurationEntry amImageConfigurationEntry) {

		Map<String, String> properties =
			amImageConfigurationEntry.getProperties();

		int maxHeight = GetterUtil.getInteger(properties.get("max-height"));
		int maxWidth = GetterUtil.getInteger(properties.get("max-width"));

		if ((maxHeight > 0) && (maxWidth > 0)) {
			return StringBundler.concat(maxWidth, "x", maxHeight, ">");
		}

		return null;
	}

	private void _scaleAndConvertToPNG(
			File inputFile, File outputFile,
			AMImageConfigurationEntry amImageConfigurationEntry)
		throws Exception {

		List<String> arguments = new ArrayList<>();

		arguments.add(inputFile.getAbsolutePath());

		String resizeArg = _getResizeArg(amImageConfigurationEntry);

		if (resizeArg != null) {
			arguments.add("-resize");
			arguments.add(resizeArg);
		}

		arguments.add(outputFile.getAbsolutePath());

		Future<?> future = _imageMagick.convert(arguments);

		future.get();
	}

	private volatile AMImageConfiguration _amImageConfiguration;

	@Reference
	private ImageMagick _imageMagick;

	@Reference
	private ImageTool _imageTool;

}