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
// should probably clarify that what remains of the template is the manifest,
// the jmeFragment, the mainActivity and the logger line in this file
public class Main extends SimpleApplication implements PhysicsCollisionListener{
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    //all objects with corresponding physics bodies
    private Node box;
    private RigidBodyControl skull_phy;
    private Spatial room;
    private RigidBodyControl room_phy;
    private Node fish;
    private RigidBodyControl fish_phy;

    //main node and physic state
    private Node selectable;
    private BulletAppState bulletAppState;

    //threshold for when objects should break, might be individual later on
    private static final float threshold = 3.0f;

    //helper vectors and plane for things i use often
    private static final Plane ZPlane = new Plane(new Vector3f(0.0f,0.0f,1.0f), 0);
    private static final Vector3f gravity = new Vector3f(0.0f, -20.0f,0.0f);
    private static final Vector3f linearFactor = new Vector3f(1.0f, 1.0f, 0.0f);


    @Override
    public void simpleInitApp() {

        //Initialize the settings for the GLView
        setDisplayFps(false);
        setDisplayStatView(false);
        //Initialize physics state
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        //Attach a node for sorting selectable models from everything else
        selectable = new Node("Selectable");
        rootNode.attachChild(selectable);

        //helper functions for organizing the loading of models and lights
        setupLight();
        setupModels();

        cam.setLocation(new Vector3f(0.0f, 0.0f, 20.0f));
        //disable flycam to disable unwanted control options
        flyCam.setEnabled(false);

        //add mapping from touching the screen to object selection
        inputManager.addMapping("select", new TouchTrigger(TouchInput.ALL));
        inputManager.addListener(touchListener, new String[]{"select"});
        //add a physics listener for shattering effects
        bulletAppState.getPhysicsSpace().addCollisionListener(this);
    }

