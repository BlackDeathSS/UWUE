package code.game.world.entities;

import code.math.Ray;
import code.math.RayCast;
import code.math.Vector3D;

/**
 *
 * @author Roman Lahin
 */
public class CubeEntity extends Entity {
    
    public Vector3D size = new Vector3D();
    
    public CubeEntity(float sizex, float sizey, float sizez) {
        size.set(sizex, sizey, sizez);
    }
    
    public boolean rayCast(Ray ray, boolean onlyMeshes) {
        if(onlyMeshes) return false;
        
        return RayCast.cubeRayCast(ray, 
                pos.x-size.x/2, pos.y-size.y/2, pos.z-size.z/2, 
                pos.x+size.x/2, pos.y+size.y/2, pos.z+size.z/2);
    }
    
    public boolean inRadius(Vector3D start) {
        float xx = (start.x - pos.x);
        xx = Math.max(0, xx*xx - size.x*size.x/4);
        
        float yy = (start.y - pos.y);
        yy = Math.max(0, yy*yy - size.y*size.y/4);
        
        float zz = (start.z - pos.z);
        zz = Math.max(0, zz*zz - size.z*size.z/4);
        
        return (xx+yy+zz) < activateRadius*activateRadius;
    }

}
