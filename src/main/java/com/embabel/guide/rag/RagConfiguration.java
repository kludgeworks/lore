/*
 * Copyright 2024-2025 Embabel Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.embabel.guide.rag;

import com.embabel.agent.rag.ingestion.ChunkTransformer;
import com.embabel.agent.rag.ingestion.ContentChunker;
import com.embabel.agent.rag.ingestion.ContentFetcher;
import com.embabel.agent.rag.ingestion.FetchRoute;
import com.embabel.agent.rag.ingestion.HierarchicalContentReader;
import com.embabel.agent.rag.ingestion.HttpContentFetcher;
import com.embabel.agent.rag.ingestion.RoutingContentFetcher;
import com.embabel.agent.rag.ingestion.RssContentFetcher;
import com.embabel.agent.rag.ingestion.TikaHierarchicalContentReader;
import kotlin.Pair;

import java.util.ArrayList;
import com.embabel.agent.rag.graph.DrivineCypherSearch;
import com.embabel.agent.rag.graph.DrivineStore;
import com.embabel.agent.rag.graph.GraphRagServiceProperties;
import com.embabel.agent.rag.graph.dialect.RagDialect;
import com.embabel.common.ai.model.EmbeddingService;
import com.embabel.guide.GuideProperties;
import org.drivine.connection.DataSourceMap;
import org.drivine.manager.PersistenceManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import java.util.Properties;

/**
 * Configuration for RAG (Retrieval Augmented Generation) components.
 * Creates the DrivineStore and related beans for Neo4j-based RAG operations.
 *
 * The EmbeddingService is provided by the ONNX embeddings auto-configuration
 * and registered via registerSingleton. @DependsOn ensures the ONNX initializer
 * runs first so the EmbeddingService bean exists when this configuration is wired.
 */
@Configuration
@EnableConfigurationProperties(GraphRagServiceProperties.class)
@DependsOn("onnxEmbeddingInitializer")
class RagConfiguration {

    @Bean
    ChunkTransformer chunkTransformer(GuideProperties guideProperties) {
        return new VersionChunkTransformer(guideProperties);
    }

    @Bean
    HierarchicalContentReader hierarchicalContentReader(GuideProperties guideProperties) {
        // Medium's RSS feed only exposes ~10 most recent articles per author.
        // Try RSS first; on failure fall back to direct HTTP in case Medium
        // is currently lax (its bot-blocking tightens and relaxes over time).
        var http = new HttpContentFetcher();
        var mediumRss = new RssContentFetcher(
                RssContentFetcher.Companion.templateResolver("https://medium.com/feed/{0}"),
                http);
        var mediumFetcher = new FallbackContentFetcher(mediumRss, http);

        var routes = new ArrayList<Pair<String, ContentFetcher>>();
        routes.add(new Pair<>("https://medium.com/**", mediumFetcher));
        for (FetchRoute route : guideProperties.getFetchRoutes()) {
            routes.add(new Pair<>(route.getPattern(), route.buildFetcher()));
        }
        return new TikaHierarchicalContentReader(new RoutingContentFetcher(http, routes));
    }

    @Bean
    @Primary
    DrivineStore drivineStore(
            @Qualifier("neo") PersistenceManager persistenceManager,
            PlatformTransactionManager platformTransactionManager,
            EmbeddingService embeddingService,
            ChunkTransformer chunkTransformer,
            GraphRagServiceProperties graphRagProperties,
            GuideProperties guideProperties,
            DataSourceMap dataSourceMap) {
        var chunkerConfig = guideProperties.getChunkerConfig() != null
                ? guideProperties.getChunkerConfig()
                : new ContentChunker.Config();
        var databaseType = dataSourceMap.getDataSources().get("neo").getType();
        var dialect = RagDialect.Companion.forDatabaseType(databaseType);
        return new DrivineStore(
                persistenceManager,
                graphRagProperties,
                chunkerConfig,
                chunkTransformer,
                embeddingService,
                platformTransactionManager,
                new DrivineCypherSearch(persistenceManager),
                dialect
        );
    }

    @Bean
    public BeanNameAutoProxyCreator transactionalProxyCreator() {
        var creator = new BeanNameAutoProxyCreator();
        creator.setBeanNames("drivineStore");
        creator.setInterceptorNames("drivineTransactionInterceptor");
        creator.setProxyTargetClass(true);
        return creator;
    }

    @Bean
    public TransactionInterceptor drivineTransactionInterceptor(PlatformTransactionManager transactionManager) {
        var attributes = new Properties();
        attributes.setProperty("*", "PROPAGATION_REQUIRED");
        return new TransactionInterceptor(transactionManager, attributes);
    }
}