package scene;

import java.util.ArrayList;
import java.util.List;

public class Scene {
    public final List<Light> lightsInScene = new ArrayList<>();
    public final List<Sphere> spheresInScene = new ArrayList<>();
    public final Camera camera = new Camera();
}
