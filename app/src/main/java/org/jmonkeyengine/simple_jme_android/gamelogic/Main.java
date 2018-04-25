package org.jmonkeyengine.simple_jme_android.gamelogic;

import android.nfc.Tag;
import android.util.Log;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.InputManager;
import com.jme3.input.TouchInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.InputListener;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Texture;
import com.jme3.util.TangentBinormalGenerator;

import java.util.logging.Logger;

import static android.content.ContentValues.TAG;

/**
 * Created by potterec on 3/17/2016.
 * Edited by Olgitt from 24/4/2018
 */
public class Main extends SimpleApplication {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    protected Spatial skull;
    private Node selectable;
    private static final Plane ZPlane = new Plane(new Vector3f(0.0f,0.0f,1.0f), 0);

    @Override
    public void simpleInitApp() {
        selectable = new Node("Selectable");
        rootNode.attachChild(selectable);

        Spatial room = assetManager.loadModel("Models/CrashBox.obj");
        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        wallMat.setBoolean("UseMaterialColors", true);
        wallMat.setColor("Ambient", ColorRGBA.White);
        wallMat.setColor("Diffuse", ColorRGBA.White);
        room.setMaterial(wallMat);
        room.rotate((float) Math.PI / 2, 0f, 0f);
        room.scale(5f);
        room.scale(1f, 2f, 1f);
        room.setLocalTranslation(0f, 0f, -4f);
        rootNode.attachChild(room);

        skull = assetManager.loadModel("Models/ReducedSkull.obj");
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");

        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.White);
        mat.setColor("Diffuse", ColorRGBA.White);
        skull.setMaterial(mat);
        skull.setLocalTranslation(0.0f, 0.0f, 0.0f);
        skull.scale(0.5f);
        selectable.attachChild(skull);

        PointLight lamp = new PointLight();
        lamp.setPosition(new Vector3f(0.0f, 2.5f, 4.0f));
        lamp.setColor(ColorRGBA.Gray);
        lamp.setRadius(100f);
        rootNode.addLight(lamp);

        DirectionalLight zedLight = new DirectionalLight(
                new Vector3f(0.0f, -0.5f, -1.0f));
        zedLight.setColor(ColorRGBA.DarkGray);
        rootNode.addLight(zedLight);

        AmbientLight sun = new AmbientLight();
        sun.setColor(ColorRGBA.DarkGray);
        rootNode.addLight(sun);

        cam.setLocation(new Vector3f(0.0f, 0.0f, 17.0f));
        flyCam.setEnabled(false);

        inputManager.addMapping("select", new TouchTrigger(TouchInput.ALL));
        inputManager.addListener(touchListener, new String[]{"select"});
    }


    @Override
    public void simpleUpdate(float tpf) {
        skull.rotate(0f, tpf, 0f);

    }


    private Vector3f lastpos;
    private Geometry objHit;
    private Material lastMat;

    private TouchListener touchListener = new TouchListener() {
        @Override
        public void onTouch(String name, TouchEvent event, float tpf) {
            if(event.getType() != TouchEvent.Type.IDLE){

                Vector2f inCoo = new Vector2f();
                inCoo.setX(event.getX());
                inCoo.setY(event.getY());
                Vector3f origin = cam.getWorldCoordinates(inCoo, 0.0f).clone();
                Vector3f dir = cam.getWorldCoordinates(inCoo, 0.3f).clone();
                Vector3f pickDir = dir.subtract(origin);
                Ray ray = new Ray(cam.getLocation(), pickDir);

                switch (event.getType()) {
                    case DOWN:
                        CollisionResults results = new CollisionResults();

                        selectable.collideWith(ray, results);
                        Log.d(TAG, "Selected: " + results.size() + " things");
                        if (results.size() > 0) {
                            CollisionResult closest = results.getClosestCollision();

                            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                            mat.setBoolean("UseMaterialColors", true);
                            mat.setColor("Ambient", ColorRGBA.Blue);
                            mat.setColor("Diffuse", ColorRGBA.Blue);
                            objHit = closest.getGeometry();
                            lastMat = objHit.getMaterial();
                            lastpos = objHit.getLocalTranslation().clone();
                            closest.getGeometry().setMaterial(mat);
                        }
                        break;

                    case MOVE:
                        Vector3f zpos = new Vector3f();
                        if(ray.intersectsWherePlane(ZPlane, zpos) && objHit != null){
                            objHit.setLocalTranslation(zpos);
                        }

                        break;

                    case UP:
                        if (objHit != null) {
                            objHit.setMaterial(lastMat);
                            objHit = null;
                        }
                        lastpos = origin;
                        break;

                    case FLING:
                        break;
                    case TAP:
                        break;
                    }
            }
        }

    };
}
