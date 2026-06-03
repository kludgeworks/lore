package com.embabel.guide.rag;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import com.embabel.guide.RepositoryConfig;

/**
 * Service to manage Git repositories for RAG ingestion.
 * Performs shallow clones (--depth 1) of requested repositories into a local workspace.
 */
@Service
public class RepositoryIngestionService {

    private static final Logger logger = LoggerFactory.getLogger(RepositoryIngestionService.class);
    private static final String WORKSPACE_DIR = ".workspace/repos";

    /**
     * Clones the given repositories into the local workspace.
     * 
     * @param repositories List of RepositoryConfig to clone
     */
    public void cloneRepositories(List<RepositoryConfig> repositories) {
        File workspace = new File(WORKSPACE_DIR);
        if (!workspace.exists()) {
            workspace.mkdirs();
        }

        for (RepositoryConfig repo : repositories) {
            String url = repo.getUrl();
            String repoName = url.substring(url.lastIndexOf('/') + 1).replace(".git", "");
            File targetDir = new File(workspace, repoName);

            if (targetDir.exists() && targetDir.isDirectory()) {
                logger.info("Repository {} already exists locally. Pulling latest changes...", repoName);
                try (Git git = Git.open(targetDir)) {
                    if (repo.getTag() != null) {
                        logger.info("Checking out explicit tag {} for {}", repo.getTag(), repoName);
                        git.checkout().setName("refs/tags/" + repo.getTag()).call();
                    } else {
                        git.pull().call();
                    }
                    logger.info("Successfully updated {}", repoName);
                } catch (Exception e) {
                    logger.error("Failed to update repository {}: {}", repoName, e.getMessage(), e);
                }
            } else {
                logger.info("Cloning repository {} into {} (shallow clone)...", url, targetDir.getAbsolutePath());
                try {
                    var command = Git.cloneRepository()
                            .setURI(url)
                            .setDirectory(targetDir)
                            .setDepth(1) // Shallow clone
                            .setCloneAllBranches(false);
                            
                    if (repo.getTag() != null) {
                        command.setBranch("refs/tags/" + repo.getTag());
                    }
                    
                    command.call().close();
                    logger.info("Successfully cloned {}", repoName);
                } catch (GitAPIException e) {
                    logger.error("Failed to clone repository {}: {}", url, e.getMessage(), e);
                }
            }
        }
    }
}
