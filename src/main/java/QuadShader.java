import java.io.IOException;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.opengl.GL33.*;

public class QuadShader extends ShaderProgram {

    private final int vaoID;
    private final int sampler;
    private final int framebufferTextureID;
    private int texUniform;

    private final int vertexShader, fragmentShader;

    public QuadShader(String vertexFilename, String fragmentFilename, int framebufferTextureID) throws IOException {
        this.vertexShader = super.createShader(vertexFilename, GL_VERTEX_SHADER);
        this.fragmentShader = super.createShader(fragmentFilename, GL_FRAGMENT_SHADER);

        super.attachAndLink(vertexShader, fragmentShader);

        this.framebufferTextureID = framebufferTextureID;

        this.vaoID = glGenVertexArrays();
        this.sampler = glGenSamplers();
        glSamplerParameteri(this.sampler, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glSamplerParameteri(this.sampler, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        //Set texture unit
        useProgram();
        texUniform = glGetUniformLocation(programID, "tex");
        glUniform1i(texUniform, 0);
        stopUsingProgram();
    }

    @Override
    public void update() {
        useProgram();
        glBindVertexArray(vaoID);
        glBindTexture(GL_TEXTURE_2D, framebufferTextureID);
        glBindSampler(0, this.sampler);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glBindSampler(0, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        stopUsingProgram();
    }

    @Override
    public void dispose() {
        glDetachShader(programID, vertexShader);
        glDeleteShader(vertexShader);
        glDetachShader(programID, fragmentShader);
        glDeleteShader(fragmentShader);
        glDeleteProgram(programID);
    }
}
