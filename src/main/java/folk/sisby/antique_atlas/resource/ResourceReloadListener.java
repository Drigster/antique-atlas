package folk.sisby.antique_atlas.resource;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface ResourceReloadListener<T> extends ResourceReloader, IdentifiableResourceReloadListener {

    CompletableFuture<T> load(ResourceManager manager, Profiler profiler, Executor executor);

    CompletableFuture<Void> apply(T data,
                                  ResourceManager manager,
                                  Profiler profiler,
                                  Executor executor);


    default CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer,
                                           ResourceManager manager,
                                           Profiler prepareProfiler,
                                           Profiler applyProfiler,
                                           Executor prepareExecutor,
                                           Executor applyExecutor) {
        CompletableFuture<T> load = load(manager, prepareProfiler, prepareExecutor);

        return load.thenCompose(synchronizer::whenPrepared)
            .thenCompose(t -> apply(t, manager, applyProfiler, applyExecutor));
    }

    default String getName() {
        return getId().toString();
    }

    Identifier getId();

    Collection<Identifier> getDependencies();

    @Override
    default Identifier getFabricId() {
        return getId();
    }

    @Override
    default Collection<Identifier> getFabricDependencies() {
        return getDependencies();
    }
}