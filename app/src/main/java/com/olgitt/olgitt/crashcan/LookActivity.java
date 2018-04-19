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
import com.threed.jpct.Matrix;
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
import static com.threed.jpct.Interact2D.projectCenter3D2D;
import static com.threed.jpct.Interact2D.reproject2D3DWS;
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
    private static int roomID = 0;

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
        t1 = new Texture(64, 64, RGBColor.WHITE);
        TextureManager.getInstance().addTexture("t1", t1);
        AssetManager assets = this.getApplicationContext().getAssets();
        try {
            InputStream objIs = assets.open("3DModels/ReducedSkull.obj");
            InputStream mtlIs = assets.open("3DModels/ReducedSkull.mtl");
            Object3D[] skullArray = loadOBJ(objIs, mtlIs, 1);
            skull = mergeAll(skullArray);

            skull.rotateZ((float) PI);
            skull.translate(0f, 0f, 1.0f);
            skull.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
            //skull.addCollisionListener();
            world.addObjects(skull);

        } catch (Throwable e) {
            e.printStackTrace();
            Log.d(TAG, e.getLocalizedMessage());
        }

        //TODO find out how to find the aspect ratio of the surfaceView here
        float scaleHeight = 1.95f;
        Object3D room = Primitives.getBox(4.5f, scaleHeight);
        roomID = room.getID();
        room.invert();
        room.rotateY((float)PI/4);
        room.rotateZ((float)PI/2);
        room.setCollisionMode(Object3D.COLLISION_CHECK_OTHERS);
        room.setTexture("t1");
        world.addObject(room);

        cam = new Camera();
        cam.setFOV(70);
        cam.setPosition(0.0f,0.0f, -15.0f);
        cam.lookAt(origin);
        world.setCameraTo(cam);

        light = new Light(world);
        light.setIntensity(152, 152, 152);
        light.setPosition(new SimpleVector(0.0f,2.0f,0.0f));
        world.setAmbientLight(64,64,64);

        world.buildAllObjects();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        frameBuffer = new FrameBuffer(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        frameBuffer.clear();
        skull.rotateY(0.01f);

        world.renderScene(frameBuffer);
        world.draw(frameBuffer);
        frameBuffer.display();
    }


    //TODO: should REALLY check up on this, it currently makes the game reload ALL resources.
    public void cleanUp(){
        TextureManager.getInstance().removeTexture("t1");
    }

    private void moveObject(int x, int y, int lx, int ly){
        //TODO; Implement some way to select and move an object, preferrably through momentum.
        SimpleVector dir = reproject2D3DWS(cam,
                frameBuffer, x, y ).normalize();

        SimpleVector lastDir = reproject2D3DWS(cam,
                frameBuffer, lx, ly).normalize();


        Object[] res = world.calcMinDistanceAndObject3D(cam.getPosition(),
                lastDir, 1000);

        if(res[1] != null){
            Object3D resObj = (Object3D) res[1];
            if(resObj.getID() != roomID){
                float d = (float) res[0];
                dir.scalarMul(d);
                SimpleVector newPos = dir.calcAdd(cam.getPosition());
                resObj.clearTranslation();
                resObj.translate(newPos.x, newPos.y, 0.0f);
            }
        }
    }

    private int lastX, lastY;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                lastX = (int) event.getX();
                lastY = (int) event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                final int currX = (int) (event.getX());
                final int currY = (int) (event.getY());

                glView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        moveObject(currX, currY, lastX, lastY);
                    }
                });
                lastX = currX;
                lastY = currY;
                return true;
        }
        return false;
    }
}



