package scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position = new Vector3f();
    private final Vector3f direction = new Vector3f(0.0f, 0.5f, 0.0f);
    private final Vector3f upDirection = new Vector3f(0.0f, 1.0f, 0.0f);

    //Getters

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getDirection() {
        return direction;
    }

    public Vector3f getUpDirection() {
        return upDirection;
    }
}
