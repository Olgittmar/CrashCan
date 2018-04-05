package com.olgitt.olgitt.crashcan;

import android.content.Context;
import android.opengl.EGLContext;
import android.opengl.GLES30;
import android.opengl.GLES32;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.support.annotation.IntDef;
import android.util.Log;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class myRenderer implements GLSurfaceView.Renderer {

    private static final float near = 2.0f;
    private static final float far = 7.0f;
    private static final float top = 1.0f;
    private static final float bottom = -1.0f;

    private ArrayList<myModel> ModelList = new ArrayList<myModel>();
    private Context parentContext;

    // mMVPMatrix is an abbreviation for "Model View Projection Matrix"
    private float[] mMVPMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mRotationMatrix = new  float[16];

    private int mProgram;


    public myRenderer(Context context){
        parentContext = context;
    }

    //init function
    public void onSurfaceCreated( GL10 gl10, EGLConfig eglConfig) {

        //Init openGL
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glClearColor(0.0f,0.0f,0.0f,1.0f);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);


        //init shaders
        String vertexShaderCode = modelUtils.loadShaderString("vertShader.vert", parentContext);
        String fragmentShaderCode = modelUtils.loadShaderString("fragShader.frag", parentContext);

        int vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES30.glCreateProgram();
        // add the vertex shader to program
        GLES30.glAttachShader(mProgram, vertexShader);
        // add the fragment shader to program
        GLES30.glAttachShader(mProgram, fragmentShader);
        // creates OpenGL ES program executables
        GLES30.glLinkProgram(mProgram);
        GLES30.glValidateProgram(mProgram);

        Log.d("debug", "myRenderer:62 " + GLU.gluErrorString(GLES30.glGetError()));

        int[] linkStatus = new int[1];
        GLES30.glGetProgramiv(mProgram, GLES30.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.d("error","\nProgram not linked, Info log:");
            String error = GLES30.glGetProgramInfoLog(mProgram);
            Log.d("error", error);
        }

        //init models
        myModel bunnyModel = new myModel("3DModels/bunnyplus.obj", parentContext);
        bunnyModel.init(mProgram);
        ModelList.add(bunnyModel);
        //myModel skullModel = new myModel("3DModels/ReducedSkull.obj", this);
        //ModelList.add(skullModel);
    }

    //display
    public void onSurfaceChanged( GL10 gl10, int Width, int Height) {
        GLES30.glViewport(0,0, Width, Height);
        float ratio = (float) Width / Height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0,
                -ratio, ratio,
                bottom, top,
                near, far);

    }


    //resizing the surfaceView
    public void onDrawFrame( GL10 gl10 ) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        float[] scratch = new float[16];

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0,
                0f, 0f, -3.0f,
                0f, 0f, 0f,
                0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0,
                mProjectionMatrix, 0,
                mViewMatrix, 0);

        // Create a rotation transformation for the triangle
        Matrix.setRotateM(mRotationMatrix, 0,
                0, 0, 0, 1.0f);

        // Combine the rotation matrix with the projection and camera view
        // Note that the mMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);
        Log.d("debug", "myRenderer:115 " + GLU.gluErrorString(GLES30.glGetError()));

        // Draw models
        if (!ModelList.isEmpty()){
            for (myModel model : ModelList){
                model.draw(scratch, mProgram);
            }
        }
     }

    public void setLookAtM(float[] M){
        mViewMatrix = M;
    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES30.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES30.GL_FRAGMENT_SHADER)
        int[] compiled = new int[1];
        int shader = GLES30.glCreateShader(type);
        Log.d("debug", "\n" + shaderCode);

        // add the source code to the shader and compile it
        GLES30.glShaderSource(shader, shaderCode);
        GLES30.glCompileShader(shader);

        GLES32.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.d("debug", "Could not compile shader " + type + ":");
            Log.d("debug", " " + GLES30.glGetShaderInfoLog(shader));
            GLES30.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }



}