package NG.Rendering.MeshLoading;

import NG.DataStructures.Generic.Color4f;
import NG.Resources.FileResource;
import NG.Resources.Resource;
import NG.Tools.Directory;
import NG.Tools.Vectors;
import org.joml.Vector2fc;
import org.joml.Vector3fc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author Geert van Ieperen created on 28-2-2019.
 */
public class MeshFile {
    private final List<Vector2fc> textureCoords;
    private final List<Vector3fc> vertices;
    private final List<Vector3fc> normals;
    private final List<Mesh.Face> faces;
    private final List<Color4f> colors;
    private final String name;

    public MeshFile(
            String name, List<Vector3fc> vertices, List<Vector3fc> normals, List<Mesh.Face> faces,
            List<Vector2fc> textureCoords, List<Color4f> colors
    ) {
        this.name = name;
        this.textureCoords = textureCoords;
        this.vertices = vertices;
        this.normals = normals;
        this.faces = faces;
        this.colors = colors;
    }

    public boolean isTextured() {
        return !getTextureCoords().isEmpty();
    }

    public boolean isColored() {
        return !getColors().isEmpty();
    }

    public List<Vector2fc> getTextureCoords() {
        return textureCoords;
    }

    public List<Vector3fc> getVertices() {
        return vertices;
    }

    public List<Vector3fc> getNormals() {
        return normals;
    }

    public List<Color4f> getColors() {
        return colors;
    }

    public List<Mesh.Face> getFaces() {
        return faces;
    }

    public Mesh getMesh() {
        if (isTextured()) {
            return new SmoothMesh(getVertices(), getNormals(), getTextureCoords(), getFaces());

        } else if (isColored()) {
            return new FlatMesh(getVertices(), getNormals(), getColors(), getFaces());

        } else {
            return new FlatMesh(getVertices(), getNormals(), getFaces());
        }
    }

    @Override
    public String toString() {
        return name;
    }

    private static MeshFile loadFile(Path file, Vector3fc scaling) throws IOException {
        String fileName = file.getFileName().toString();

        assert fileName.contains(".") : fileName;
        String extension = fileName.substring(fileName.lastIndexOf('.'));

        switch (extension) {
            case ".obj":
                return FileLoaders.loadOBJ(scaling, file, fileName);
            case ".ply":
                return FileLoaders.loadPLY(scaling, file, fileName);
            default:
                throw new UnsupportedMeshFileException(fileName);
        }
    }

    public static Resource<MeshFile> createResource(Directory meshes, String... path) {
        return createResource(Vectors.Scaling.UNIFORM, meshes, path);
    }

    public static Resource<MeshFile> createResource(Vector3fc scaling, Directory meshes, String... path) {
        return createResource(scaling, meshes.getPath(path));
    }

    public static Resource<MeshFile> createResource(Path path) {
        return createResource(Vectors.Scaling.UNIFORM, path);
    }

    public static Resource<MeshFile> createResource(Vector3fc scaling, Path path) {
        return FileResource.get((p) -> loadFile(p, scaling), path);
    }

    public static class UnsupportedMeshFileException extends IOException {
        public UnsupportedMeshFileException(String fileName) {
            super(fileName);
        }
    }
}
