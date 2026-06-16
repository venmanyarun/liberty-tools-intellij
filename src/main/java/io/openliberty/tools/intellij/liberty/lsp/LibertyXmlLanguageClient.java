/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.liberty.lsp;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures;
import io.openliberty.tools.intellij.lsp4mp.MicroProfileProjectService;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lemminx.customservice.XMLLanguageClientAPI;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client for LemMinX language server and Liberty LemMinX ext
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 */
public class LibertyXmlLanguageClient extends LanguageClientImpl implements XMLLanguageClientAPI, MicroProfileProjectService.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyXmlLanguageClient.class);

    private final LSPClientFeatures clientFeatures = new LSPClientFeatures() {
        @Override
        public void initializeParams(InitializeParams params) {
            super.initializeParams(params);

            ClientCapabilities capabilities = params.getCapabilities();
            if (capabilities == null) {
                capabilities = new ClientCapabilities();
                params.setCapabilities(capabilities);
            }

            // Set inline completion capability on textDocument using a Map-based approach
            // This is necessary because LSP4J might not have direct support for inlineCompletion
            var textDocument = capabilities.getTextDocument();
            if (textDocument == null) {
                textDocument = new org.eclipse.lsp4j.TextDocumentClientCapabilities();
                capabilities.setTextDocument(textDocument);
            }
            
            // Use Gson to dynamically add the inlineCompletion capability
            Gson gson = new Gson();
            JsonObject textDocJson = gson.toJsonTree(textDocument).getAsJsonObject();
            
            JsonObject inlineCompletionCapability = new JsonObject();
            inlineCompletionCapability.addProperty("dynamicRegistration", true);
            textDocJson.add("inlineCompletion", inlineCompletionCapability);
            
            // Convert back to TextDocumentClientCapabilities
            org.eclipse.lsp4j.TextDocumentClientCapabilities updatedTextDoc =
                gson.fromJson(textDocJson, org.eclipse.lsp4j.TextDocumentClientCapabilities.class);
            capabilities.setTextDocument(updatedTextDoc);
        }
    };

    public LibertyXmlLanguageClient(Project project) {
        super(project);
    }

    @Override
    public LSPClientFeatures getClientFeatures() {
        return clientFeatures;
    }

    @Override
    public void libraryUpdated(Library library) {
        // not needed for LemMinX LS
    }

    @Override
    public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
        // not needed for LemMinX LS
    }
}
