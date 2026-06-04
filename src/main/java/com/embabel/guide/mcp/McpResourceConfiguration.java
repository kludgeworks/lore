package com.embabel.guide.mcp;

import com.embabel.agent.mcpserver.sync.McpResourcePublisher;
import com.embabel.agent.mcpserver.sync.SyncResourceSpecificationFactory;
import com.embabel.guide.DomainConfig;
import com.embabel.guide.GuideComposition;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class McpResourceConfiguration {

    private final GuideComposition guideComposition;

    public McpResourceConfiguration(GuideComposition guideComposition) {
        this.guideComposition = guideComposition;
    }

    /**
     * The MCP "about" text, built from the active profile's effective domain (including anything
     * inherited via guide.extends) so it describes the current knowledge base rather than hard-coding
     * a single framework.
     */
    private String aboutContent() {
        DomainConfig domain = guideComposition.effectiveDomain();
        String name = domain.getName();

        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" Guide MCP server (streamable HTTP)\n\n");
        sb.append("Purpose:\n");
        sb.append("- Exposes documentation and references for ").append(name)
                .append(" (").append(domain.getDescription()).append(") to MCP clients.\n");
        sb.append("- Primary use is answering ").append(name)
                .append(" questions using MCP tools (docs_* and API lookup tools).\n\n");
        sb.append("Endpoints:\n");
        sb.append("- Streamable HTTP: http://localhost:1337/mcp\n");
        sb.append("- Tools list: http://localhost:1337/mcp/tools/list\n");
        sb.append("- Resources list: http://localhost:1337/mcp/resources/list\n\n");

        List<String> references = domain.getReferences();
        if (!references.isEmpty()) {
            sb.append("Key references:\n");
            for (String reference : references) {
                sb.append("- ").append(reference).append("\n");
            }
            sb.append("\n");
        }

        String toolGuidance = domain.getToolGuidance();
        if (toolGuidance != null && !toolGuidance.isBlank()) {
            sb.append(toolGuidance).append("\n\n");
        }

        sb.append("Recommended usage for agents:\n");
        sb.append("1) Prefer docs_* tools to answer ").append(name).append(" questions.\n");
        sb.append("2) Use the API/reference tools to confirm class/package signatures.\n");
        sb.append("3) If unsure, query the tools list to see the exact available tools.\n\n");
        sb.append("Notes:\n");
        sb.append("- If running on a different port, update the /mcp URL accordingly.\n");
        sb.append("- This server currently exposes MCP tools and resources (no MCP prompts).\n");
        return sb.toString();
    }

    @Bean
    public McpResourcePublisher guideResources() {
        return new McpResourcePublisher() {
            @Override
            public List<SyncResourceSpecification> resources() {
                return List.of(
                        SyncResourceSpecificationFactory.staticSyncResourceSpecification(
                                "guide://about",
                                "about",
                                "About this MCP server and how to use it",
                                aboutContent(),
                                "text/plain"
                        )
                );
            }

            @Override
            public String infoString(@Nullable Boolean verbose, int indent) {
                return guideComposition.effectiveDomain().getName() + " Guide MCP resources";
            }
        };
    }
}
