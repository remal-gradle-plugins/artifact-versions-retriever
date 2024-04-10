package name.remal.gradle_plugins.versions_retriever.maven;

import static name.remal.gradle_plugins.toolkit.PropertyUtils.getFinalized;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import lombok.val;
import name.remal.gradle_plugins.versions_retriever.AbstractRetrieveVersions;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;

public abstract class AbstractRetrieveFromMaven extends AbstractRetrieveVersions {

    {
        getOutputs().cacheIf("This task should always fetch from remote Maven repository", __ -> false);
    }

    @Input
    public abstract Property<Boolean> getWithReleases();

    {
        getWithReleases().convention(true);
    }

    @Input
    public abstract Property<Boolean> getWithSnapshots();

    {
        getWithSnapshots().convention(false);
    }

    private final MavenRepositoryContainerImpl repositories =
        getObjectFactory().newInstance(MavenRepositoryContainerImpl.class);

    @Internal
    public MavenRepositoryContainer getRepositories() {
        return repositories;
    }

    public void repositories(Action<MavenRepositoryContainer> action) {
        action.execute(repositories);
    }

    @Input
    @Nested
    protected abstract MapProperty<String, MavenRepository> getRepositoryMap();

    {
        getRepositoryMap().set(getProviderFactory().provider(() -> {
            Map<String, MavenRepository> map = new LinkedHashMap<>();
            for (val repo : repositories.getRepositories().get()) {
                val key = getFinalized(repo.getUri());
                map.put(key, repo);
            }
            return map;
        }));
    }

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @Inject
    protected abstract ProviderFactory getProviderFactory();

}
