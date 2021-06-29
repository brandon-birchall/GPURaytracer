import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import scene.Camera;
import scene.Scene;

import java.io.IOException;
import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

//Template from LWJGL website: https://www.lwjgl.org/guide
public class Window {
    // The window handle
    private long window;

    private ComputeShader computeShader;
    private QuadShader quadShader;

    private boolean resetFramebuffer = true;

    private int width = 1280, height = 720;
    private static final float MOVEMENT_SPEED = 1.0f;

    private int framebufferTex;

    public Scene currentScene;

    public void run() {
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

        quadShader.dispose();
        computeShader.dispose();
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);

        // Create the window
        window = glfwCreateWindow(width, height, "Raytracer", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            if ((key == GLFW_KEY_DOWN || key == GLFW_KEY_S) && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
                this.getCamera().getPosition().add(MOVEMENT_SPEED, 0.0f, 0.0f);
            }
            if ((key == GLFW_KEY_UP || key == GLFW_KEY_W) && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
                this.getCamera().getPosition().add(-MOVEMENT_SPEED, 0.0f, 0.0f);
            }
            if ((key == GLFW_KEY_LEFT || key == GLFW_KEY_A) && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
                this.getCamera().getPosition().add(0.0f, 0.0f, -MOVEMENT_SPEED);
            }
            if ((key == GLFW_KEY_RIGHT || key == GLFW_KEY_D) && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
                this.getCamera().getPosition().add(0.0f, 0.0f, MOVEMENT_SPEED);
            }
            if ((key == GLFW_KEY_SPACE || key == GLFW_KEY_Z) && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
                this.getCamera().getPosition().add(0.0f, -MOVEMENT_SPEED, 0.0f);
            }
            if ((key == GLFW_KEY_LEFT_SHIFT || key == GLFW_KEY_X) && (action == GLFW_PRESS || action == GLFW_REPEAT)) {
                this.getCamera().getPosition().add(0.0f, MOVEMENT_SPEED, 0.0f);
            }
        });


        //Set resize callback
        glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (Window.this.width != width || Window.this.height != height)) {
                    Window.this.width = width;
                    Window.this.height = height;
                    Window.this.resetFramebuffer = true;
                    computeShader.setDimensions(width, height);
                }
            }
        });

        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        glfwSwapInterval(1);


        // Make the window visible
        glfwShowWindow(window);


        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        GLUtil.setupDebugMessageCallback();

        createFramebufferTex();

        try {
            quadShader = new QuadShader("vertex.glsl", "fragment.glsl", framebufferTex);
            computeShader = new ComputeShader("compute.glsl", framebufferTex);
            computeShader.setDimensions(width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }

        currentScene = computeShader.currentScene;
    }


    private void createFramebufferTex() {
        this.framebufferTex = glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, framebufferTex);
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_RGBA32F, width, height);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public Camera getCamera() {
        return computeShader.camera;
    }

    private void traceScene() {
        computeShader.useProgram();

        if (resetFramebuffer) {
            recalculateProjectionMatrix();
        }

        computeShader.update();

        glUseProgram(0);
    }

    private void recalculateProjectionMatrix() {
        computeShader.projMatrix.setPerspective((float) Math.toRadians(60.0f), (float) width / height, 1f, 2f);
        glDeleteTextures(framebufferTex);
        createFramebufferTex();
        resetFramebuffer = false;
    }

    private void loop() {
        int x = 0;
        while ( !glfwWindowShouldClose(window) ) {
            x++;
            glfwPollEvents();

            glViewport(0,0,width, height);

            traceScene();
            quadShader.update();

            glfwSwapBuffers(window);
        }
    }
}
