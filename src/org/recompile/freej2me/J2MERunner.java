package org.recompile.freej2me;

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

/**
 * @author: 屈寒山
 * @date: 2022/5/25 - 16:10
 */
public class J2MERunner {

    private final int width = 240;
    private final int height = 320;
    private int scale = 1;
    private int titleHeight = 0;

    private final JFrame frame;
    private final JPanel screen;

    private static final boolean[] pressedKeys = new boolean[128];

    public static void main(String[] args) {
        new J2MERunner(args);
    }

    public J2MERunner(String[] args) {
        Mobile.setPlatform(new MobilePlatform(width, height));
        String jarFile = new File(args[0]).toURI().toString();

        frame = new JFrame();
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        screen = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                g.drawImage(Mobile.getPlatform().getLCD(), 0, 0, width * scale, height * scale, null);
            }
        };
        screen.setPreferredSize(new Dimension(width * scale, height * scale));
        frame.add(screen);
        frame.pack();
        frame.setVisible(true);
        titleHeight = (int) (frame.getSize().getHeight() - screen.getSize().getHeight());
        Mobile.getPlatform().setPainter(screen::repaint);

        if (Mobile.getPlatform().loadJar(jarFile)) {
            Mobile.getPlatform().runJar();
            System.out.println("Running...");
        } else {
            System.out.println("Run failed");
        }
        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
//                System.out.println(e.getKeyCode() + ":" + Integer.toHexString(e.getKeyCode()));
                int keycode = e.getKeyCode();
                int mobiKey = getMobileKey(keycode);
                int mobiKeyN = (mobiKey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array

                switch (keycode) // Handle emulator control keys
                {
                    case KeyEvent.VK_EQUALS:
                        scale++;
                        updateScale(scale);
                        break;
                    case KeyEvent.VK_MINUS:
                        scale--;
                        updateScale(scale);
                    case KeyEvent.VK_C:
                        if (e.isControlDown()) {
                            ScreenShot.takeScreenshot(false);
                        }
                        break;
                    case KeyEvent.VK_Z:
                        if(e.isControlDown()) {
//                            Mobile.getPlatform().destroyJar();
                        }
                        break;
                }

                if (mobiKey == 0) { //Ignore events from keys not mapped to a phone keypad key
                    return;
                }

                if (!pressedKeys[mobiKeyN]) {
                    //~ System.out.println("keyPressed:  " + Integer.toString(mobiKey));
                    Mobile.getPlatform().keyPressed(mobiKey);
                } else {
                    //~ System.out.println("keyRepeated:  " + Integer.toString(mobiKey));
                    Mobile.getPlatform().keyRepeated(mobiKey);
                }

                pressedKeys[mobiKeyN] = true;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int mobiKey = getMobileKey(e.getKeyCode());
                int mobiKeyN = (mobiKey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array

                if (mobiKey == 0) { //Ignore events from keys not mapped to a phone keypad key
                    return;
                }

                pressedKeys[mobiKeyN] = false;
                Mobile.getPlatform().keyReleased(mobiKey);
            }
        });
    }

    private void updateScale(int scale) {
        this.scale = scale;
        if (this.scale > 5) {
            this.scale = 5;
        }
        if (this.scale < 1) {
            this.scale = 1;
        }
        screen.setSize(new Dimension(width * this.scale, height * this.scale));
        frame.setSize(width * this.scale, height * this.scale + this.titleHeight);
    }

    private int getMobileKey(int keycode) {
        switch (keycode) {
            case KeyEvent.VK_0:
                return Mobile.KEY_NUM0;
            case KeyEvent.VK_1:
                return Mobile.KEY_NUM1;
            case KeyEvent.VK_2:
                return Mobile.KEY_NUM2;
            case KeyEvent.VK_3:
                return Mobile.KEY_NUM3;
            case KeyEvent.VK_4:
                return Mobile.KEY_NUM4;
            case KeyEvent.VK_5:
                return Mobile.KEY_NUM5;
            case KeyEvent.VK_6:
                return Mobile.KEY_NUM6;
            case KeyEvent.VK_7:
                return Mobile.KEY_NUM7;
            case KeyEvent.VK_8:
                return Mobile.KEY_NUM8;
            case KeyEvent.VK_9:
                return Mobile.KEY_NUM9;
            case KeyEvent.VK_ASTERISK:
                return Mobile.KEY_STAR;
            case KeyEvent.VK_NUMBER_SIGN:
                return Mobile.KEY_POUND;

            case KeyEvent.VK_NUMPAD0:
                return Mobile.KEY_NUM0;
            case KeyEvent.VK_NUMPAD7:
                return Mobile.KEY_NUM1;
            case KeyEvent.VK_NUMPAD8:
                return Mobile.KEY_NUM2;
            case KeyEvent.VK_NUMPAD9:
                return Mobile.KEY_NUM3;
            case KeyEvent.VK_NUMPAD4:
                return Mobile.KEY_NUM4;
            case KeyEvent.VK_NUMPAD5:
                return Mobile.KEY_NUM5;
            case KeyEvent.VK_NUMPAD6:
                return Mobile.KEY_NUM6;
            case KeyEvent.VK_NUMPAD1:
                return Mobile.KEY_NUM7;
            case KeyEvent.VK_NUMPAD2:
                return Mobile.KEY_NUM8;
            case KeyEvent.VK_NUMPAD3:
                return Mobile.KEY_NUM9;

            case KeyEvent.VK_UP:
                return Mobile.KEY_NUM2;
            case KeyEvent.VK_DOWN:
                return Mobile.KEY_NUM8;
            case KeyEvent.VK_LEFT:
                return Mobile.KEY_NUM4;
            case KeyEvent.VK_RIGHT:
                return Mobile.KEY_NUM6;

            case KeyEvent.VK_ENTER:
                return Mobile.KEY_NUM5;

            case KeyEvent.VK_Q:
                return Mobile.NOKIA_SOFT1;
            case KeyEvent.VK_W:
                return Mobile.NOKIA_SOFT2;
            case KeyEvent.VK_E:
                return Mobile.KEY_STAR;
            case KeyEvent.VK_R:
                return Mobile.KEY_POUND;

            case KeyEvent.VK_A:
                return -1;
            case KeyEvent.VK_Z:
                return -2;
        }
        return 0;
    }
}
