/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.olgitt.olgitt.crashcan;

import android.app.Activity;
import android.content.res.AssetManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.threed.jpct.Camera;
import com.threed.jpct.FrameBuffer;
import com.threed.jpct.Interact2D;
import com.threed.jpct.Light;
import com.threed.jpct.Loader;
import com.threed.jpct.Object3D;
import com.threed.jpct.Primitives;
import com.threed.jpct.RGBColor;
import com.threed.jpct.SimpleVector;
import com.threed.jpct.Texture;
import com.threed.jpct.TextureManager;
import com.threed.jpct.World;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.content.ContentValues.TAG;
import static com.threed.jpct.Loader.loadOBJ;
import static com.threed.jpct.Object3D.mergeAll;
import static java.lang.Math.PI;


public class LookActivity extends Activity implements GLSurfaceView.Renderer , View.OnTouchListener {

    private GLSurfaceView glView;
    private World world;
    private FrameBuffer frameBuffer;
    private Light light;
    private SimpleVector rotAxis = new SimpleVector(0.0f, 1.0f, 0.0f);
    private Texture t1;
    private Camera cam;
    private static final SimpleVector origin = new SimpleVector(0.0f, 0.0f, 0.0f);
    private static final SimpleVector XAxis = new SimpleVector(1.0f, 0.0f, 0.0f);
    private static final SimpleVector YAxis = new SimpleVector(0.0f, 1.0f, 0.0f);
    private static final SimpleVector ZAxis = new SimpleVector(0.0f, 0.0f, 1.0f);

    private Object3D skull;
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);

        //init surfaceView
        glView = new GLSurfaceView(this);
        glView.setEGLContextClientVersion(2);
        glView.setRenderer(this);
        glView.setDebugFlags(GLSurfaceView.DEBUG_CHECK_GL_ERROR);
        setContentView(glView);
        glView.setOnTouchListener(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        glView.onResume();
    }

    @Override
    public void onPause() {
        cleanUp();
        super.onPause();
        glView.onPause();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        world = new World();
        t1 = new Texture(64, 64, RGBColor.BLUE);
        TextureManager.getInstance().addTexture("t1", t1);

        AssetManager assets = this.getApplicationContext().getAssets();
        try {
            InputStream objIs = assets.open("3DModels/ReducedSkull.obj");
            InputStream mtlIs = assets.open("3DModels/ReducedSkull.mtl");
            Log.d(TAG, "\n Bytes available: " + objIs.available());
            Object3D[] skullArray = loadOBJ(objIs, mtlIs, 1);
            skull = mergeAll(skullArray);
            int arrayLength = skullArray.length;
            int triangles = skull.getMesh().getTriangleCount();
            Log.d(TAG,"\nloadedBunny: " + arrayLength + " and " + triangles );
            skull.setOrigin(skull.getCenter());
            skull.rotateZ((float) PI);
            skull.translate(0, 0, 1.0f);
            skull.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
            world.addObjects(skull);

        } catch (Throwable e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
        }


            cam = new Camera();
            cam.setFOV(70);
            cam.setPosition(0.0f,0.0f, -10.0f);
            cam.lookAt(origin);
            world.setCameraTo(cam);

            light = new Light(world);
            light.setIntensity(228, 228, 228);
            light.setPosition(origin);
            world.setAmbientLight(128,128,128);


        world.buildAllObjects();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        frameBuffer = new FrameBuffer(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        frameBuffer.clear();

        rotAxis.rotateZ(0.01f);
        rotAxis.rotateX(0.01f);
        skull.rotateY(0.01f);
        world.renderScene(frameBuffer);
        world.draw(frameBuffer);
        frameBuffer.display();
    }


    //TODO: should REALLY check up on this, it currently makes the game reload ALL resources.
    public void cleanUp(){
        TextureManager.getInstance().removeTexture("t1");
    }

    private void moveObject(float x, float y){
        //TODO; Implement some way to select and move an object, preferrably through momentum.
    }

    private float lastX, lastY;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                final float currX = event.getX();
                final float currY = event.getY();
                final float dx = currX - lastX;
                final float dy = currY - lastY;
                glView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        moveObject(currX, currY);
                    }
                });
                lastX = currX;
                lastY = currY;
        }
        return false;
    }
}



