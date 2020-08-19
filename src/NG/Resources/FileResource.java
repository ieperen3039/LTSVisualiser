package NG.Resources;

import NG.Tools.Directory;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geert van Ieperen created on 26-2-2020.
 */
public class FileResource<T> extends Resource<T> {
    private static final Map<Path, FileResource<?>> allFileResources = new HashMap<>();

    /** must be relative to file directory, for the sake of serialisation */
    private final Path fileLocation;
    private final FileLoader<T> loader;

    private FileResource(FileLoader<T> loader, Path relativePath) {
        super();
        this.loader = loader;
        this.fileLocation = relativePath;
    }

    @Override
    protected T reload() throws ResourceException {
        try {
            return loader.apply(fileLocation.toRealPath());

        } catch (IOException e) {
            throw new ResourceException(e, fileLocation.toString() + ": " + e.getMessage());
        }
    }

    public static <T> FileResource<T> get(FileLoader<T> loader, Path path) {
        Path relativePath = Directory.workDirectory().relativize(path);
        //noinspection unchecked
        return (FileResource<T>) allFileResources.computeIfAbsent(
                relativePath, (p) -> new FileResource<>(loader, p)
        );
    }

    public interface FileLoader<T> extends Serializable {
        T apply(Path path) throws IOException;
    }
}
