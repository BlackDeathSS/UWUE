package code.game.world.entities;

import code.engine3d.E3D;

import code.game.Main;
import code.game.scripting.Scripting;
import code.game.world.Node;
import code.game.world.World;

import code.math.collision.Ray;
import code.math.collision.Sphere;
import code.math.MathUtils;
import code.math.Vector3D;

import org.luaj.vm2.LuaValue;

/**
 *
 * @author Roman Lahin
 */
public class Entity {
    
    //World-entity stuff
    public Vector3D pos = new Vector3D();
	public Node node;
    
    public String name, unicalID;
    
    public void destroy() {
        pos = null;
        name = unicalID = null;
        activateWhen = onActivate = onFail = null;
    }
    
    public void update(World world) {}
	public Vector3D getMin() {return new Vector3D(pos);}
	public Vector3D getMax() {return new Vector3D(pos);}
    public void physicsUpdate(World world) {}
    public void collisionTest(Entity entity) {}
    public boolean rayCast(Ray ray, boolean onlyMeshes) {return false;}
    public boolean meshSphereCast(Sphere sphere) {return false;}
    //spherecast is only for mesh objects
    //todo this physics system s**** i should rewrite it
    
    public void animate(long step, boolean paused, Entity teteAtete) {}
    public void render(E3D e3d, World world) {}
    
    //Scripting stuff
    public LuaValue activateWhen;
    public LuaValue onActivate, onFail;
    
    public boolean activable;
    public float activateRadius = 250;
    public boolean clickable = true;
    public boolean pointable = true;
    
    public boolean animateWhenPaused = false;
    
	public boolean activateWhenInside = false;
    protected boolean inRadius = true;
    
    protected boolean activateImpl(Main main) {
        if(onActivate != null) {
            Scripting.runScript(onActivate);
            return true;
        }
        
        return false;
    }
    
    protected boolean failImpl(Main main) {
        if(onFail != null) {
            Scripting.runScript(onFail);
            return true;
        }
        
        return false;
    }

    public boolean canBeActivated(Entity lookingAt, Ray ray, boolean click) {
        if(!activable) return false;
		if(clickable && !click) return false;
        
        boolean oldInRadius = inRadius;
        inRadius = inRadius(ray.start);
        
        //in radius and old in radius was false
        //or it doesnt need to check this radius thing because activation is called by click
        if(inRadius && (activateWhenInside || clickable || !oldInRadius) && (!pointable || lookingAt == this)) {
            return true;
        }
        
        return false;
    }

    public boolean activate(Main main) {
        boolean succesRun = activateWhen == null || Scripting.runScript(activateWhen).toboolean();

        if(succesRun) return activateImpl(main);
        else return failImpl(main);
    }
    
    protected boolean inRadius(Vector3D start) {
        return start.distanceSqr(pos) <= activateRadius*activateRadius;
    }
    
	//todo make this right way
    protected static boolean rayCastSphere(Ray ray, Vector3D pos, float radius, float height) {
		Vector3D rayStart = new Vector3D(ray.start);
		rayStart.sub(pos);
		rayStart.mul(1, 2 * radius / height, 1);
		rayStart.add(pos);
		
		Vector3D rayDir = new Vector3D(ray.dir);
		rayDir.mul(1, 2 * radius / height, 1);
		
        float distSphere = MathUtils.distanceToRay(pos, rayStart, rayDir);
        if(distSphere > radius*radius) return false;
        
		rayDir.setLength((float) Math.max(0, rayStart.distance(pos) - radius));
		rayDir.div(1, 2 * radius / height, 1);
		
        float dist = rayDir.lengthSquared();
        
        if(dist < ray.dir.lengthSquared() && dist < ray.distance*ray.distance) {
            ray.distance = (float) Math.sqrt(dist);
            ray.mesh = null;
            return true;
        }
        
        return false;
    }

}
