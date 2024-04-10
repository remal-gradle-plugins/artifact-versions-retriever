package name.remal.gradle_plugins.versions_retriever.maven;

import static lombok.AccessLevel.PUBLIC;

import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Internal;

@NoArgsConstructor(access = PUBLIC, onConstructor_ = {@Inject})
abstract class MavenRepositoryContainerImpl implements MavenRepositoryContainer {

    @Internal
    public abstract ListProperty<MavenRepository> getRepositories();

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @Override
    public void maven(Action<MavenRepository> action) {
        val repository = getObjectFactory().newInstance(MavenRepository.class);
        action.execute(repository);
        getRepositories().add(repository);
    }

}