    private void setupLight(){
        //pretty self-explanatory

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
        //sets up the models, should probably have a general function for this

        //room geometry
        //the room is an actual model, not a skybox, just to simplify collisions with the walls
        room = assetManager.loadModel("Models/CrashBox.obj");
        //placeholder texture until I fix something cooler
        Material wallMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        wallMat.setBoolean("UseMaterialColors", true);
        wallMat.setColor("Ambient", ColorRGBA.White);
        wallMat.setColor("Diffuse", ColorRGBA.White);
        room.setMaterial(wallMat);

        //rotate the room properly, probably exported the model wrong
        room.rotate((float) Math.PI / 2, 0f, 0f);
        //resize the room to fill the screen
        room.scale(5f);
        room.scale(1f, 2f, 1f);
        room.setLocalTranslation(0f, 0f, -4f);
        //attach to rootnode to make it unselectable and visible.
        // Equivalent to drawing in main graphics update with z-buffer and backface culling enabled.
        rootNode.attachChild(room);

        //Room physics, simple boxes for collision
        // mass = 0 => immovable object
        room_phy = new RigidBodyControl(0);
        room.addControl(room_phy);
        bulletAppState.getPhysicsSpace().add(room_phy);

        //Add a fish to throw
        //the texture for the fish should be loaded with the model but for some reason it doesn't
        //TODO investigate why texture doesn't load with model
        fish = (Node) assetManager.loadModel("Models/fish.fbx");
        fish.setLocalScale(0.02f);
        fish.setLocalTranslation(6f, -2f, 0f);
        //fish is projectile and should not break
        fish.setUserData("isBreakable", false);

        //add physics to the fish
        fish_phy = new RigidBodyControl(0.5f);
        fish.addControl(fish_phy);
        fish_phy.setGravity(gravity);
        fish_phy.setPhysicsLocation(fish.getLocalTranslation());
        fish_phy.setLinearFactor(linearFactor);
        bulletAppState.getPhysicsSpace().add(fish_phy);
        selectable.attachChild(fish);

        //because I want to handle all objects similarly in the picking function
        // all objects need the same structure
        for (Geometry child : getGeometries(fish)) {
            child.setUserData("parentPart", fish.getName().toString());
        }


        //box geometry, no texture yet
        // should add some granite-looking texture to hide the cracks
        // or figure out how to layer models
        box = (Node) assetManager.loadModel("Models/crashingCube.fbx");
        box.setLocalScale(1.5f);
        box.setLocalTranslation(-5f, -3f, 0f);
        box.setUserData("isBreakable", true);
        String parentPart = box.getName().toString();
        Log.d(TAG, box.getName());

        //box physics
        skull_phy = new RigidBodyControl(1.0f);
        box.addControl(skull_phy);
        skull_phy.setGravity(gravity);
        skull_phy.setPhysicsLocation(box.getLocalTranslation());
        skull_phy.setLinearFactor(linearFactor);
        bulletAppState.getPhysicsSpace().add(skull_phy);

        for (Geometry child : getGeometries(box)) {
            //set collisionshapes for the fractures but don't enable them
            //this way we can keep track of all the fracture positions while they are inactive
            //this is of course a performance loss, but so far it seems to impact the performance less
            //than trying to create the shapes when impact occurs.

            //from what I've seen on the subject, trying to procedurally fracture objects on time of impact
            // severely impacts performance, so it's better to do it this way.
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

        bulletAppState.getPhysicsSpace().addAll(box);
        //selectable.attachChild(box);
        rootNode.attachChild(box);

    }


    @Override
    public void simpleUpdate(float tpf) {

        //Nothing so far, haven't figured it out but
        // I think this is where forces should be applied after an impact

    }


    private Geometry objHit;
    //private Material lastMat;
    private Vector3f startVel = new Vector3f(0f, 0f, 0f);

    private TouchListener touchListener = new TouchListener() {
        @Override
        public void onTouch(String name, TouchEvent event, float tpf) {
            if(event.getType() != TouchEvent.Type.IDLE){

                //get touch coordinates on screen
                Vector2f inCoo = new Vector2f();
                inCoo.setX(event.getX());
                inCoo.setY(event.getY());

                //project to world coordinates
                Vector3f origin = cam.getWorldCoordinates(inCoo, 0.0f).clone();
                Vector3f dir = cam.getWorldCoordinates(inCoo, 0.3f).clone();
                //ray cast in the direction from cam position to projected touch coordinates
                Vector3f pickDir = dir.subtract(origin);
                Ray ray = new Ray(cam.getLocation(), pickDir);

                switch (event.getType()) {
                    case DOWN:
                        //check if the ray collided with any object on screen that is selectable
                        CollisionResults results = new CollisionResults();
                        selectable.collideWith(ray, results);
                        Log.d(TAG, "Selected: " + results.size() + " things");

                        if (results.size() > 0) {
                            //get the collided object that is closest to the screen
                            //this way what you pick is what you get
                            CollisionResult closest = results.getClosestCollision();

                            //if something was selected, temporarily turn it blue
/*                            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                            mat.setBoolean("UseMaterialColors", true);
                            mat.setColor("Ambient", ColorRGBA.Blue);
                            mat.setColor("Diffuse", ColorRGBA.Blue);*/

                            //get the geometry of the collided object, eg the package containing the model
                            objHit = closest.getGeometry();
                            //if we turn it blue, save material for when we release it
                            //lastMat = objHit.getMaterial();
                            //closest.getGeometry().setMaterial(mat);

                            //check for where we touched the screen corresponds to the z = 0 -plane
                            //save for setting velocity
                            ray.intersectsWherePlane(ZPlane, startVel);
                            }
                        break;

                    case MOVE:
                        //draw an arrow from object to where we are touching?
                        break;

                    case UP:
                        Vector3f endVel = new Vector3f();
                        if(ray.intersectsWherePlane(ZPlane, endVel) && objHit != null) {
                            //set velocity to scale with where we release
                            Vector3f velocity = endVel.subtract(startVel);
                            //for some reason this feels more natural
                            velocity.setY(velocity.y*1.3f);
                            velocity.setZ(0f);
                            velocity = velocity.mult(3f);

                            //set a max speed
                            if(velocity.length() > 20f){
                                velocity = velocity.normalize().mult(20f);
                            }

                            //check if what we are throwing is a fragment or a node
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

    //from jme3 contributor Dokthar:
    // https://hub.jmonkeyengine.org/t/spatial-get-sub-mesh/34593/6
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
    @Override
    public void collision(PhysicsCollisionEvent event) {
        //get objects involved
        Spatial A = event.getNodeA();
        Spatial B = event.getNodeB();
        //applied impulse corresponds to the magnitude of impact between A and B
        float appliedImpulse = event.getAppliedImpulse();
        if (appliedImpulse > 0.5f){
            Log.d(TAG, "impulse: " + appliedImpulse);
        }

        //need to do the check twice because there is no knowing which obect is A or B
        if (A instanceof Node) {
            //if it isn't a Node it definetly isn't breakable
            if (A.getUserData("isBreakable")) {
                //if it is a Node, it might still just be a grouping
                if (((Node) A).getQuantity() > 0) {
                    //not sure if this check is really needed but better safe than sorry

                    //check if impact was greater than required threshold for breaking
                    if (appliedImpulse > threshold) {

                        //Get all children of A
                        List<Geometry> childrenSource = getGeometries(A);
                        A.getControl(RigidBodyControl.class).setEnabled(false);

                        //for now, fracture the whole thing
                        // later fracture only the parts closest to impact

                        //need to figure out an algorithm for choosing what parts to fracture and
                        // how to make them fly away from the impact
                        for (Geometry child : childrenSource) {
                            RigidBodyControl childPhy = child.getControl(RigidBodyControl.class);
                            childPhy.setEnabled(true);


                            //this just made the whole thing explode violently
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


