package org.jmonkeyengine.simple_jme_android.gamelogic;

import android.util.Log;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.TouchInput;
import com.jme3.input.controls.TouchListener;
import com.jme3.input.controls.TouchTrigger;
import com.jme3.input.event.TouchEvent;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Plane;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import static android.content.ContentValues.TAG;

/**
 * Basic template created by potterec on 3/17/2016.
 * Edited by Olgitt from 24/4/2018
 */
public class Main extends SimpleApplication implements PhysicsCollisionListener{
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private Node skull;
    private RigidBodyControl skull_phy;
    private Spatial room;
    private RigidBodyControl room_phy;
    private Node fish;
    private RigidBodyControl fish_phy;
    private Node selectable;
    private BulletAppState bulletAppState;

    private static final float threshold = 2.0f;
    private static final Plane ZPlane = new Plane(new Vector3f(0.0f,0.0f,1.0f), 0);
    private static final Vector3f gravity = new Vector3f(0.0f, -20.0f,0.0f);
    private static final Vector3f linearFactor = new Vector3f(1.0f, 1.0f, 0.0f);


    @Override
    public void simpleInitApp() {
        setDisplayFps(false);
        setDisplayStatView(false);
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);

        selectable = new Node("Selectable");
        rootNode.attachChild(selectable);

        setupLight();
        setupModels();

        cam.setLocation(new Vector3f(0.0f, 0.0f, 20.0f));
        flyCam.setEnabled(false);

        inputManager.addMapping("select", new TouchTrigger(TouchInput.ALL));
        inputManager.addListener(touchListener, new String[]{"select"});
        bulletAppState.getPhysicsSpace().addCollisionListener(this);
    }

    private void setupLight(){
        PointLight lamp = new PointLight();
        lamp.setPosition(new Vector3f(0.0f, 2.5f, 4.0f));
        lamp.setColor(ColorRGBA.Gray);
        lamp.setRadius(50f);
        rootNode.addLight(lamp);


        DirectionalLight zedLight = new DirectionalLight(
                new Vector3f(0.0f, -0.5f, -1.0f));
        zedLight.setColor(ColorRGBA.DarkGray);
        rootNode.addLight(zedLight);


        AmbientLight sun = new AmbientLight();
        sun.setColor(ColorRGBA.DarkGray);
        rootNode.addLight(sun);
    }

    private void setupModels() {

        //room geometry
        room = assetManager.loadModel("Models/CrashBox.obj");
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

        //Room physics
        room_phy = new RigidBodyControl(0);
        room.addControl(room_phy);
        room.setShadowMode(RenderQueue.ShadowMode.Cast);
        bulletAppState.getPhysicsSpace().add(room_phy);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Ambient", ColorRGBA.Blue);
        mat.setColor("Diffuse", ColorRGBA.Blue);

        //Add a fish to throw
        fish = (Node) assetManager.loadModel("Models/fish.fbx");
        fish.setLocalScale(0.02f);
        fish.setLocalTranslation(6f, -2f, 0f);
        fish.setUserData("isBreakable", false);

        //add physics to the fish
        fish_phy = new RigidBodyControl(0.5f);
        fish.addControl(fish_phy);
        fish_phy.setGravity(gravity);
        fish_phy.setPhysicsLocation(fish.getLocalTranslation());
        fish_phy.setLinearFactor(linearFactor);
        bulletAppState.getPhysicsSpace().add(fish_phy);
        selectable.attachChild(fish);

        for (Geometry child : getGeometries(fish)) {
            child.setUserData("parentPart", fish.getName().toString());
        }


        //Skull geometry, for now skull = box
        skull = (Node) assetManager.loadModel("Models/crashingCube.fbx");
        skull.setLocalScale(1.5f);
        skull.setLocalTranslation(-5f, -3f, 0f);
        skull.setUserData("isBreakable", true);
        String parentPart = skull.getName().toString();
        Log.d(TAG, skull.getName());

        //Skull physics
        skull_phy = new RigidBodyControl(1.0f);
        skull.addControl(skull_phy);
        skull_phy.setGravity(gravity);
        skull_phy.setPhysicsLocation(skull.getLocalTranslation());
        skull_phy.setLinearFactor(linearFactor);
        bulletAppState.getPhysicsSpace().add(skull_phy);

        for (Geometry child : getGeometries(skull)) {
            CollisionShape childShape = new HullCollisionShape(child.getMesh());
            RigidBodyControl childPhy = new RigidBodyControl(childShape, 1/ 46f);
            child.addControl(childPhy);
            child.setUserData("parentPart", parentPart);
            childPhy.setLinearFactor(linearFactor);
            childPhy.setGravity(gravity);
            childPhy.setPhysicsLocation(child.getLocalTranslation());
            childPhy.setEnabled(false);
            bulletAppState.getPhysicsSpace().add(childPhy);
        }

        bulletAppState.getPhysicsSpace().addAll(skull);
        //selectable.attachChild(skull);
        rootNode.attachChild(skull);

    }


    @Override
    public void simpleUpdate(float tpf) {

    }


    private Geometry objHit;
    //private Material lastMat;
    private Vector3f startVel = new Vector3f(0f, 0f, 0f);

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

