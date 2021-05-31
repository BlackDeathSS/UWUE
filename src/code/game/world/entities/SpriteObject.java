package code.game.world.entities;

import code.engine3d.E3D;
import code.engine3d.instancing.Sprite;

import code.game.world.World;

import code.engine3d.collision.Ray;
import code.math.Vector3D;

import code.utils.FPS;

/**
 *
 * @author Roman Lahin
 */
public class SpriteObject extends Entity {
    
    private Sprite spr;
    public boolean visible = true;
    
    public SpriteObject(Sprite spr) {
        this.spr = spr;
    }
    
    public void destroy() {
        spr.destroy();
        spr = null;
        super.destroy();
    }
    
    public boolean rayCast(Ray ray, boolean onlyMeshes) {
        if(onlyMeshes) return false;
        
        Vector3D tmp = new Vector3D(pos);
        tmp.add(0, spr.h*(spr.align+1)/2, 0);
        
        return Entity.rayCastSphere(ray, tmp, spr.w/2);
    }
    
    public void update(World world) {
        animate(FPS.frameTime, false, null);
    }
    
    public void animate(long step, boolean paused, Entity teteAtete) {
        if(!paused || animateWhenPaused || teteAtete == this) spr.animate(step, false);
    }
    
    public void render(E3D e3d, World world) {
        if(visible) {
            spr.setTransformation(pos, null);
            spr.setCamera(world.m, e3d.invCam);
            spr.render(e3d);
        }
    }

}
