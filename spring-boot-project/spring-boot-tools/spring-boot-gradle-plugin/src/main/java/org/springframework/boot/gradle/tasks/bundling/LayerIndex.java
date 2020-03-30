/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle.tasks.bundling;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.gradle.api.file.FileCopyDetails;

import org.springframework.boot.loader.tools.Layer;
import org.springframework.boot.loader.tools.Layers;
import org.springframework.boot.loader.tools.Library;
import org.springframework.boot.loader.tools.LibraryCoordinates;
import org.springframework.boot.loader.tools.layer.CustomLayers;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Index describing the layer to which each entry in a jar belongs.
 *
 * @author Andy Wilkinson
 */
final class LayerIndex {

	private final MultiValueMap<Layer, String> entries = new LinkedMultiValueMap<>();

	private final Layers layers;

	private final Function<FileCopyDetails, String> coordinatesResolver;

	LayerIndex(LayerConfiguration layerConfiguration, Function<FileCopyDetails, String> coordinatesResolver) {
		this.layers = determineLayers(layerConfiguration);
		this.coordinatesResolver = coordinatesResolver;
	}

	void addEntry(FileCopyDetails entryDetails) {
		if (this.layers != null) {
			this.entries.add(layerForFileDetails(entryDetails), entryDetails.getPath());
		}
	}

	void addEntry(String path) {
		if (this.layers != null) {
			this.entries.add(this.layers.getLayer(path), path);
		}
	}

	void write(Writer writer) throws IOException {
		if (this.entries.isEmpty()) {
			return;
		}
		for (Layer layer : this.layers) {
			List<String> layerEntries = this.entries.get(layer);
			if (layerEntries != null) {
				for (String entry : layerEntries) {
					writer.append(layer + " " + entry + "\n");
				}
			}
		}
		writer.flush();
	}

	private Layer layerForFileDetails(FileCopyDetails details) {
		String path = details.getPath();
		if (path.startsWith("BOOT-INF/lib/")) {
			String coordinates = this.coordinatesResolver.apply(details);
			LibraryCoordinates libraryCoordinates = (coordinates != null) ? new LibraryCoordinates(coordinates)
					: new LibraryCoordinates("?:?:?");
			return this.layers.getLayer(new Library(null, details.getFile(), null, libraryCoordinates, false));
		}
		return this.layers.getLayer(details.getSourcePath());
	}

	private static Layers determineLayers(LayerConfiguration layerConfiguration) {
		if (layerConfiguration == null) {
			return null;
		}
		if (layerConfiguration.getLayersOrder() == null || layerConfiguration.getLayersOrder().isEmpty()) {
			return Layers.IMPLICIT;
		}
		List<Layer> customLayers = layerConfiguration.getLayersOrder().stream().map(Layer::new)
				.collect(Collectors.toList());
		return new CustomLayers(customLayers, layerConfiguration.getApplication(), layerConfiguration.getLibraries());
	}

}
