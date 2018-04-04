package com.olgitt.olgitt.crashcan;

import android.content.Context;
import android.nfc.Tag;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLES32;
import android.util.Log;

import com.jme3.math.Vector3f;
import com.mokiat.data.front.parser.OBJDataReference;
import com.mokiat.data.front.parser.OBJFace;
import com.mokiat.data.front.parser.OBJMesh;
import com.mokiat.data.front.parser.OBJModel;
import com.mokiat.data.front.parser.OBJNormal;
import com.mokiat.data.front.parser.OBJObject;
import com.mokiat.data.front.parser.OBJTexCoord;
import com.mokiat.data.front.parser.OBJVertex;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static android.content.ContentValues.TAG;
import static java.nio.charset.StandardCharsets.UTF_8;


public class myModel extends OBJModel{

    private static final int COORDS_PER_VERTEX = 3;


    // Use to access and set the view transformation
    private int mMVPMatrixHandle;
    private OBJModel myResModel = new OBJModel();

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private FloatBuffer normalBuffer;
    private IntBuffer indexBuffer;

    private int mPositionHandle;
    private int mColorHandle;
    private int mNormalHandle;

    private Vector3f position = new Vector3f(0.0f, 0.0f, 0.0f);


    public myModel( String filename, Context context) {

        myResModel = modelUtils.LoadModel(filename, context);

        // set the buffer to read the first coordinate
        int numOfVerts = myResModel.getVertices().size()*3;
        int numOfNorms = myResModel.getNormals().size()*3;
        int numOfTexs = myResModel.getTexCoords().size()*3;

        float[] vertexArray = new float[numOfVerts];
        float[] normalArray = new float[numOfNorms];
        float[] texCoordArray = new float[numOfTexs];

        if (myResModel != null){
            int intBufferSize = numOfVerts* Integer.SIZE;
            indexBuffer = ByteBuffer.allocateDirect(intBufferSize)
                    .order(ByteOrder.nativeOrder()).asIntBuffer();

            for (OBJObject object : myResModel.getObjects()) {
                for (OBJMesh mesh : object.getMeshes()) {
                    final String materialName = mesh.getMaterialName();
                    for(OBJFace face: mesh.getFaces()){
                        for(OBJDataReference ref: face.getReferences()){
                            indexBuffer.put(ref.vertexIndex);
                        }
                    }
                }
            }





            if(!myResModel.getVertices().isEmpty()) {
                int vertBufferSize = numOfVerts*Float.SIZE;

                vertexBuffer = ByteBuffer.allocateDirect(vertBufferSize)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                for(ListIterator<OBJVertex> vertexIterator = myResModel.getVertices().listIterator();
                    vertexIterator.hasNext();) {
                    int i = vertexIterator.nextIndex();
                    OBJVertex v = vertexIterator.next();
                    vertexArray[i] = v.x;
                    vertexArray[i + 1] = v.y;
                    vertexArray[i + 2] = v.z;
                }
                vertexBuffer.put(vertexArray).position(0);
            }

            if(!myResModel.getNormals().isEmpty()) {
                int normBufferSize = numOfNorms*Float.SIZE;
                normalBuffer = ByteBuffer.allocateDirect(normBufferSize)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                for(ListIterator<OBJNormal> normalIterator = myResModel.getNormals().listIterator();
                    normalIterator.hasNext();) {
                    int i = normalIterator.nextIndex();
                    OBJNormal n = normalIterator.next();
                    normalArray[i] = n.x;
                    normalArray[i + 1] = n.y;
                    normalArray[i + 2] = n.z;
                }
                normalBuffer.put(normalArray).position(0);
            }

            if(!myResModel.getTexCoords().isEmpty()) {
                int texBufferSize = numOfTexs*Float.SIZE;
                textureBuffer = ByteBuffer.allocateDirect(texBufferSize)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                for(ListIterator<OBJTexCoord> texCoordIterator = myResModel.getTexCoords().listIterator();texCoordIterator.hasNext();) {
                    int i = texCoordIterator.nextIndex();
                    OBJTexCoord texCoord = texCoordIterator.next();
                    texCoordArray[i] = texCoord.u;
                    texCoordArray[i + 1] = texCoord.v;
                    texCoordArray[i + 2] = texCoord.w;
                }
                textureBuffer.put(texCoordArray).position(0);

            }
        }


    }



    public void draw(float[] mvpMatrix, int mProgram) {

        int vertexCount = myResModel.getVertices().size();
        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
        // Enable a handle to the triangle vertices
        GLES30.glEnableVertexAttribArray(mPositionHandle);

//        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mPositionHandle);
//        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
//                vertexBuffer.capacity()*3,
//                vertexBuffer,
//                GLES30.GL_STATIC_DRAW);

        // Prepare the triangle coordinate data
        GLES30.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES30.GL_FLOAT, false,
                0, 0);
        GLES30.glEnableVertexAttribArray(mPositionHandle);




        if (normalBuffer.hasRemaining()){
            // get handle to fragment shader's vColor member
            mNormalHandle = GLES30.glGetAttribLocation(mProgram, "vNormal");
            // Enable a handle to the vertex colors
            GLES30.glEnableVertexAttribArray(mNormalHandle);

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mNormalHandle);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                    normalBuffer.capacity()*3,
                    normalBuffer,
                    GLES30.GL_STATIC_DRAW);
            // Set colors
            GLES30.glVertexAttribPointer(mNormalHandle, COORDS_PER_VERTEX,
                    GLES30.GL_FLOAT, false,
                    3 * Float.SIZE, 0);
        }
/*
        if (textureBuffer.hasRemaining()) {
            // get handle to fragment shader's vColor member
            mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
            // Enable a handle to the vertex colors
            GLES30.glEnableVertexAttribArray(mColorHandle);
            // Set colors
            GLES30.glVertexAttribPointer(mColorHandle, COORDS_PER_VERTEX,
                    GLES30.GL_FLOAT, false,
                    3 * Float.SIZE, textureBuffer);
        }*/

        //just a placeholder for now
        mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
        GLES30.glUniform4f(mColorHandle, 0.3f, 0.3f,0.3f, 0.0f);


        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);


        // Draw the model triangles
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, vertexCount, GLES30.GL_UNSIGNED_BYTE, indexBuffer);
    }

    //public int getmProgram() {return mProgram;}

    public String getVertShader(){
        return "vertexShader";
    }

    public String getFragShader(){
        return "fragmentShader";
    }

    public Vector3f getPosition() { return position; }

}