/*                            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                            mat.setBoolean("UseMaterialColors", true);
                            mat.setColor("Ambient", ColorRGBA.Blue);
                            mat.setColor("Diffuse", ColorRGBA.Blue);*/


                            objHit = closest.getGeometry();
                            //lastMat = objHit.getMaterial();

                            ray.intersectsWherePlane(ZPlane, startVel);
                            //closest.getGeometry().setMaterial(mat);
                        }
                        break;

                    case MOVE:
                        break;

                    case UP:
                        Vector3f endVel = new Vector3f();
                        if(ray.intersectsWherePlane(ZPlane, endVel) && objHit != null) {
                            Vector3f velocity = endVel.subtract(startVel);
                            //for some reason this feels more natural
                            velocity.setY(velocity.y*1.3f);
                            velocity.setZ(0f);
                            velocity = velocity.mult(3f);
                            if(velocity.length() > 20f){
                                velocity = velocity.normalize().mult(20f);
                            }

                            //if the thing is Not part of a bigger object, throw it
                            //Otherwise, throw the bigger object.
                            Spatial parentPart = selectable.getChild((String) objHit.getUserData("parentPart"));

                            if (objHit.getControl(RigidBodyControl.class) != null) {
                                objHit.getControl(RigidBodyControl.class).setLinearVelocity(velocity);
                            } else if (parentPart.getControl(RigidBodyControl.class).isEnabled()){
                                parentPart.getControl(RigidBodyControl.class).setLinearVelocity(velocity);
                            }

                            //objHit.setMaterial(lastMat);
                            objHit = null;
                        }
                        break;

                    case TAP: case FLING: case HOVER_END:
                        break;
                    }
            }
        }

    };

    private List<Geometry> getGeometries(Spatial spatial) {
        final List<Geometry> geoms = new LinkedList<Geometry>();
        if (spatial instanceof Geometry) {
            geoms.add((Geometry)spatial);
        } else if (spatial instanceof Node) {
            Node node = (Node) spatial;
            node.depthFirstTraversal(new SceneGraphVisitorAdapter() {
                @Override
                public void visit(Geometry geom) {
                    geoms.add(geom);
                }
            });
        }
        return geoms;
    }


    //Check if either of the collided objects are breakable
    //If so, get the force of impact and calculate level of destruction
    //give a number of fracture-cells their own physics based on the level of destruction
    //Maybe apply an impulse to the cells
    @Override
    public void collision(PhysicsCollisionEvent event) {
        Spatial A = event.getNodeA();
        Spatial B = event.getNodeB();
        float appliedImpulse = event.getAppliedImpulse();
        if (appliedImpulse > 0.5f){
            Log.d(TAG, "impulse: " + appliedImpulse);
        }
        if (A instanceof Node) {
            if (A.getUserData("isBreakable")) {
                if (((Node) A).getQuantity() > 0) {
                    if (appliedImpulse > threshold) {

                        //Get all children of A
                        List<Geometry> childrenSource = getGeometries(A);
                        A.getControl(RigidBodyControl.class).setEnabled(false);

                        Vector3f impactPoint = event.getLocalPointA();


                        for (Geometry child : childrenSource) {
                            RigidBodyControl childPhy = child.getControl(RigidBodyControl.class);
                            childPhy.setEnabled(true);

/*                            Vector3f center = childPhy.getPhysicsLocation();
                            Vector3f impulse = center.subtract(impactPoint).normalize();
                            impulse.mult(event.getAppliedImpulse()*0.0000001f);
                            childPhy.applyImpulse(impulse, new Vector3f(0f,0f,0f));*/
                        }
                    }
                }
            }
        }

        if (B instanceof Node) {
            if (B.getUserData("isBreakable")) {
                if (((Node) B).getQuantity() > 0) {
                    if (appliedImpulse > threshold) {

                        List<Geometry> childrenSource = getGeometries(B);

                        B.getControl(RigidBodyControl.class).setEnabled(false);

                        Vector3f impactPoint = event.getLocalPointB();

                        for (Spatial child : childrenSource) {
                            RigidBodyControl childPhy = child.getControl(RigidBodyControl.class);
                            childPhy.setEnabled(true);

/*                            Vector3f center = childPhy.getPhysicsLocation();
                            Vector3f impulse = center.subtract(impactPoint).normalize();
                            impulse.mult(event.getAppliedImpulse()*0.0000001f);
                            childPhy.applyImpulse(impulse, new Vector3f(0f,0f,0f));*/
                        }
                    }
                }
            }
        }
    }

}


