import org.joml.Vector3f;
import scene.Light;
import scene.Material;
import scene.Sphere;

public class Main {
    private static final int LIGHTS = 1;
    private static final int SPHERES = 3;
    private static final int SPHERE_SPREAD = 10;
    private static final int LIGHT_SPREAD = 5;

    public static void main(String[] args) {
        Window w = new Window();
        w.init();


        for (int i = 0; i < SPHERES; i++) {
            w.currentScene.spheresInScene.add(
                    new Sphere(
                            Material.DEFAULT_MATERIAL, new Vector3f((float) ((Math.random() * SPHERE_SPREAD) - SPHERE_SPREAD / 2),(float) ((Math.random() * SPHERE_SPREAD) - SPHERE_SPREAD / 2),(float) ((Math.random() * SPHERE_SPREAD) - SPHERE_SPREAD / 2)),
                            (float) (((Math.random() + 0.4) * 0.5))));
        }

        for (int i = 0; i < LIGHTS; i++) {
            w.currentScene.lightsInScene.add(
                    new Light(new Vector3f((float) ((Math.random() * LIGHT_SPREAD) - LIGHT_SPREAD / 2),(float) ((Math.random() * LIGHT_SPREAD) - LIGHT_SPREAD / 2),(float) ((Math.random() * LIGHT_SPREAD) - LIGHT_SPREAD / 2)),
                            new Vector3f((float)Math.random(), (float)Math.random(), (float)Math.random()),
                            (float) ((Math.random() + 5) * 3)));
        }

        w.currentScene.lightsInScene.add(new Light(new Vector3f(0.0f, 200.0f, 0.0f), new Vector3f(1.0f), 2000f));

        w.run();
    }
}
