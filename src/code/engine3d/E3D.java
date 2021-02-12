package code.engine3d;

import code.Engine;
import static code.engine3d.Renderable.NORMALDRAW;
import code.math.Vector3D;
import code.utils.font.BMFont;
import java.util.Vector;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;

/**
 *
 * @author Roman Lahin
 */
public class E3D {
    
    public float fovX, fovY;
    public int w, h;
    
    Vector<Renderable> postDraw;
    
    Vector<Vector<Renderable>>[] toDraw;
    Vector<Integer>[] toDrawOffset;
    int[] toDrawUsed;
    
    int rectCoordVBO, rectuvVBO, rectuvMVBO, windowColVBO, arrowVBO, cubeVBO, rectNormals;
    
    public boolean mode2D;
    public int maxLights;
    
    public E3D() {
        toDraw = new Vector[]{new Vector(), new Vector()};
        toDrawOffset = new Vector[]{new Vector(), new Vector()};
        toDrawUsed = new int[2];
        postDraw = new Vector();
        
        rectCoordVBO = GL15.glGenBuffers(); //Creates a VBO ID
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectCoordVBO); //Loads the current VBO to store the data
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 
                new short[]{
                    0, 0, 0, 1, 0, 0,
                    1, 1, 0, 0, 1, 0
                }, GL15.GL_STATIC_DRAW);
        
        rectuvVBO = GL15.glGenBuffers(); //Creates a VBO ID
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectuvVBO); //Loads the current VBO to store the data
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 
                new short[]{
                    0, 0, 1, 0,
                    1, 1, 0, 1
                }, GL15.GL_STATIC_DRAW);
        
        rectNormals = GL15.glGenBuffers(); //Creates a VBO ID
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectNormals); //Loads the current VBO to store the data
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 
                new short[]{
                    0, 0, 1, 0, 0, 1,
                    0, 0, 1, 0, 0, 1
                }, GL15.GL_STATIC_DRAW);
        
        arrowVBO = GL15.glGenBuffers(); //Creates a VBO ID
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrowVBO); //Loads the current VBO to store the data
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 
                new short[]{
                    -1, -1, 0, 1, 0, 0,
                    -1, 1, 0,
                }, GL15.GL_STATIC_DRAW);
        
        rectuvMVBO = GL15.glGenBuffers(); //Creates a VBO ID
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectuvMVBO); //Loads the current VBO to store the data
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 
                new short[]{
                    0, 1, 1, 1,
                    1, 0, 0, 0
                }, GL15.GL_STATIC_DRAW);
        
        windowColVBO = GL15.glGenBuffers(); //Creates a VBO ID
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, windowColVBO); //Loads the current VBO to store the data
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 
                new float[]{
                    0, 0, 0, 0.75f, 0, 0, 0, 0.75f,
                    0, 0, 0, 0, 0, 0, 0, 0
                }, GL15.GL_STATIC_DRAW);
        
        cubeVBO = GL15.glGenBuffers(); //Creates a VBO ID
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, cubeVBO); //Loads the current VBO to store the data
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, 
                new short[]{
                    0, 0, 0, 0, 0, 1,
                    0, 0, 0, 0, 1, 0,
                    0, 0, 0, 1, 0, 0,
                    
                    1, 1, 1, 1, 1, 0,
                    1, 1, 1, 1, 0, 1,
                    1, 1, 1, 0, 1, 1,
                    
                    1, 0, 1, 1, 0, 0,
                    1, 0, 1, 0, 0, 1,
                    
                    0, 1, 0, 1, 1, 0,
                    0, 1, 0, 0, 1, 1,
                    
                    0, 0, 1, 0, 1, 1,
                    1, 0, 0, 1, 1, 0,
                }, GL15.GL_STATIC_DRAW);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); //Unloads the current VBO when done.
        
        GL11.glLightModelfv(GL11.GL_LIGHT_MODEL_AMBIENT, new float[]{1,1,1,1});
        GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, 1);
        GL11.glMaterialfv(GL11.GL_FRONT, GL11.GL_DIFFUSE, new float[]{1,1,1,1});
        maxLights = GL11.glGetInteger(GL11.GL_MAX_LIGHTS);
        
        for(int i=0; i<maxLights; i++) {
            GL11.glLightf(GL11.GL_LIGHT0+i, GL11.GL_CONSTANT_ATTENUATION, 0);
            GL11.glLightf(GL11.GL_LIGHT0+i, GL11.GL_QUADRATIC_ATTENUATION, 0.0001F * 0.1f);
        }
        
        Shader shader = new Shader("/shaders/dither");
    }
    
    public void destroy() {
        GL15.glDeleteBuffers(rectCoordVBO);
        GL15.glDeleteBuffers(rectuvVBO);
        GL15.glDeleteBuffers(rectuvMVBO);
        GL15.glDeleteBuffers(rectNormals);
        GL15.glDeleteBuffers(windowColVBO);
        GL15.glDeleteBuffers(arrowVBO);
        GL15.glDeleteBuffers(cubeVBO);
        
        LightGroup.clear(false);
    }
    
    public Matrix4f cam = new Matrix4f(), invCam = new Matrix4f(), proj = new Matrix4f();
    public float[] invCamf = new float[16];
    public Matrix4f m = new Matrix4f();
    float[] tmp = new float[16];
    
    public void setCam(Vector3D camera, float rotX, float rotY, float fov, int w, int h) {
        this.w = w;
        this.h = h;
        
        cam.identity();
        cam.rotateY((float) Math.toRadians(rotY));
        cam.rotateX((float) Math.toRadians(rotX));
        cam.setTranslation(camera.x, camera.y, camera.z);
        
        invCam.set(cam);
        invCam.invert();
        invCam.get(invCamf);
        
        proj.identity();
        proj.perspective((float) Math.toRadians(fov), (float) w / h, 1f, 40000.0f);
        
        fovY = fov;
        fovX = (float)Math.toDegrees(2f*Math.atan((float) (Math.tan(Math.toRadians(fovY/2f)) * w / h)));
    }
    
    public void disableFog() {
        GL11.glDisable(GL11.GL_FOG);
    }
    
    public void setLinearFog(float start, float end, float[] color) {
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogfv(GL11.GL_FOG_COLOR, color);

        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_LINEAR);
        GL11.glFogf(GL11.GL_FOG_START, start);
        GL11.glFogf(GL11.GL_FOG_END, end);
    }
    
    public void setExpFog(float density, float[] color) {
        GL11.glEnable(GL11.GL_FOG);
        GL11.glFogfv(GL11.GL_FOG_COLOR, color);

        GL11.glFogi(GL11.GL_FOG_MODE, GL11.GL_EXP);
        GL11.glFogf(GL11.GL_FOG_DENSITY, density);
    }
    
    public void clearZbuffer() {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }
    
    public void clearColor(int color) {
        GL11.glClearColor(((color>>16)&255) / 255f, 
                ((color>>8)&255) / 255f, 
                (color&255) / 255f, 1);
        
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }
    
    public void proj() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadMatrixf(proj.get(tmp));
    }
    
    private void ortho(int w, int h) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadMatrixf(m.identity().ortho(0, w, h, 0, 0, 40000).get(tmp));
    }
    
    public void prepare2D(int xx, int yy, int ww, int hh) {
        GL11.glViewport(xx, yy, ww, hh);
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        
        ortho(ww, hh);
        
        mode2D = true;
    }
    
    public void prepareRender(int xx, int yy, int ww, int hh) {
        GL11.glViewport(xx, yy, ww, hh);
        
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glCullFace(GL11.GL_BACK);
    }
    
    public void add(Renderable obj) {
        if(obj.drawOrder <= NORMALDRAW) {
            Vector<Vector<Renderable>> toDraw = this.toDraw[obj.drawOrder];
            Vector<Integer> toDrawOffset = this.toDrawOffset[obj.drawOrder];
            int usedLists = toDrawUsed[obj.drawOrder];
            int orderOffset = obj.orderOffset;
            
            int vec = 0;
            for(; vec<usedLists; vec++) {
                if(toDrawOffset.elementAt(vec).intValue() == orderOffset) break;
            }
            
            if(vec == usedLists) {
                if(usedLists == toDraw.size()) {
                    toDraw.add(new Vector());
                    toDrawOffset.add(orderOffset);
                } else toDrawOffset.set(vec, orderOffset);
                
                toDrawUsed[obj.drawOrder]++;
            }
            
            toDraw.elementAt(vec).add(obj);
        } else postDraw.add(obj);
    }
    
    public void renderVectors() {
        //Finally draw 3d
        for(int i=0; i<=NORMALDRAW; i++) {
            Vector<Vector<Renderable>> toDraw = this.toDraw[i];
            int usedLists = toDrawUsed[i];
            sort(toDraw, toDrawOffset[i], usedLists);
            
            for(int x=0; x<usedLists; x++) {
                Vector<Renderable> toRender = toDraw.elementAt(x);
                
                for(Renderable object : toRender) object.render(this);
                
                toRender.removeAllElements();
            }
            
            toDrawUsed[i] = 0;
        }
        
        sort(postDraw);
        for(Renderable object : postDraw) object.render(this);
        
        postDraw.removeAllElements();
    }
    
    public void flush() {
        GLFW.glfwSwapBuffers(Engine.window);
        GLFW.glfwPollEvents();
    }
    
    public void drawRect(Material mat, float x, float y, float w, float h, 
            int color, float a) {
        drawRect(mat, x, y, w, h, 
                ((color>>16)&255) / 255f,
                ((color>>8)&255) / 255f,
                (color&255) / 255f,
                a);
    }
    
    public void drawRect(Material mat, float x, float y, float w, float h, 
            float r, float g, float b, float a) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(w, h, 0);
        
        if(mat != null) GL15.glActiveTexture(GL15.GL_TEXTURE0);
        
        if(r != 1 || g != 1 || b != 1 || a != 1) GL11.glColor4f(r, g, b, a);
        
        if(mat != null) mat.bind();
        else {
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }
        
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectCoordVBO);
        GL11.glVertexPointer(3, GL11.GL_SHORT, 0, 0);
        
        if(mat != null) {
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectuvVBO);
            GL11.glTexCoordPointer(2, GL11.GL_SHORT, 0, 0);
        }
        
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
        
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        if(mat != null) {
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            mat.unbind();
        } else {
            GL11.glDisable(GL11.GL_BLEND);
        }
        
        if(r != 1 || g != 1 || b != 1 || a != 1) GL11.glColor4f(1, 1, 1, 1);
        
        GL11.glPopMatrix();
    }
    
    public void drawDitheredSurface(Texture frameBuffer, Texture dither, float x, float y, float w, float h,
            Shader ditherShader, int ditherW, int ditherH) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(w, h, 0);
        
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        
        frameBuffer.bind(false, false, true, 0);
        if(ditherShader.isCompiled()) dither.bind(false, false, false, 1);
        ditherShader.bind();
        ditherShader.setUniformf(ditherW, frameBuffer.w/dither.w);
        ditherShader.setUniformf(ditherH, frameBuffer.h/dither.h);
        
        GL15.glEnableClientState(GL15.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectCoordVBO);
        GL15.glVertexPointer(3, GL15.GL_SHORT, 0, 0);
        
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
        
        GL15.glDisableClientState(GL15.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        
        frameBuffer.unbind(0);
        dither.unbind(1);
        ditherShader.unbind();
        
        GL11.glPopMatrix();
    }
    
    public void drawWindow(float x, float y, float w, float h, BMFont font) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL15.glEnableClientState(GL15.GL_VERTEX_ARRAY);
        GL15.glEnableClientState(GL15.GL_COLOR_ARRAY);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, rectCoordVBO);
        GL15.glVertexPointer(3, GL15.GL_SHORT, 0, 0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, windowColVBO);
        GL15.glColorPointer(4, GL15.GL_FLOAT, 0, 0);
        
        GL11.glLoadIdentity();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(w, -getWindowYBorder(), 0);
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
        
        GL11.glLoadIdentity();
        GL11.glTranslatef(x, y+h, 0);
        GL11.glScalef(w, getWindowYBorder(), 0);
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
        
        GL15.glDisableClientState(GL15.GL_COLOR_ARRAY);
        GL11.glColor4f(0, 0, 0, 0.75f);
        
        GL11.glLoadIdentity();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(w, h, 0);
        GL11.glDrawArrays(GL11.GL_QUADS, 0, 4);
        
        GL15.glDisableClientState(GL15.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL11.glDisable(GL11.GL_BLEND);
        
        GL11.glColor4f(1, 1, 1, 1);
        
        GL11.glPopMatrix();
    }
    
    public void drawCube(Vector3D min, Vector3D max, int color, float a) {
        m.identity();
        m.translate(min.x, min.y, min.z);
        m.scale(max.x-min.x, max.y-min.y, max.z-min.z);
        invCam.mul(m);
        
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadMatrixf(invCam.get(tmp));
        invCam.set(invCamf);
        
        float r = ((color>>16)&255) / 255f;
        float g = ((color>>8)&255) / 255f;
        float b = (color&255) / 255f;
        
        if(r != 1 || g != 1 || b != 1 || a != 1) GL11.glColor4f(r, g, b, a);
        
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, cubeVBO);
        GL11.glVertexPointer(3, GL11.GL_SHORT, 0, 0);
        
        GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
        
        GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL11.glDisable(GL11.GL_BLEND);
        
        if(r != 1 || g != 1 || b != 1 || a != 1) GL11.glColor4f(1, 1, 1, 1);
        
        GL11.glPopMatrix();
    }
    
    public static int getWindowYBorder() {
        return 15;
    }
    
    public void drawArrow(float x, float y, float w, float h, float rot, int color, float a) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        
        m.identity();
        m.scale(w/2, h/2, 0);
        m.rotate((float)Math.toRadians(rot), 0, 0, 1);
        m.setTranslation(x, y, 0);
        
        GL11.glLoadMatrixf(m.get(tmp));
        
        if(color != 0xffffff || a != 1) GL11.glColor4f(((color>>16)&255) / 255f, 
                ((color>>8)&255) / 255f, 
                (color&255) / 255f, a);
        
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            
        GL15.glEnableClientState(GL15.GL_VERTEX_ARRAY);
        
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrowVBO);
        GL15.glVertexPointer(3, GL15.GL_SHORT, 0, 0);
        
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
        
        GL15.glDisableClientState(GL15.GL_VERTEX_ARRAY);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL11.glDisable(GL11.GL_BLEND);
        
        if(color != 0xffffff || a != 1) GL11.glColor4f(1, 1, 1, 1);
        
        GL11.glPopMatrix();
    }
    
    private static void sort(Vector<Renderable> list) {
        for(int i=list.size()-1; i>=1; i--) {
            Renderable nearest = null;
            int pos = 0;
            
            for(int x=0; x<=i; x++) {
                Renderable m1 = list.elementAt(x);
                
                //m1 ближе чем m2
                if(nearest == null || 
                        (m1.sortZ > nearest.sortZ && m1.orderOffset >= nearest.orderOffset) ||
                        m1.orderOffset > nearest.orderOffset) {
                    nearest = m1;
                    pos = x;
                }
            }
            
            list.setElementAt(list.elementAt(i), pos);
            list.setElementAt(nearest, i);
        }
    }
    
    private static void sort(Vector<Vector<Renderable>> list, Vector<Integer> offset, int length) {
        for(int i=length-1; i>=1; i--) {
            Vector<Renderable> max = null;
            int maximumOffset = Integer.MIN_VALUE;
            int maximumIndex = 0;
            
            for(int x=0; x<=i; x++) {
                int tmpOffset = offset.elementAt(x);
                
                if(tmpOffset > maximumOffset) {
                    max = list.elementAt(x);
                    maximumOffset = tmpOffset;
                    maximumIndex = x;
                }
            }
            
            list.setElementAt(list.elementAt(i), maximumIndex);
            list.setElementAt(max, i);
            
            offset.setElementAt(offset.elementAt(i), maximumIndex);
            offset.setElementAt(maximumOffset, i);
        }
    }
    
    boolean clipEnabled;
    float cx, cy, cw, ch;
    
    public void clip(float x, float y, float cw, float ch) {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadMatrixf(m.identity().get(tmp));

        //left clip
        double[] eq = new double[]{1,0,0,-x};
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, eq);
	GL11.glEnable(GL11.GL_CLIP_PLANE0);
        
        //right clip
        eq[0] = -1; eq[3] = x+cw;
        GL11.glClipPlane(GL11.GL_CLIP_PLANE1, eq);
	GL11.glEnable(GL11.GL_CLIP_PLANE1);
        
        //bottom clip
        eq[0] = 0; eq[1] = 1; eq[3] = -y;
        GL11.glClipPlane(GL11.GL_CLIP_PLANE2, eq);
	GL11.glEnable(GL11.GL_CLIP_PLANE2);
        
        //top clip
        eq[1] = -1; eq[3] = y+ch;
        GL11.glClipPlane(GL11.GL_CLIP_PLANE3, eq);
	GL11.glEnable(GL11.GL_CLIP_PLANE3);
        
        GL11.glPopMatrix();
        
        cx = x; cy = y;
        this.cw = cw; this.ch = ch;
        clipEnabled = true;
    }
    
    public Vector clipPlanes = new Vector();
    
    public void pushClip() {
        clipPlanes.add(new Float(cx));
        clipPlanes.add(new Float(cy));
        clipPlanes.add(new Float(cw));
        clipPlanes.add(new Float(ch));
        clipPlanes.add(new Boolean(clipEnabled));
    }
    
    public void popClip() {
        clipEnabled = (Boolean) clipPlanes.elementAt(clipPlanes.size()-1);
        ch = (Float) clipPlanes.elementAt(clipPlanes.size()-2);
        cw = (Float) clipPlanes.elementAt(clipPlanes.size()-3);
        cy = (Float) clipPlanes.elementAt(clipPlanes.size()-4);
        cx = (Float) clipPlanes.elementAt(clipPlanes.size()-5);
        
        clipPlanes.removeElementAt(clipPlanes.size()-1);
        clipPlanes.removeElementAt(clipPlanes.size()-1);
        clipPlanes.removeElementAt(clipPlanes.size()-1);
        clipPlanes.removeElementAt(clipPlanes.size()-1);
        clipPlanes.removeElementAt(clipPlanes.size()-1);
        
        if(clipEnabled) clip(cx, cy, cw, ch);
        else disableClip();
    }
    
    public void disableClip() {
	GL11.glDisable(GL11.GL_CLIP_PLANE0);
	GL11.glDisable(GL11.GL_CLIP_PLANE1);
	GL11.glDisable(GL11.GL_CLIP_PLANE2);
	GL11.glDisable(GL11.GL_CLIP_PLANE3);
        clipEnabled = false;
    }

}
