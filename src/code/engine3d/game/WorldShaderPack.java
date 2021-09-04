package code.engine3d.game;

import code.engine3d.E3D;
import code.engine3d.Shader;
import code.engine3d.ShaderPack;
import code.utils.assetManager.AssetManager;
import java.util.Arrays;

/**
 *
 * @author Roman Lahin
 */
public class WorldShaderPack extends ShaderPack {
    
    public int uvOffset, alphaThreshold;
    
    protected WorldShaderPack(E3D e3d, String path, String[][] defs) {
        super(e3d, path, defs);
        
        for(Shader shader : shaders) {
            shader.addUniformBlock(e3d.matrices, "mats");
            
            shader.bind();
            shader.addTextureUnit(0);
			shader.addUniformBlock(e3d.lights, "lights");
			shader.addUniformBlock(e3d.fog, "fog");
            shader.unbind();
        }
        
        shaders[0].bind();
        uvOffset = shaders[0].getUniformIndex("uvOffset");
        alphaThreshold = shaders[0].getUniformIndex("alphaThreshold");
        shaders[0].unbind();
    }
    
    public static WorldShaderPack get(E3D e3d, String path, String[][] defs) {
        String defsName = (defs != null) ? String.valueOf(Arrays.hashCode(defs)) : "";
        
        WorldShaderPack shaderPack = (WorldShaderPack) AssetManager.get("SHDRPCK_" + path + defsName);
        if(shaderPack != null) return shaderPack;
        
        shaderPack = new WorldShaderPack(e3d, path, defs);
        AssetManager.add("SHDRPCK_" + path + defsName, shaderPack);
        
        return shaderPack;
    }
    
    public void destroy() {
        super.destroy();
    }

}
