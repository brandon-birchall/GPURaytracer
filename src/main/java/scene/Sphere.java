package scene;

import org.joml.Vector3f;

public class Sphere {
    public final Material material;

    int x = 0;
    int offset = (int) (Math.random() * 100);
    int rate = 40;

    public Sphere(Material material, Vector3f position, float radius) {
        this.material = material;
        this.position = position;
        this.radius = radius;
    }

    public Vector3f getPosition() {
        position.y = (float) (2 * Math.sin(((float)x++ / rate) + offset));
        return position;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    private Vector3f position;
    private float radius;
}
