package com.olgitt.olgitt.crashcan;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLU;
import android.opengl.Matrix;
import android.util.Log;

import com.jme3.math.Vector3f;
import com.jme3.scene.mesh.IndexBuffer;
import com.mokiat.data.front.parser.OBJDataReference;
import com.mokiat.data.front.parser.OBJFace;
import com.mokiat.data.front.parser.OBJMesh;
import com.mokiat.data.front.parser.OBJModel;
import com.mokiat.data.front.parser.OBJNormal;
import com.mokiat.data.front.parser.OBJObject;
import com.mokiat.data.front.parser.OBJTexCoord;
import com.mokiat.data.front.parser.OBJVertex;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.ListIterator;



public class myModel extends OBJModel{

    private static final int COORDS_PER_VERTEX = 3;

    // Use to access and set the view transformation
    private int mMVPMatrixHandle;
    private OBJModel myResModel = new OBJModel();

    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer textureBuffer;
    private ShortBuffer mIndexBuffer;


    private int numIndices = 0;
    private int[] VAO = new int[1];


    private Vector3f position = new Vector3f(0.0f, 0.0f, 0.0f);
    private Vector3f lookAt = new Vector3f(0.0f, 0.0f, 1.0f);
    private Vector3f upDir = new Vector3f(0.0f, 1.0f, 0.0f);


    public myModel( String filename, Context context) {

        myResModel = modelUtils.LoadModel(filename, context);


        // set the buffer to read the first coordinate
        int numOfVerts = myResModel.getVertices().size()*3;
        int numOfNorms = myResModel.getNormals().size()*3;
        int numOfTexs = myResModel.getTexCoords().size()*3;

        float[] vertexArray = new float[numOfVerts];
        float[] normalArray = new float[numOfNorms];
        float[] texCoordArray = new float[numOfTexs];
        ArrayList<Short> indexArray = new ArrayList<Short>();

        if (myResModel != null){

            for (OBJObject object : myResModel.getObjects()) {
                for (OBJMesh mesh : object.getMeshes()) {
                    final String materialName = mesh.getMaterialName();

                    for(OBJFace face : mesh.getFaces()){
                        for(OBJDataReference ref : face.getReferences()){
                            if(ref.hasVertexIndex()){
                            indexArray.add((short)(ref.vertexIndex));
                            }
                        }
                    }
                }
            }

            mIndexBuffer = ByteBuffer.allocateDirect(indexArray.size()* Short.BYTES)
                    .order(ByteOrder.nativeOrder()).asShortBuffer();
            for(short index = 0 ; index < indexArray.size(); index++){
                mIndexBuffer.put(indexArray.get(index));
            }
            mIndexBuffer.position(0);
            numIndices = mIndexBuffer.capacity();

            if(!myResModel.getVertices().isEmpty()) {

                for(ListIterator<OBJVertex> vertexIterator = myResModel.getVertices().listIterator();
                    vertexIterator.hasNext();) {
                    int i = vertexIterator.nextIndex()*3;
                    OBJVertex v = vertexIterator.next();
                    vertexArray[i] = v.x;
                    vertexArray[i + 1] = v.y;
                    vertexArray[i + 2] = v.z;
                }

                int vertBufferSize = numOfVerts* Float.BYTES;
                vertexBuffer = ByteBuffer.allocateDirect(vertBufferSize)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                vertexBuffer.put(vertexArray).position(0);
            }


            if(!myResModel.getNormals().isEmpty()) {
               for(ListIterator<OBJNormal> normalIterator = myResModel.getNormals().listIterator();
                    normalIterator.hasNext();) {
                    int i = normalIterator.nextIndex()*3;
                    OBJNormal n = normalIterator.next();
                    normalArray[i] = n.x;
                    normalArray[i + 1] = n.y;
                    normalArray[i + 2] = n.z;
                }
                int normBufferSize = numOfNorms* Float.BYTES;
                normalBuffer = ByteBuffer.allocateDirect(normBufferSize)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                normalBuffer.put(normalArray).position(0);
            }


            if(!myResModel.getTexCoords().isEmpty()) {
                for(ListIterator<OBJTexCoord> texCoordIterator = myResModel.getTexCoords().listIterator();texCoordIterator.hasNext();) {
                    int i = texCoordIterator.nextIndex()*3;
                    OBJTexCoord texCoord = texCoordIterator.next();
                    texCoordArray[i] = texCoord.u;
                    texCoordArray[i + 1] = texCoord.v;
                    texCoordArray[i + 2] = texCoord.w;
                }
                int texBufferSize = numOfTexs* Float.BYTES;
                textureBuffer = ByteBuffer.allocateDirect(texBufferSize)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                textureBuffer.put(texCoordArray).position(0);
            }
        }
    }

