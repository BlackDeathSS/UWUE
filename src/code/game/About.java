package code.game;

import code.engine.Screen;
import code.ui.TextView;
import code.utils.assetManager.AssetManager;
import code.utils.Keys;

/**
 *
 * @author Roman Lahin
 */
public class About extends Screen {
    
    private Main main;
    private Menu menu;
    
    private String loadedText;
    private TextView text;
    
    About(Main main, Menu menu) {
        this.main = main;
        this.menu = menu;
        
        loadedText = AssetManager.loadString("about.txt");
        
        text = new TextView(main.getWidth(), main.getHeight(), main.font);
        text.setHCenter(true);
        text.setVCenter(true);
        setText();
    }
    
    private void setText() {
        text.setText(loadedText, '\n');
    }
    
    public void destroy() {
        menu.destroy();
    }
    
    public void sizeChanged(int w, int h, Screen scr) {
        text.setSize(main.getWidth(), main.getHeight());
        setText();
        menu.sizeChanged(w, h, this);
    }
    
    public void tick() {
        menu.drawBackground();
        main.hudRender.drawRect(0, 0, main.getWidth(), main.getHeight(), 0, 0.5f);
        text.draw(main.hudRender, 0, 0, main.fontColor);
        
        step();
    }

    private void step() {
        if(Keys.isPressed(Keys.DOWN)) text.scroll(-3);
        if(Keys.isPressed(Keys.UP)) text.scroll(3);
    }
    
    public void keyPressed(int key) {
        if(Keys.isThatBinding(key, Keys.ESC)) {
            main.clickedS.play();
            main.setScreen(menu);
        }
    }
    
    public void mouseAction(int key, boolean pressed) {
        if(key == Screen.MOUSE_LEFT && !pressed) {
            main.clickedS.play();
            main.setScreen(menu);
        }
    }
    
    public void mouseScroll(double xx, double yy) {
        text.scroll((int) (yy*main.scrollSpeed()));
    }

}
