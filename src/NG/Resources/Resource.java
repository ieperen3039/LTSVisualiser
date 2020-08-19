package NG.Resources;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * contains an object that is not included when serialized. Instead, it is regenerated when necessary, and automatically
 * dropped when not used for some amount of time.
 * @author Geert van Ieperen created on 25-2-2020.
 */
public abstract class Resource<T> implements Serializable {
    private static final List<WeakReference<Resource<?>>> allResources = Collections.synchronizedList(new ArrayList<>());

    /** number of calls to {@link #cool()} before this is dropped */
    private static final int MAX_HEAT = 300; // sec * FPS

    /** the cached element */
    protected transient T element = null;
    /** indicator how many ticks the last {@link #get()} call was */
    private transient int heat = 0;

    public Resource() {
        allResources.add(new WeakReference<>(this));
    }

    /**
     * returns the cached element, possibly generating a new element
     * @return the element itself
     * @throws ResourceException if the reloading operation fails
     */
    public T get() throws ResourceException {
        if (element == null) {
            element = reload();
        }
        heat = MAX_HEAT;

        return element;
    }

    /**
     * drops the cached element, causing a reload on the next get
     */
    public void drop() {
        element = null;
    }

    /**
     * reloads the resource.
     * @throws ResourceException whenever the resource could not be generated
     */
    protected abstract T reload() throws ResourceException;

    /**
     * marks some time period. If this resource has not been used after at most {@link #MAX_HEAT} calls to this method,
     * it is dropped as if calling {@link #drop()}.
     */
    private void cool() {
        if (element != null) {
            heat--;
            if (heat == 0) drop();
        }
    }

    @Override
    public String toString() {
        if (element == null) {
            return "[empty resource]";
        } else {
            return "[" + element + "]";
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        allResources.add(new WeakReference<>(this));
    }

    public static long getNrOfActiveResources() {
        synchronized (allResources) {
            return allResources.stream()
                    .map(Reference::get)
                    .filter(Objects::nonNull)
                    .filter(r -> r.element != null)
                    .count();
        }
    }

    public static void cycle() {
        synchronized (allResources) {
            allResources.removeIf(r -> r.get() == null);
            for (WeakReference<Resource<?>> resourceRef : allResources) {
                Resource<?> resource = resourceRef.get();
                if (resource != null) {
                    resource.cool();
                }
            }
        }
    }

    public static void dropAll() {
        synchronized (allResources) {
            for (WeakReference<Resource<?>> resourceRef : allResources) {
                Resource<?> resource = resourceRef.get();
                if (resource != null) {
                    resource.drop();
                }
            }
        }
    }

    /**
     * create a resource that is generated from another resource.
     * @param source    a resource generating an element of type A
     * @param extractor a function that generates the desired element of type B using source
     * @return a resource generating an element of type B
     */
    public static <A, B> Resource<B> derive(Resource<A> source, ResourceConverter<A, B> extractor) {
        return new GeneratorResource<>(() -> extractor.apply(source.get()), null);
    }

    public static <A, B> Resource<B> derive(
            Resource<A> source, ResourceConverter<A, B> extractor, ResourceCleaner<B> cleanup
    ) {
        return new GeneratorResource<>(() -> extractor.apply(source.get()), cleanup);
    }

    /** serializable version of {@link Supplier} */
    public interface ResourceGenerator<T> extends Supplier<T>, Serializable {}

    /** serializable version of {@link Consumer} */
    public interface ResourceCleaner<T> extends Consumer<T>, Serializable {}

    /** serializable version of {@link Function} */
    public interface ResourceConverter<A, B> extends Function<A, B>, Serializable {}

    /**
     * error to indicate a failure to fetch a resource. Usually this indicates that this resource has been sent over,
     * and the receiving end does not have this resource on the same place.
     */
    public static class ResourceException extends RuntimeException {
        public ResourceException(Exception cause, String message) {
            super(message, cause);
        }
    }
}
