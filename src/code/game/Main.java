package code.game;

import code.engine.Engine;
import code.engine.Screen;

import code.audio.AudioEngine;
import code.audio.SoundSource;

import code.engine3d.E3D;

import code.game.world.entities.Player;

import code.ui.TextBox;

import code.game.scripting.Scripting;
import code.utils.assetManager.AssetManager;
import code.utils.FPS;
import code.utils.IniFile;
import code.utils.Keys;
import code.utils.StringTools;
import code.utils.font.BMFont;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;

import org.lwjgl.glfw.GLFW;

/**
 *
 * @author Roman Lahin
 */
public class Main extends Screen {
    
    //bilding
    static int TILDE, ERASE;
    private static Main main;

    public IniFile gamecfg;
    public Configuration conf;
    
    public E3D e3d;
    public SoundSource musPlayer;
    public Globals lua;
    public LuaTable luasave;
    
    BMFont font;
    int fontColor, fontSelColor;
    SoundSource selectedS, clickedS, gameStartS;

    private boolean run = true;
    private Screen screen, nextScreen;
    private boolean needToDestroyScreen;
    
    private TextBox textBox;
    private TextBox console;
    
    public static void main(String[] args) {
        int sizes[] = Engine.init();
        main = new Main();
        Engine.setListener(main);
        
        main.gamecfg = AssetManager.loadIni("game.ini", true);
        main.conf = new Configuration(sizes[0], sizes[1]);
        
        Keys.UP = Keys.addKeyToBinding(Keys.UP, GLFW.GLFW_KEY_UP);
        Keys.DOWN = Keys.addKeyToBinding(Keys.DOWN, GLFW.GLFW_KEY_DOWN);
        Keys.LEFT = Keys.addKeyToBinding(Keys.LEFT, GLFW.GLFW_KEY_LEFT);
        Keys.RIGHT = Keys.addKeyToBinding(Keys.RIGHT, GLFW.GLFW_KEY_RIGHT);
        Keys.OK = Keys.addKeyToBinding(Keys.OK, GLFW.GLFW_KEY_ENTER);
        Keys.ESC = Keys.addKeyToBinding(Keys.ESC, GLFW.GLFW_KEY_ESCAPE);
        
        Player.initKeys(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_D, 
                GLFW.GLFW_KEY_SPACE, GLFW.GLFW_KEY_LEFT_SHIFT,
                GLFW.GLFW_KEY_E);
        
        Main.TILDE = Keys.addKeyToBinding(Main.TILDE, GLFW.GLFW_KEY_GRAVE_ACCENT);
        Main.ERASE = Keys.addKeyToBinding(Main.ERASE, GLFW.GLFW_KEY_BACKSPACE);
        
        int w = main.conf.startInFullscr? main.conf.fw:main.conf.ww;
        int h = main.conf.startInFullscr? main.conf.fh:main.conf.wh;
        
        Engine.createGLWindow(main.conf.startInFullscr, w, h, main.conf.vsync, main.conf.aa);
        Engine.setTitle(main.gamecfg.get("game", "name"));
        AudioEngine.init();
        AudioEngine.soundTypesVolume = new int[]{100, 100, 100};
        main.conf.apply(false);
        
        main.init();
    }

    private void init() {
        musPlayer = ((SoundSource) SoundSource.get().lock()).beMusicPlayer();
        musPlayer.setSoundType(Configuration.MUSIC);
        
        selectedS = (SoundSource) SoundSource.get("/sounds/select.ogg").lock();
        selectedS.set3D(false);
        selectedS.buffer.neverUnload = true;
        
        clickedS = (SoundSource) SoundSource.get("/sounds/click.ogg").lock();
        clickedS.set3D(false);
        clickedS.buffer.neverUnload = true;
        
        gameStartS = (SoundSource) SoundSource.get("/sounds/game start.ogg").lock();
        gameStartS.set3D(false);
        gameStartS.buffer.neverUnload = true;
        
        font = BMFont.loadFont(gamecfg.get("hud", "font"));
        font.setInterpolation(false);
        setFontScale(getHeight());
        
        fontColor = StringTools.getRGB(gamecfg.getDef("hud", "font_color", "255,255,255"), ',');
        fontSelColor = StringTools.getRGB(gamecfg.getDef("hud", "font_selected_color", "221,136,149"), ',');
        
        e3d = new E3D();
        
        clearLua();
        
        console = new TextBox(this, font) {
            public void onCancel() {
                super.onCancel();
                text = "";
            }
            
            public void onEnter() {
                super.onEnter();

                LuaValue val = Scripting.runScript(main, text);
                if(!val.isnil()) {
                    System.out.println("bool " + val.toboolean());
                    System.out.println("int " + val.toint());
                    System.out.println("num " + val.todouble());
                    System.out.println("str " + val.tojstring());
                }

                text = "";
            }
        }.setXYW(0, 0, getWidth());

        setScreen(new Menu(this));
        run();
    }

