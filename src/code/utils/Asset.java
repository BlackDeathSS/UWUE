package code.utils;

import code.Engine;
import code.audio.SoundBuffer;
import code.audio.SoundSource;
import code.engine3d.Material;
import code.engine3d.Texture;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Vector;
import org.lwjgl.opengl.GL15;

/**
 *
 * @author Roman Lahin
 */
public class Asset {
    
    static Vector<Integer> vbos = new Vector();
    static Vector<DisposableContent> disposable = new Vector();
    static Hashtable<String, ReusableContent> reusable = new Hashtable();
    
    public static void free() {
        Enumeration<ReusableContent> els = reusable.elements();
        while(els.hasMoreElements()) {
            els.nextElement().using = false;
        }
    }
    
    private static void destroyDisposable(boolean destroyEverything) {
        for(Integer vbo : vbos) {
            GL15.glDeleteBuffers(vbo.intValue());
        }
        
        for(int i=0; i<disposable.size(); i++) {
            DisposableContent content = disposable.elementAt(i);
            
            if(!content.neverUnload || destroyEverything) {
                content.destroy();
                disposable.removeElementAt(i);
                i--;
            }
        }
    }
    
    public static final int REUSABLE = 1, DISPOSABLE = 2, NONFREE = 4, LOCKED = 8,
            ALL = REUSABLE | DISPOSABLE | NONFREE | LOCKED,
            ALL_EXCEPT_LOCKED = ALL & (~LOCKED);
    
    public static void destroyThings(int mask) {
        boolean destroyNonFree = (mask&NONFREE) == NONFREE;
        boolean destroyLocked = (mask&LOCKED) == LOCKED;
        
        if((mask&DISPOSABLE) == DISPOSABLE) destroyDisposable(destroyLocked);
        
        if((mask&REUSABLE) == REUSABLE) {
            Enumeration<String> keys = reusable.keys();
            Enumeration<ReusableContent> els = reusable.elements();

            while(keys.hasMoreElements()) {
                String key = keys.nextElement();
                ReusableContent el = els.nextElement();

                if(((!el.using || destroyNonFree) && !el.neverUnload) || destroyLocked) {
                    el.destroy();
                    reusable.remove(key);
                }
            }
        }
        
        System.gc();
    }
    
    public static Material getMaterial(String name, 
            Hashtable<String,String> replace, String prefix, String postfix) {
        String[] lines = StringTools.cutOnStrings(name, ';');
        IniFile stuff = new IniFile(lines, false);
        
        String path = lines[0];
        
        if(replace != null && replace.get(path) != null) {
            path = replace.get(path);
        } else if(prefix != null || postfix != null) {
            //Trenchbroom handling
            StringBuffer sb = new StringBuffer();
            
            if(prefix != null) sb.append(prefix);
            sb.append(path);
            if(postfix != null) sb.append(postfix);
            path = sb.toString();
        }
        
        Texture tex = getTexture(path);
        Material mat = new Material(tex);
        
        mat.load(stuff);
        
        return mat;
    }
    
    public static Material getMaterial(String name) {
        return getMaterial(name, null, null, null);
    }
    
    public static Texture getTexture(String name) {
        Texture tex = (Texture)reusable.get("TEX_" + name);
        if(tex != null) {
            tex.using = true;
            return tex;
        }
        
        if(name.equals("null")) {
            tex = new Texture(0);
            tex.neverUnload = true;
            reusable.put("TEX_" + name, tex);
            return tex;
        }
        
        tex = Texture.createTexture(name);
        
        if(tex != null) {
            reusable.put("TEX_" + name, tex);
            return tex;
        }
        
        return getTexture("null");
    }
    
    public static SoundBuffer getSoundBuffer(String file) {
        SoundBuffer sound = (SoundBuffer)reusable.get("SOUNDBUFF_" + file);
        if(sound != null) {
            sound.using = true;
            return sound;
        }
        
        sound = SoundBuffer.createBuffer(file);
        if(sound != null) {
            reusable.put("SOUNDBUFF_" + file, sound);
            return sound;
        }
        
        return null;
    }
    
    public static SoundSource getSoundSource() {
        return getSoundSource(null);
    }
    
    public static SoundSource getSoundSource(String file) {
        SoundSource source = file==null?new SoundSource():new SoundSource(file);
        
        disposable.add(source);
        return source;
    }
    
    public static String[] loadLines(String path) {
        File f = new File("data", path);
        
        Vector<String> lines = new Vector();
        try {
            Scanner sn = new Scanner(f, "UTF-8");
            sn.useDelimiter("\n");
            
            while(sn.hasNext()) {
                String str = sn.next().trim();
                if(str.length() > 0) lines.add(str);
            }
            
            sn.close();
        } catch(Exception e) {
            System.out.println("Can't load "+f.getAbsolutePath());
            Engine.printError(e);
        }
        
        return lines.toArray(new String[lines.size()]);
    }

    public static IniFile loadIni(String path, boolean sections) {
        String[] lines = loadLines(path);
        
        IniFile ini = new IniFile(new Hashtable());
        if(lines != null) ini.set(lines, sections);
        
        return ini;
    }

}