    public void init(int mProgram){
        GLES30.glUseProgram(mProgram);

        float[] scratch = {-1.0f, 0.0f, 0.0f, 0.0f,
                            0.0f, 2.0f, 0.0f, 0.0f,
                            0.0f, 0.0f, 2.0f, 1.0f,
                            0.0f, 0.0f,-0.2f, 3.0f};

        final int vertCount = myResModel.getVertices().size();

        int[] normVBO = new int[1];
        int[] vertVBO = new int[1];
        int[] indexVBO = new int[1];

        GLES30.glGenVertexArrays(1, VAO, 0);
        GLES30.glGenBuffers(1, vertVBO, 0);
        GLES30.glGenBuffers(1, normVBO, 0);
        GLES30.glGenBuffers(1, indexVBO, 0);

        GLES30.glBindVertexArray(VAO[0]);

        //Vertices
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertVBO[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                vertCount*3* Float.BYTES,
                vertexBuffer, GLES30.GL_STATIC_DRAW);
        int mPositionHandle = GLES30.glGetAttribLocation(mProgram, "vPosition");
        GLES30.glEnableVertexAttribArray(mPositionHandle);
        GLES30.glVertexAttribPointer(mPositionHandle,
                3,
                GLES30.GL_FLOAT, false,
                0, 0);

        //Normals
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, normVBO[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,
                vertCount*3*Float.BYTES,
                normalBuffer, GLES30.GL_STATIC_DRAW);
        int mNormalHandle = GLES30.glGetAttribLocation(mProgram, "vNormal");
        GLES30.glEnableVertexAttribArray(mNormalHandle);
        GLES30.glVertexAttribPointer(mNormalHandle,
                3,
                GLES30.GL_FLOAT, false,
                0, 0);

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexVBO[0]);
        Log.d("debug","\nmyModel:BindBuffer:indexVBO " + GLU.gluErrorString(GLES30.glGetError()));
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,
                numIndices* Short.BYTES,
                mIndexBuffer, GLES30.GL_STATIC_DRAW);
        Log.d("debug","\nmyModel:BufferData:index " + GLU.gluErrorString(GLES30.glGetError()));


/*        if (textureBuffer.hasRemaining()) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[2]);
            // Enable a handle to the vertex colors
            GLES30.glEnableVertexAttribArray(mColorHandle);
            // Set colors
            GLES30.glVertexAttribPointer(mColorHandle,
                    vertCount,
                    GLES30.GL_FLOAT, false,
                    0, 0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,0);
        }*/


        //just a placeholder for now
        int mColorHandle = GLES30.glGetUniformLocation(mProgram, "vColor");
        GLES30.glUniform4f(mColorHandle, 1.0f, 1.0f,1.0f, 1.0f);

        int mLightHandle = GLES30.glGetUniformLocation(mProgram, "vLight");
        GLES30.glUniform3f(mLightHandle, 0.0f, 0.3f,2.0f);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, scratch, 0);

        //unbind the VAO
        GLES30.glBindVertexArray(0);
    }


    
    public void draw(float[] mvpMatrix, int mProgram) {

        // Add program to OpenGL ES environment
        GLES30.glUseProgram(mProgram);
        GLES30.glBindVertexArray(VAO[0]);

        mMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        // Pass the projection and view transformation to the shader
        GLES30.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        int mLightHandle = GLES30.glGetUniformLocation(mProgram, "vLight");
        GLES30.glUniform3f(mLightHandle, 0.0f, 0.3f,2.0f);

        // Draw the model triangles
        GLES30.glDrawElements(GLES30.GL_TRIANGLES,
                numIndices,
                GLES30.GL_UNSIGNED_SHORT,
                0);

        Log.d("debug","\nmyModel:drawElements " + GLU.gluErrorString(GLES30.glGetError()));
        GLES30.glBindVertexArray(0);
    }

    public String getVertShader(){
        return "vertexShader";
    }

    public String getFragShader(){
        return "fragmentShader";
    }

    public Vector3f getPosition() { return position; }

}
