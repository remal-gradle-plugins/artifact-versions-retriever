package name.remal.gradle_plugins.versions_retriever.git;

import static java.nio.file.Files.write;
import static java.util.stream.Collectors.toList;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.defaultValue;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isEmpty;
import static name.remal.gradle_plugins.toolkit.ObjectUtils.isNotEmpty;
import static name.remal.gradle_plugins.toolkit.PathUtils.normalizePath;
import static name.remal.gradle_plugins.toolkit.PredicateUtils.not;
import static name.remal.gradle_plugins.toolkit.git.GitUtils.findGitRepositoryRootFor;
import static org.eclipse.jgit.lib.Constants.DEFAULT_REMOTE_NAME;
import static org.eclipse.jgit.lib.Constants.GITDIR;
import static org.eclipse.jgit.lib.Constants.HEAD;
import static org.eclipse.jgit.lib.Constants.R_TAGS;
import static org.eclipse.jgit.lib.Repository.shortenRefName;
import static org.eclipse.jgit.revwalk.RevSort.TOPO_KEEP_BRANCH_TOGETHER;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.inject.Inject;
import lombok.CustomLog;
import lombok.SneakyThrows;
import lombok.val;
import name.remal.gradle_plugins.toolkit.Version;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.SubmoduleConfig.FetchRecurseSubmodulesMode;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TagOpt;
import org.gradle.api.GradleException;
import org.gradle.initialization.BuildCancellationToken;
import org.gradle.workers.WorkAction;

