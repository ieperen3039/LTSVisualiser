package NG.Resources;

import NG.Tools.Toolbox;

/**
 * @author Geert van Ieperen created on 26-2-2020.
 */
public class GeneratorResource<T> extends Resource<T> {
    private final ResourceGenerator<? extends T> generator;
    private final ResourceCleaner<T> cleanup;

    /**
     * a resource to use with lambdas.
     * @param generator is called to generate a new element
     * @param cleanup   is called on the element when this is dropped. If no action is required, use null.
     */
    public GeneratorResource(ResourceGenerator<? extends T> generator, ResourceCleaner<T> cleanup) {
        this.generator = generator;
        this.cleanup = cleanup;
    }

    public GeneratorResource(ResourceGenerator<? extends T> generator) {
        this.generator = generator;
        this.cleanup = null;
    }

    @Override
    protected T reload() throws ResourceException {
        return generator.get();
    }

    @Override
    public void drop() {
        if (cleanup != null && element != null) {
            cleanup.accept(element);
            Toolbox.checkGLError(String.valueOf(element));
        }

        super.drop();
    }

}
