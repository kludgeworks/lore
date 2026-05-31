package com.embabel.guide.rag

import com.embabel.agent.api.annotation.LlmTool
import com.embabel.agent.api.tool.ToolObject
import com.embabel.agent.mcpserver.McpToolExport
import com.embabel.agent.rag.tools.ToolishRag
import com.embabel.common.core.types.ZeroToOne
import com.embabel.common.util.StringTransformer
class SafeSearchTools(private val toolishRag: ToolishRag) {

    @LlmTool(name = "docs_vectorSearch", description = "Perform vector search safely. Specify topK and similarity threshold from 0-1")
    fun safeVectorSearch(
        query: String,
        @LlmTool.Param(description = "topK", required = false) topK: Int?,
        @LlmTool.Param(description = "similarity threshold from 0-1", required = false) threshold: Double?
    ): String {
        val actualTopK = topK ?: 10
        val actualThreshold = threshold ?: 0.7
        val tool = toolishRag.tools().find { it.definition.name.endsWith("vectorSearch") }
            ?: return "Error: vectorSearch tool not found in ToolishRag"
        
        val safeQuery = query.replace("\"", "\\\"").replace("\n", "\\n")
        val input = """{"query": "$safeQuery", "topK": $actualTopK, "threshold": $actualThreshold}"""
        val result = tool.call(input)
        return if (result is com.embabel.agent.api.tool.Tool.Result.Text) result.content else result.toString()
    }

    @LlmTool(name = "docs_textSearch", description = "Perform text search safely. Specify topK and similarity threshold from 0-1")
    fun safeTextSearch(
        query: String,
        @LlmTool.Param(description = "topK", required = false) topK: Int?,
        @LlmTool.Param(description = "similarity threshold from 0-1", required = false) threshold: Double?
    ): String {
        val actualTopK = topK ?: 10
        val actualThreshold = threshold ?: 0.7
        val tool = toolishRag.tools().find { it.definition.name.endsWith("textSearch") }
            ?: return "Error: textSearch tool not found in ToolishRag"
        
        val safeQuery = query.replace("\"", "\\\"").replace("\n", "\\n")
        val input = """{"query": "$safeQuery", "topK": $actualTopK, "threshold": $actualThreshold}"""
        val result = tool.call(input)
        return if (result is com.embabel.agent.api.tool.Tool.Result.Text) result.content else result.toString()
    }

    companion object {
        @JvmStatic
        fun exportSafeTools(toolishRag: ToolishRag, namingStrategy: StringTransformer): McpToolExport {
            // Filter out the unsafe native tools from the LlmReference's tool list
            val filteredOriginalTools = toolishRag.tools().filter { 
                !it.definition.name.endsWith("vectorSearch") && !it.definition.name.endsWith("textSearch") 
            }
            
            // Create a ToolObject for the filtered original tools
            val originalToolsObject = ToolObject(
                objects = filteredOriginalTools,
                namingStrategy = toolishRag.namingStrategy
            )
            
            // Create a ToolObject for our safe wrapper tools
            val wrapperObject = ToolObject(
                objects = listOf(SafeSearchTools(toolishRag)),
                namingStrategy = StringTransformer.IDENTITY 
            )
            
            return McpToolExport.fromToolObjects(
                listOf(originalToolsObject, wrapperObject), 
                namingStrategy
            )
        }
    }
}