    public void destroy() {
        if(screen != null) screen.destroy();
        
        Scripting.save(luasave);
        
        e3d.destroy();
        font.destroy();
        
        AssetManager.destroyThings(AssetManager.ALL);
        
        Engine.destroy();
        AudioEngine.close();
    }
    
    void clearLua() {
        lua = JsePlatform.standardGlobals();
        lua.set("save", luasave==null?(luasave = Scripting.load(this)):luasave);
        Scripting.initFunctions(this);
    }

    void stop() {
        run = false;
        nextScreen = null;
    }

    private void run() {
        while(run) {
            //Change screen to next screen
            if(nextScreen != null) {
                if(screen != null && needToDestroyScreen) {
                    needToDestroyScreen = false;
                    screen.destroy();
                }

                screen = nextScreen;
                nextScreen = null;
                screen.show();
            }
            
            FPS.frame();

            if(screen != null) {
                screen.tick();
            }
            
            if(textBox == console) {
                if(!e3d.mode2D) e3d.prepare2D(0, 0, getWidth(), getHeight());
                console.draw(e3d, false, 0);
            }
            
            Engine.flush();

            if(!conf.vsync) try {
                Thread.sleep(Math.max(1, 8 - (System.currentTimeMillis() - FPS.previousFrame)));
                //max 125 fps (todo: add support of 144hz monitors??)
            } catch (Exception e) {}
            
            if(userTryingToCloseApp()) {
                stop();
            }
        }

        destroy();
    }

    public void setScreen(Screen screen) {
        setScreen(screen, false);
    }

    public void setScreen(Screen screen, boolean needToDestroy) {
        nextScreen = screen;
        needToDestroyScreen = needToDestroy;
    }

    public Screen getScreen() {
        return screen;
    }

    public Game getGame() {
        if(screen instanceof Game) return (Game)screen;
        else if(screen instanceof DialogScreen) return ((DialogScreen) screen).game;
        else return null;
    }
    
    public void openTextBox(Object textBox) {
        this.textBox = (TextBox) textBox;
        this.textBox.open();
    }
    
    public void closeTextBox() {
        if(textBox != null) {
            textBox.selected = false;
            textBox = null;
        }
    }

    public TextBox getTextBox() {
        return textBox;
    }
    
    public void charInput(int codepoint) {
        if(textBox != null) {
            char[] chrs = Character.toChars(codepoint);
            textBox.addChars(chrs);
        }
    }

    public void keyPressed(int key) {
        if(key == GLFW.GLFW_KEY_F11) {
            boolean fullscr = !Engine.isFullscr();
            Engine.setWindow(fullscr, fullscr ? conf.fw : conf.ww, fullscr ? conf.fh : conf.wh, conf.vsync);
            return;
        } else if(key == GLFW.GLFW_KEY_F2) {
            Engine.takeScreenshot();
            return;
        }
        
        if(textBox != null && Keys.isThatBinding(key, Keys.OK)) {
            textBox.onEnter();
            return;
        } else if(textBox != null && Keys.isThatBinding(key, Keys.ESC)) {
            textBox.onCancel();
            return;
        } else if(textBox != null && Keys.isThatBinding(key, ERASE)) {
            textBox.erase();
            return;
        }
        
        if(textBox != null) return;
        
        Keys.keyPressed(key);
        if(screen != null) screen.keyPressed(key);
    }
    
    public void keyRepeated(int key) {
        if(textBox != null) {
            if(Keys.isThatBinding(key, ERASE)) {
                textBox.erase();
            }
            return;
        }
        
        if(screen != null) screen.keyRepeated(key);
    }

    public void keyReleased(int key) {
        Keys.keyReleased(key);
        
        if((textBox == console || textBox == null) && Keys.isThatBinding(key, TILDE)) {
            if(textBox == null) openTextBox(console);
            else console.onCancel();
            
            return;
        } 
        
        if(textBox != null) return;
        
        if(screen != null) screen.keyReleased(key);
    }

    public void mouseAction(int button, boolean pressed) {
        if(!pressed && textBox != null && textBox != console) {
            if(!textBox.isInBox(getMouseX(), getMouseY())) textBox.onMouseUnfocus();
            //Don't return okay?
        } 
        
        if(screen != null) screen.mouseAction(button, pressed);
    }

    public void mouseScroll(double xoffset, double yoffset) {
        if(screen != null) screen.mouseScroll(xoffset, yoffset);
    }
    
    float scrollSpeed() {
        return font.getHeight()/2f;
    }

    public void sizeChanged(int w, int h, Screen scr) {
        setFontScale(h);
        console.setXYW(0, 0, w);
        
        if(screen != null) screen.sizeChanged(w, h, scr);
    }
    
    private void setFontScale(int h) {
        font.baseScale = Math.max(1, Math.round(h * 2 / 768f));
    }

}