@CustomLog
@SuppressWarnings("java:S3776")
abstract class RetrievePreviousVersionFromGitTagAction
    implements WorkAction<RetrievePreviousVersionFromGitTagActionParams> {

    @Inject
    protected abstract BuildCancellationToken getBuildCancellationToken();

    @Override
    public void execute() {
        val params = getParameters();
        val projectDir = params.getProjectDirectory().getAsFile().get();
        val repositoryPath = findGitRepositoryRootFor(projectDir.toPath());
        if (repositoryPath == null) {
            throw new GradleException("Git repository root dir couldn't be found for " + projectDir);
        }
        val tagPatterns = params.getTagPatterns().get().stream()
            .map(Pattern::compile)
            .collect(toList());
        if (tagPatterns.isEmpty()) {
            throw new GradleException("Tag patterns can't be empty");
        }
        val resultPropertiesPath = normalizePath(params.getResultPropertiesFile().get().getAsFile().toPath());
        retrieveImpl(repositoryPath, tagPatterns, resultPropertiesPath);
    }

    @VisibleForTesting
    @SneakyThrows
    void retrieveImpl(Path repositoryPath, List<Pattern> tagPatterns, Path resultPropertiesPath) {
        logger.debug("Retrieving previous version for Git repository " + repositoryPath);
        try (
            val repository = FileRepositoryBuilder.create(repositoryPath.resolve(GITDIR).toFile());
            val git = new Git(repository)
        ) {
            retrieveImpl(git, tagPatterns, resultPropertiesPath);
        }
    }

    @SneakyThrows
    private void retrieveImpl(Git git, List<Pattern> tagPatterns, Path resultPropertiesPath) {
        logger.debug("Fetching tags");
        git.fetch()
            .setRefSpecs(R_TAGS + "*" + ':' + R_TAGS + "*")
            .setRecurseSubmodules(FetchRecurseSubmodulesMode.NO)
            .setRemote(getFetchRemote(git).getName())
            .setProgressMonitor(new GradleProgressMonitor(getBuildCancellationToken()))
            .call();

        val repository = git.getRepository();
        val objectIdVersions = collectObjectIdVersions(repository, tagPatterns);
        if (objectIdVersions.isEmpty()) {
            logger.warn("No version tags found for Git repository " + repository.getDirectory());
            write(resultPropertiesPath, new byte[0]);
            return;
        }

        Pair<Version, RevCommit> versionCommit = getVersionCommit(repository, objectIdVersions);
        if (versionCommit == null) {
            if (isNotEmpty(repository.getObjectDatabase().getShallowCommits())) {
                logger.debug("Repository contains shallow commits, fetching all commits");
                git.fetch()
                    .setTagOpt(TagOpt.NO_TAGS)
                    .setRecurseSubmodules(FetchRecurseSubmodulesMode.NO)
                    .setRemote(getFetchRemote(git).getName())
                    .setProgressMonitor(new GradleProgressMonitor(getBuildCancellationToken()))
                    .call();
                versionCommit = getVersionCommit(repository, objectIdVersions);
            }
        }
        if (versionCommit == null) {
            val headRef = repository.getRefDatabase().exactRef(HEAD);
            logger.warn("No reachable version found for Git commit in repository " + repository.getDirectory());
            write(resultPropertiesPath, new byte[0]);
            return;
        }
    }

    @Nullable
    @SneakyThrows
    private Pair<Version, RevCommit> getVersionCommit(
        Repository repository,
        Map<ObjectId, SortedSet<Version>> objectIdVersions
    ) {
        try (val revWalk = new RevWalk(repository)) {
            revWalk.sort(TOPO_KEEP_BRANCH_TOGETHER);

            val ref = repository.getRefDatabase().exactRef(HEAD);
            revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));

            for (val commit : revWalk) {
                val commitVersions = objectIdVersions.get(commit.getId());
                if (isNotEmpty(commitVersions)) {
                    return Pair.of(commitVersions.last(), commit);
                }
            }
        }

        return null;
    }

    @SneakyThrows
    private Map<ObjectId, SortedSet<Version>> collectObjectIdVersions(
        Repository repository,
        List<Pattern> tagPatterns
    ) {
        val objectIdVersions = new LinkedHashMap<ObjectId, SortedSet<Version>>();

        val tagRefs = repository.getRefDatabase().getRefsByPrefix(R_TAGS);
        for (val tagRef : tagRefs) {
            val tagName = shortenRefName(tagRef.getName());
            forTagPatterns:
            for (val tagPattern : tagPatterns) {
                val matcher = tagPattern.matcher(tagName);
                if (matcher.matches()) {
                    val versionString = matcher.group("version");
                    if (versionString == null) {
                        logger.warn(
                            "Capturing group `version` was not matched for pattern /{}/ and Git tag `{}`",
                            tagPattern,
                            tagName
                        );
                    } else if (versionString.isEmpty()) {
                        logger.warn(
                            "Capturing group `version` is empty for pattern /{}/ and Git tag `{}`",
                            tagPattern,
                            tagName
                        );
                    } else {
                        val objectId = getPeeledObjectId(repository, tagRef);
                        val version = Version.parse(versionString);
                        objectIdVersions.computeIfAbsent(objectId, __ -> new TreeSet<>()).add(version);
                        break forTagPatterns;
                    }
                }
            }
        }

        return objectIdVersions;
    }

    @SneakyThrows
    @SuppressWarnings("java:S2583")
    private static ObjectId getPeeledObjectId(Repository repository, Ref ref) {
        val peeledRef = defaultValue(repository.getRefDatabase().peel(ref), ref);
        return defaultValue(peeledRef.getPeeledObjectId(), peeledRef.getObjectId());
    }

    @SneakyThrows
    private static RemoteConfig getFetchRemote(Git git) {
        val remotes = git.remoteList().call().stream()
            .filter(not(it -> isEmpty(it.getURIs())))
            .collect(toList());
        RemoteConfig remote = remotes.stream()
            .filter(it -> DEFAULT_REMOTE_NAME.equals(it.getName()))
            .findFirst()
            .orElse(null);
        if (remote == null && isNotEmpty(remotes)) {
            remote = remotes.get(0);
        }
        if (remote == null) {
            throw new GradleException(
                "No remotes are configured for Git repository " + git.getRepository().getDirectory()
            );
        }
        return remote;
    }

}
