package code.engine3d;

import code.math.Vector3D;
import code.utils.IniFile;
import org.joml.Matrix4f;

/**
 *
 * @author Roman Lahin
 */
public class Renderable {
    
    public static final int NORMALDRAW = 1, PREDRAW = 0, POSTDRAW = 2;
    
    public int drawOrder = NORMALDRAW, orderOffset;
    public float sortZ;
    
    public long time;
    
    public void load(IniFile ini) {
        String tmp = ini.get("order");
        
        if(tmp != null) {
            if(tmp.startsWith("pre")) {
                drawOrder = PREDRAW;
                if(tmp.length() > 3) orderOffset = Integer.valueOf(tmp.substring(3));
            } else if(tmp.startsWith("post")) {
                drawOrder = POSTDRAW;
                if(tmp.length() > 4) orderOffset = Integer.valueOf(tmp.substring(4));
            } else orderOffset = Integer.valueOf(tmp);
        }
    }

    public void setMatrix(float[] put) {}
    public void setMatrix(Vector3D pos, Vector3D rot, Matrix4f tmp, Matrix4f invCam) {}
    
    public void prepareRender(E3D e3d) {
        if(drawOrder == NORMALDRAW) e3d.toRender.add(this);
        else if(drawOrder == PREDRAW) e3d.preDraw.add(this);
        else e3d.postDraw.add(this);
    }
    
    public void animate(long time, boolean set) {
        if(set) this.time = time;
        else this.time += time;
    }
    public void render(E3D e3d) {}
    
    public static final Matrix4f tmpMat = new Matrix4f();
    
    public static Matrix4f buildMatrix(Vector3D pos, Vector3D rot) {
        return buildMatrix(pos, rot, new Matrix4f());
    }
    
    public static Matrix4f buildMatrix(Vector3D pos, Vector3D rot, Matrix4f tmp) {
        if(rot != null) {
            tmp.rotateX((float) Math.toRadians(rot.x));
            tmp.rotateY((float) Math.toRadians(rot.y));
            tmp.rotateZ((float) Math.toRadians(rot.z));
        }
        tmp.setTranslation(pos.x, pos.y, pos.z);
        
        return tmp;
    }
    
}
