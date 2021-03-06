/**
 * Copyright (c) 2015-2016 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * The Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 * Bosch Software Innovations GmbH - Please refer to git log
 */
package org.eclipse.vorto.repository.web.api.v1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.vorto.core.api.model.mapping.MappingModel;
import org.eclipse.vorto.repository.api.AbstractModel;
import org.eclipse.vorto.repository.api.ModelId;
import org.eclipse.vorto.repository.api.ModelInfo;
import org.eclipse.vorto.repository.api.exception.ModelNotFoundException;
import org.eclipse.vorto.repository.core.impl.UserContext;
import org.eclipse.vorto.repository.web.AbstractRepositoryController;
import org.eclipse.vorto.repository.web.core.ModelDtoFactory;
import org.eclipse.vorto.repository.web.core.ModelRepositoryController;
import org.eclipse.vorto.utilities.reader.IModelWorkspace;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Alexander Edelmann - Robert Bosch (SEA) Pte. Ltd.
 */
@Api(value = "/models")
@RestController("modelRepositoryController")
@RequestMapping(value = "/api/v1/models")
public class ModelController extends AbstractRepositoryController {

	private static Logger logger = Logger.getLogger(ModelRepositoryController.class);
	
	@ApiOperation(value = "Returns a model by its full qualified model ID")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful retrieval of model info"), @ApiResponse(code = 400, message = "Wrong input"),
			@ApiResponse(code = 404, message = "Model not found"),
			@ApiResponse(code = 403, message = "Not Authorized to view model") })
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasPermission(T(org.eclipse.vorto.repository.api.ModelId).fromPrettyFormat(#modelId),'model:get')")
	@RequestMapping(value = "/{modelId:.+}", method = RequestMethod.GET)
	public ModelInfo getModelInfo(
			@ApiParam(value = "The modelId of vorto model, e.g. com.mycompany.Car:1.0.0", required = true) final @PathVariable String modelId) {
		Objects.requireNonNull(modelId, "modelId must not be null");

		logger.info("getModelInfo: [" + modelId + "]");
		ModelInfo resource = modelRepository.getById(ModelId.fromPrettyFormat(modelId));
		
		if (resource == null) {
			throw new ModelNotFoundException("Model does not exist", null);
		}
		return ModelDtoFactory.createDto(resource,
				UserContext.user(SecurityContextHolder.getContext().getAuthentication().getName()));
	}

	@ApiOperation(value = "Returns the model content")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful retrieval of model content"), @ApiResponse(code = 400, message = "Wrong input"),
			@ApiResponse(code = 404, message = "Model not found") })
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasPermission(T(org.eclipse.vorto.repository.api.ModelId).fromPrettyFormat(#modelId),'model:get')")
	@RequestMapping(value = "/{modelId:.+}/content", method = RequestMethod.GET)
	public AbstractModel getModelContent(
			@ApiParam(value = "The modelId of vorto model, e.g. com.mycompany.Car:1.0.0", required = true) final @PathVariable String modelId) {

		final ModelId modelID = ModelId.fromPrettyFormat(modelId);
		if (this.modelRepository.getById(modelID) == null) {
			throw new ModelNotFoundException("Model does not exist", null);
		}
		
		byte[] modelContent = createZipWithAllDependencies(modelID);

		IModelWorkspace workspace = IModelWorkspace.newReader()
				.addZip(new ZipInputStream(new ByteArrayInputStream(modelContent))).read();
		return ModelDtoFactory.createResource(
				workspace.get().stream().filter(p -> p.getName().equals(modelID.getName())).findFirst().get(), Optional.empty());
	}
	
	@ApiOperation(value = "Returns the model content including target platform specific attributes")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful retrieval of model content"), @ApiResponse(code = 400, message = "Wrong input"),
			@ApiResponse(code = 404, message = "Model not found") })
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasPermission(T(org.eclipse.vorto.repository.api.ModelId).fromPrettyFormat(#modelId),'model:get')")
	@RequestMapping(value = "/{modelId:.+}/content/{targetplatformKey}", method = RequestMethod.GET)
	public AbstractModel getModelContentForTargetPlatform(
			@ApiParam(value = "The modelId of vorto model, e.g. com.mycompany.Car:1.0.0", required = true) final @PathVariable String modelId,
			@ApiParam(value = "The key of the targetplatform, e.g. lwm2m", required = true) final @PathVariable String targetplatformKey) {

		final ModelId modelID = ModelId.fromPrettyFormat(modelId);
		List<ModelInfo> mappingResource = modelRepository
				.getMappingModelsForTargetPlatform(modelID, targetplatformKey);
		if (!mappingResource.isEmpty()) {
			byte[] mappingContentZip = createZipWithAllDependencies(mappingResource.get(0).getId());
			IModelWorkspace workspace = IModelWorkspace.newReader()
					.addZip(new ZipInputStream(new ByteArrayInputStream(mappingContentZip))).read();

			MappingModel mappingModel = (MappingModel) workspace.get().stream().filter(p -> p instanceof MappingModel)
					.findFirst().get();

			byte[] modelContent = createZipWithAllDependencies(modelID);

			workspace = IModelWorkspace.newReader().addZip(new ZipInputStream(new ByteArrayInputStream(modelContent)))
					.read();

			return ModelDtoFactory.createResource(
					workspace.get().stream().filter(p -> p.getName().equals(modelID.getName())).findFirst().get(),
					Optional.of(mappingModel));
		} else {
			return getModelContent(modelId);
		}
	}

	private byte[] createZipWithAllDependencies(ModelId modelId) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zos = new ZipOutputStream(baos);

		try {
			addModelToZip(zos, modelId);

			zos.close();
			baos.close();

			return baos.toByteArray();

		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	@ApiOperation(value = "Returns the model content including target platform specific attributes for the given model- and mapping modelID")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful retrieval of model content"), @ApiResponse(code = 400, message = "Wrong input"),
			@ApiResponse(code = 404, message = "Model not found") })
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasPermission(T(org.eclipse.vorto.repository.api.ModelId).fromPrettyFormat(#modelId),'model:get')")
	@RequestMapping(value = "/{modelId:.+}/content/mappings/{mappingId:.+}", method = RequestMethod.GET)
	public AbstractModel getModelContentByModelAndMappingId(
			@ApiParam(value = "The model ID (prettyFormat)", required = true) final @PathVariable String modelId,
			@ApiParam(value = "The mapping Model ID (prettyFormat)", required = true) final @PathVariable String mappingId) {

		ModelInfo vortoModelInfo = modelRepository.getById(ModelId.fromPrettyFormat(modelId));
		ModelInfo mappingModelInfo = modelRepository.getById(ModelId.fromPrettyFormat(mappingId));

		if (vortoModelInfo == null) {
			throw new ModelNotFoundException("Could not find vorto model with ID: " + modelId);
		} else if (mappingModelInfo == null) {
			throw new ModelNotFoundException("Could not find mapping with ID: " + mappingId);

		}

		byte[] mappingContentZip = createZipWithAllDependencies(mappingModelInfo.getId());
		IModelWorkspace workspace = IModelWorkspace.newReader()
				.addZip(new ZipInputStream(new ByteArrayInputStream(mappingContentZip))).read();
		MappingModel mappingModel = (MappingModel) workspace.get().stream().filter(p -> p instanceof MappingModel)
				.findFirst().get();

		byte[] modelContent = createZipWithAllDependencies(vortoModelInfo.getId());
		workspace = IModelWorkspace.newReader().addZip(new ZipInputStream(new ByteArrayInputStream(modelContent)))
				.read();

		return ModelDtoFactory.createResource(workspace.get().stream()
				.filter(p -> p.getName().equals(vortoModelInfo.getId().getName())).findFirst().get(),
				Optional.of(mappingModel));

	}
	
	@ApiOperation(value = "Downloads the model file")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful download of model file"), @ApiResponse(code = 400, message = "Wrong input"),
			@ApiResponse(code = 404, message = "Model not found") })
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN') or hasPermission(T(org.eclipse.vorto.repository.api.ModelId).fromPrettyFormat(#modelId),'model:get')")
	@RequestMapping(value = "/{modelId:.+}/file", method = RequestMethod.GET)
	public void downloadModelById(
			@ApiParam(value = "The modelId of vorto model, e.g. com.mycompany.Car:1.0.0", required = true) final @PathVariable String modelId,
			@ApiParam(value = "Set true if dependencies shall be included", required = false) final @RequestParam(value = "includeDependencies", required = false) boolean includeDependencies,
			final HttpServletResponse response) {

		Objects.requireNonNull(modelId, "modelId must not be null");

		final ModelId modelID = ModelId.fromPrettyFormat(modelId);

		logger.info("Download of Model file : [" + modelID.toString() + "]");

		if (includeDependencies) {
			byte[] zipContent = createZipWithAllDependencies(modelID);
			response.setHeader(CONTENT_DISPOSITION, ATTACHMENT_FILENAME + modelID.getNamespace() + "_"
					+ modelID.getName() + "_" + modelID.getVersion() + ".zip");
			response.setContentType(APPLICATION_OCTET_STREAM);
			try {
				IOUtils.copy(new ByteArrayInputStream(zipContent), response.getOutputStream());
				response.flushBuffer();
			} catch (IOException e) {
				throw new RuntimeException("Error copying file.", e);
			}
		} else {
			createSingleModelContent(modelID, response);
		}
	}
}
