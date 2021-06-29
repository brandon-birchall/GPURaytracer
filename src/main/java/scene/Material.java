package scene;

import org.joml.Vector3f;

public class Material {
    public static final Material DEFAULT_MATERIAL = new Material(new Vector3f(1.0f, 1.0f, 1.0f), 50);

    private final Vector3f albedo;
    private final float specularN;

    public Material(Vector3f albedo, float specularN) {
        this.albedo = albedo;
        this.specularN = specularN;
    }

    public float getSpecularN() {
        return specularN;
    }

    public Vector3f getAlbedo() {
        return albedo;
    }

}
