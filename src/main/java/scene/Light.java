package scene;

import org.joml.Vector3f;

public class Light {
    private Vector3f position;
    private Vector3f colour;
    private float brightness;



    public Light(Vector3f position, Vector3f colour, float brightness) {
        this.position = position;
        this.colour = colour;
        this.brightness = brightness;
    }

    public float getBrightness() {
        return brightness;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public Vector3f getColour() {
        return colour;
    }

    public void setColour(Vector3f colour) {
        this.colour = colour;
    }
}
