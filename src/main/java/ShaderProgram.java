import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.List;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindFragDataLocation;
import static org.lwjgl.opengl.GL43.GL_COMPUTE_SHADER;


public abstract class ShaderProgram {

    protected int programID;


    public void useProgram() {
        glUseProgram(programID);
    }
    public void stopUsingProgram() {
        glUseProgram(0);
    }


    //Methods for setting the values of uniform variables

    protected void setUniform(String uniformName, Vector3f value) {
        int uniformLocation = glGetUniformLocation(programID, uniformName);
        glUniform3f(uniformLocation, value.x, value.y, value.z);
    }
    protected void setUniform(String uniformName, int value) {
        int uniformLocation = glGetUniformLocation(programID, uniformName);
        glUniform1i(uniformLocation, value);
    }
    protected void setUniform(String uniformName, Vector2f value) {
        int uniformLocation = glGetUniformLocation(programID, uniformName);
        glUniform2f(uniformLocation, value.x, value.y);
    }
    protected void setUniform(String uniformName, float value) {
        int uniformLocation = glGetUniformLocation(programID, uniformName);
        glUniform1f(uniformLocation, value);
    }




    //Creates a shader object from the given file
    public int createShader(String filename, int type) throws IOException {
        //Shader pointer
        int shader = glCreateShader(type);

        //Read file into bytebuffer
        byte[] sourceData = this.getClass().getResourceAsStream(filename).readAllBytes();

        ByteBuffer source = ByteBuffer.wrap(sourceData).flip();


        PointerBuffer strings = BufferUtils.createPointerBuffer(1);
        IntBuffer lengths = BufferUtils.createIntBuffer(1);

        strings.put(0, source);
        lengths.put(0, source.remaining());

        //Set source
        //glShaderSource(shader, strings, lengths);
        glShaderSource(shader, new String(sourceData).subSequence(0, sourceData.length));

        //Compile and print any errors
        glCompileShader(shader);
        int compiled = glGetShaderi(shader, GL_COMPILE_STATUS);
        String shaderLog = glGetShaderInfoLog(shader);
        if (shaderLog.trim().length() > 0) {
            System.err.println(shaderLog);
        }
        if (compiled == 0) {
            throw new AssertionError("Could not compile shader");
        }
        return shader;
    }

    public void attachAndLink(int shaderObjectID) {
        //Create shader program
        int program = glCreateProgram();

        //Attach compiled shader
        glAttachShader(program, shaderObjectID);

        //Link shader
        link(program);

        this.programID = program;
    }

    public void attachAndLink(int vertexShaderID, int fragmentShaderID) {
        //Create shader program
        int program = glCreateProgram();

        //Attach compiled shader
        glAttachShader(program, vertexShaderID);
        glAttachShader(program, fragmentShaderID);

        glBindFragDataLocation(program, 0, "color");

        //Link shader
        link(program);

        this.programID = program;
    }

    private void link(int pid) throws AssertionError {
        glLinkProgram(pid);
        //Get link status and report errors
        int linked = glGetProgrami(pid, GL_LINK_STATUS);
        String programLog = glGetProgramInfoLog(pid);
        if (programLog.trim().length() > 0) {
            System.err.println(programLog);
        }
        if (linked == 0) {
            throw new AssertionError("Could not link program");
        }
    }

    public abstract void dispose();

    /**
     * Called once per frame
     */
    public abstract void update();
}
