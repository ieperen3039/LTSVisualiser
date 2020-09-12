package NG.Resources;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Geert van Ieperen created on 26-2-2020.
 */
public class LazyInit<T> implements Serializable {
    private final ResourceGenerator<? extends T> generator;
    private final ResourceCleaner<T> cleanup;
    /** the cached element */
    protected transient T element = null;

    /**
     * a resource to use with lambdas.
     * @param generator is called to generate a new element
     * @param cleanup   is called on the element when this is dropped. If no action is required, use null.
     */
    public LazyInit(ResourceGenerator<? extends T> generator, ResourceCleaner<T> cleanup) {
        this.generator = generator;
        this.cleanup = cleanup;
    }

    public LazyInit(ResourceGenerator<? extends T> generator) {
        this.generator = generator;
        this.cleanup = null;
    }

    /**
     * reloads the resource.
     * @throws ResourceException whenever the resource could not be generated
     */
    protected T reload() throws ResourceException {
        return generator.get();
    }

    /**
     * drops the cached element, causing a reload on the next get
     */
    public void drop() {
        if (cleanup != null && element != null) {
            cleanup.accept(element);
        }

        element = null;
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

        return element;
    }

    /**
     * execute the given action on this element, only if it exists
     * @param action an action that receives the element iff it exists.
     */
    public void ifPresent(Consumer<T> action) {
        if (element != null) {
            action.accept(element);
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

    /**
     * create a resource that is generated from another resource.
     * @param source    a resource generating an element of type A
     * @param extractor a function that generates the desired element of type B using source
     * @return a resource generating an element of type B
     */
    public static <A, B> LazyInit<B> derive(LazyInit<A> source, ResourceConverter<A, B> extractor) {
        return new LazyInit<>(() -> extractor.apply(source.get()), null);
    }

    public static <A, B> LazyInit<B> derive(
            LazyInit<A> source, ResourceConverter<A, B> extractor, ResourceCleaner<B> cleanup
    ) {
        return new LazyInit<>(() -> extractor.apply(source.get()), cleanup);
    }

    /** serializable version of {@link Supplier} */
    public interface ResourceGenerator<T2> extends Supplier<T2>, Serializable {}

    /** serializable version of {@link Consumer} */
    public interface ResourceCleaner<T2> extends Consumer<T2>, Serializable {}

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
