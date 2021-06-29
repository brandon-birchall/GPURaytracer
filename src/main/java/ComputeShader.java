import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import scene.Camera;
import scene.Material;
import scene.Scene;

import java.io.IOException;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;

public class ComputeShader extends ShaderProgram {
    private final int MAX_BOXES = 10;
    private final int MAX_SPHERES = 1000;
    private final int MAX_LIGHTS = 100;

    protected int workGroupSizeX;
    protected int workGroupSizeY;

    protected int framebufferImageBinding;

    protected final Camera camera;

    private final int framebufferTextureID;

    protected final Matrix4f viewMatrix = new Matrix4f();
    protected final Matrix4f projMatrix = new Matrix4f();
    protected final Matrix4f invMatrix = new Matrix4f();

    protected int width,height;
    private final int computeShaderObjectID;

    public Scene currentScene = new Scene();

    public ComputeShader(String computeShaderFilename, int framebufferTextureID) throws IOException {
        computeShaderObjectID = super.createShader(computeShaderFilename, GL_COMPUTE_SHADER);
        super.attachAndLink(computeShaderObjectID);

        //Create framebuffer texture
        this.framebufferTextureID = framebufferTextureID;

        camera = new Camera();


        //Use this shader
        useProgram();

        //Framebuffer
        IntBuffer params = BufferUtils.createIntBuffer(1);
        int loc = glGetUniformLocation(this.programID, "framebufferImage");
        glGetUniformiv(this.programID, loc, params);
        framebufferImageBinding = params.get(0);

        //Stop using this shader
        stopUsingProgram();
    }

    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void update() {

        //Update view matrix based on camera
        viewMatrix.setLookAt(camera.getPosition(), camera.getDirection(), camera.getUpDirection());

        //NDC space to World Space
        projMatrix.invertPerspectiveView(viewMatrix, invMatrix);

        //Uniforms
        setViewFrustumUniform();
        setLightArrayUniform();
        setSphereArrayUniform();

        IntBuffer workGroupSize = BufferUtils.createIntBuffer(3);
        glGetProgramiv(this.programID, GL_COMPUTE_WORK_GROUP_SIZE, workGroupSize);
        workGroupSizeX = workGroupSize.get(0);
        workGroupSizeY = workGroupSize.get(1);

        //Bind framebuffer image to texture
        glBindImageTexture(framebufferImageBinding, framebufferTextureID, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);

        int numGroupsX = (int) Math.ceil((double) width / workGroupSizeX);
        int numGroupsY = (int) Math.ceil((double) height / workGroupSizeY);

        glDispatchCompute(numGroupsX, numGroupsY, 1);

        glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT);

        //Unbind framebuffer image
        glBindImageTexture(framebufferImageBinding, 0, 0, false, 0, GL_WRITE_ONLY, GL_RGBA32F);
        stopUsingProgram();
    }

    private void setViewFrustumUniform() {
        //Set eye uniform
        setUniform("eye", camera.getPosition());


        //Computes view frustum
        Vector3f temp = new Vector3f();

        invMatrix.transformProject(temp.set(-1, -1, 0)).sub(camera.getPosition());
        setUniform("ray00", temp);

        invMatrix.transformProject(temp.set(-1, 1, 0)).sub(camera.getPosition());
        setUniform("ray01", temp);

        invMatrix.transformProject(temp.set(1, -1, 0)).sub(camera.getPosition());
        setUniform("ray10", temp);

        invMatrix.transformProject(temp.set(1, 1, 0)).sub(camera.getPosition());
        setUniform("ray11", temp);
    }

    private void setSphereArrayUniform() {
        for (int i = 0; i < MAX_SPHERES; i++) {
            Vector3f position = new Vector3f(0);
            float radius = 0;
            Material material = Material.DEFAULT_MATERIAL;

            if(i < currentScene.spheresInScene.size()) {
                position = currentScene.spheresInScene.get(i).getPosition();
                radius = currentScene.spheresInScene.get(i).getRadius();
                material = currentScene.spheresInScene.get(i).material;
            }

            this.setUniform("sceneSpheres[" + i + "].position", position);
            this.setUniform("sceneSpheres[" + i + "].radius", radius);
            this.setUniform("sceneSpheres[" + i + "].mat.albedo", material.getAlbedo());
            this.setUniform("sceneSpheres[" + i + "].mat.specularN", material.getSpecularN());
        }
        this.setUniform("numberOfSpheres", currentScene.spheresInScene.size());
    }

    private void setLightArrayUniform() {
        for (int i = 0; i < MAX_LIGHTS; i++) {
            Vector3f position = new Vector3f(0);
            Vector3f colour = new Vector3f(0);
            float brightness = 0;

            if(i < currentScene.lightsInScene.size()) {
                position = currentScene.lightsInScene.get(i).getPosition();
                colour = currentScene.lightsInScene.get(i).getColour();
                brightness = currentScene.lightsInScene.get(i).getBrightness();
            }

            this.setUniform("sceneLights[" + i + "].position", position);
            this.setUniform("sceneLights[" + i + "].colour", colour);
            this.setUniform("sceneLights[" + i + "].brightness", brightness);
        }
        this.setUniform("numberOfLights", currentScene.lightsInScene.size());
    }
    @Override
    public void dispose() {
        glDetachShader(programID, computeShaderObjectID);
        glDeleteShader(computeShaderObjectID);
        glDeleteProgram(programID);
    }


}
