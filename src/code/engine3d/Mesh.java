package code.engine3d;

import code.math.Vector3D;
import code.utils.IniFile;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

/**
 *
 * @author Roman Lahin
 */
public class Mesh extends Renderable {
    
    public String name;
    public float[] drawMatrix = new float[16];

    private Vector3D omin, omax;
    public Vector3D min, max;
    public Vector3D middle = new Vector3D();
    
    public int[] vertsID, uvsID, normals, vertsCount;
    public Material[] mats;
    
    public float[][] physicsVerts;
    public float[][] normalsPerFace;

    public Mesh() {}
    
    public Mesh(int[] verts, int[] uvs, int[] normals, int[] vertex, 
            Material[] mats, Vector3D min, Vector3D max) {
        set(verts, uvs, normals, vertex, mats, min, max);
    }
    
    public void set(int[] verts, int[] uvs, int[] normals, int[] vertex, 
            Material[] mats, Vector3D min, Vector3D max) {
        this.vertsID = verts;
        this.uvsID = uvs;
        this.normals = normals;
        this.vertsCount = vertex;
        this.mats = mats;
        
        this.omin = new Vector3D(min);
        this.omax = new Vector3D(max);
        this.min = min;
        this.max = max;
    }
    
    public void setPhysics(float[][] xyz) {
        physicsVerts = xyz;
        normalsPerFace = new float[xyz.length][];
        
        for(int i=0; i<xyz.length; i++) {
            float[] verts = xyz[i];
            normalsPerFace[i] = new float[verts.length * 3 / 9];
            float[] nrms = normalsPerFace[i];
            
            for(int t=0; t<verts.length; t+=3*3) {
                float cx = verts[t], cy = verts[t+1], cz = verts[t+2];
                float bx = verts[t+3], by = verts[t+4], bz = verts[t+5];
                float ax = verts[t+6], ay = verts[t+7], az = verts[t+8];
                
                float x = (ay - by) * (az - cz) - (az - bz) * (ay - cy);
                float y = (az - bz) * (ax - cx) - (ax - bx) * (az - cz);
                float z = (ax - bx) * (ay - cy) - (ay - by) * (ax - cx);
                
                double sqrt = Math.sqrt(x * x + y * y + z * z);
                
                nrms[t / 3] = (float) (x / sqrt);
                nrms[t / 3 + 1] = (float) (y / sqrt);
                nrms[t / 3 + 2] = (float) (z / sqrt);
            }
        }
    }
    
    public void setMatrix(float[] invcam) {
        System.arraycopy(invcam, 0, drawMatrix, 0, 16);
        
        min.set(omin); max.set(omax);
        
        updateZ();
    }
    
    public void setMatrix(Vector3D pos, Vector3D rot, Matrix4f tmp, Matrix4f invCam) {
        tmp.identity();
        if(rot != null) {
            tmp.rotateX((float) Math.toRadians(rot.x));
            tmp.rotateY((float) Math.toRadians(rot.y));
            tmp.rotateZ((float) Math.toRadians(rot.z));
        }
        tmp.setTranslation(pos.x, pos.y, pos.z);
        
        updateBB(tmp.get(drawMatrix));
        
        Matrix4f tmp2 = new Matrix4f(invCam);
        tmp2.mul(tmp);
        tmp2.get(drawMatrix);
        
        updateZ();
    }
    
    private void updateBB(float[] mat) {
        Vector3D v000 = new Vector3D(omin.x, omin.y, omin.z);
        Vector3D v001 = new Vector3D(omin.x, omin.y, omax.z);
        Vector3D v010 = new Vector3D(omin.x, omax.y, omin.z);
        Vector3D v011 = new Vector3D(omin.x, omax.y, omax.z);
        
        Vector3D v100 = new Vector3D(omax.x, omin.y, omin.z);
        Vector3D v101 = new Vector3D(omax.x, omin.y, omax.z);
        Vector3D v110 = new Vector3D(omax.x, omax.y, omin.z);
        Vector3D v111 = new Vector3D(omax.x, omax.y, omax.z);
        
        v000.transform(mat); v001.transform(mat); v010.transform(mat); v011.transform(mat);
        v100.transform(mat); v101.transform(mat); v110.transform(mat); v111.transform(mat);
        
        float mx = Math.min(v000.x, v001.x);
        mx = Math.min(mx, Math.min(v010.x, v011.x));
        
        float my = Math.min(v000.y, v001.y);
        my = Math.min(my, Math.min(v010.y, v011.y));
        
        float mz = Math.min(v000.z, v001.z);
        mz = Math.min(mz, Math.min(v010.z, v011.z));
        
        min.set(mx, my, mz);
        
        mx = Math.max(v000.x, v001.x);
        mx = Math.max(mx, Math.min(v010.x, v011.x));
        
        my = Math.max(v000.y, v001.y);
        my = Math.max(my, Math.min(v010.y, v011.y));
        
        mz = Math.max(v000.z, v001.z);
        mz = Math.max(mz, Math.min(v010.z, v011.z));
        
        max.set(mx, my, mz);
    }
    
    private void updateZ() {
        middle.set(min);
        middle.add(max.x, max.y, max.z);
        middle.mul(0.5f, 0.5f, 0.5f);
        middle.transform(drawMatrix);
    }
    
    public float getZ() {
        return middle.z;
    }
    
    public void render(E3D e3d) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadMatrixf(drawMatrix);
            
        GL15.glEnableClientState(GL15.GL_VERTEX_ARRAY);
        GL15.glEnableClientState(GL15.GL_TEXTURE_COORD_ARRAY);
        GL15.glEnableClientState(GL15.GL_NORMAL_ARRAY);
        GL15.glActiveTexture(GL15.GL_TEXTURE0);

        for(int submesh = 0; submesh < mats.length; submesh++) {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertsID[submesh]);
            GL15.glVertexPointer(3, GL15.GL_FLOAT, 0, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, uvsID[submesh]);
            GL15.glTexCoordPointer(2, GL15.GL_FLOAT, 0, 0);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, normals[submesh]);
            GL15.glNormalPointer(GL15.GL_FLOAT, 0, 0);

            mats[submesh].bind();
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertsCount[submesh]);
        }

        GL15.glDisableClientState(GL15.GL_VERTEX_ARRAY);
        GL15.glDisableClientState(GL15.GL_TEXTURE_COORD_ARRAY);
        GL15.glDisableClientState(GL15.GL_NORMAL_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
