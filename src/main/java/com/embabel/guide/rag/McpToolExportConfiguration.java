package com.embabel.guide.rag;

import com.embabel.agent.filter.PropertyFilter;
import com.embabel.agent.mcpserver.McpToolExport;
import com.embabel.agent.rag.service.SearchOperations;
import com.embabel.agent.rag.tools.ToolishRag;
import com.embabel.guide.GuideComposition;
import com.embabel.guide.GuideProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Export MCP tools
 */
@Configuration
class McpToolExportConfiguration {

    @Bean
    McpToolExport documentationRagTools(
            SearchOperations searchOperations,
            GuideProperties properties,
            GuideComposition guideComposition
    ) {
        var toolishRag = new ToolishRag(
                "docs",
                guideComposition.effectiveDomain().getName() + " docs",
                searchOperations
        );
        var activeVersion = properties.getContent().getActiveVersion();
        if (activeVersion != null) {
            var versionFilter = new PropertyFilter.In(
                    "version",
                    List.of(activeVersion, VersionChunkTransformer.SUPPLEMENTARY)
            );
            toolishRag = toolishRag.withMetadataFilter(versionFilter);
        }
        return SafeSearchTools.exportSafeTools(
                toolishRag,
                properties.toolNamingStrategy()
        );
    }

    @Bean
    McpToolExport referenceTools(
            DataManager dataManager,
            GuideProperties properties) {
        return McpToolExport.fromLlmReferences(
                dataManager.referencesForAllUsers(),
                properties.toolNamingStrategy()
        );
    }
}
