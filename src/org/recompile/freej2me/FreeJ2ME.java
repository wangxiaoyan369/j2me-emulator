/*
	This file is part of FreeJ2ME.

	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	FreeJ2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with FreeJ2ME.  If not, see http://www.gnu.org/licenses/
*/
package org.recompile.freej2me;

/*
	FreeJ2ME - AWT
*/

import org.recompile.mobile.Mobile;
import org.recompile.mobile.MobilePlatform;
import org.recompile.mobile.PlatformImage;

import javax.imageio.ImageIO;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class FreeJ2ME {
    public static void main(String[] args) {
        FreeJ2ME app = new FreeJ2ME(args);
    }

    private final Frame main;
    private int lcdWidth;
    private int lcdHeight;
    private int scaleFactor = 2;

    private final LCD lcd;

    private int xBorder;
    private int yBorder;

    private PlatformImage img;

    private final Config config;
    private boolean useNokiaControls = false;
    private boolean useSiemensControls = false;
    private boolean useMotorolaControls = false;
    private final boolean rotateDisplay = false;
    private int limitFPS = 0;

    private final boolean[] pressedKeys = new boolean[128];

    public FreeJ2ME(String[] args) {
        main = new Frame("FreeJ2ME");
        main.setSize(0, 0);
        main.setResizable(false);
        main.setBackground(new Color(0, 0, 64));

        try {
            InputStream resourceAsStream = main.getClass().getResourceAsStream("/org/recompile/icon.png");
            if (resourceAsStream != null) {
                main.setIconImage(ImageIO.read(resourceAsStream));
            }
        } catch (IOException ignored) {

        }

        main.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                main.dispose();
                System.exit(0);
            }
        });

        // Setup Device //

        lcdWidth = 240;
        lcdHeight = 320;

        // args 0游戏地址 1游戏宽度 2游戏高度 3缩放
        String jarfile = "";
        if (args.length >= 1) {
            jarfile = args[0];
        }
        if (args.length >= 3) {
            lcdWidth = Integer.parseInt(args[1]);
            lcdHeight = Integer.parseInt(args[2]);
        }
        if (args.length >= 4) {
            scaleFactor = Integer.parseInt(args[3]);
        }

        Mobile.setPlatform(new MobilePlatform(lcdWidth, lcdHeight));

        lcd = new LCD();
        lcd.setFocusable(true); //可获取焦点
        main.add(lcd);

        config = new Config();
        config.onChange = this::settingsChanged;

        Mobile.getPlatform().setPainter(() -> lcd.paint(lcd.getGraphics()));

        lcd.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                int keycode = e.getKeyCode();
                int mobiKey = getMobileKey(keycode);
                int mobiKeyN = (mobiKey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array

                switch (keycode) // Handle emulator control keys
                {
                    case KeyEvent.VK_PLUS:
                    case KeyEvent.VK_ADD:
                        scaleFactor++;
                        main.setSize(lcdWidth * scaleFactor + xBorder, lcdHeight * scaleFactor + yBorder);
                        break;
                    case KeyEvent.VK_MINUS:
                    case KeyEvent.VK_SUBTRACT:
                        if (scaleFactor > 1) {
                            scaleFactor--;
                            main.setSize(lcdWidth * scaleFactor + xBorder, lcdHeight * scaleFactor + yBorder);
                        }
                        break;
                    case KeyEvent.VK_C:
                        if (e.isControlDown()) {
                            ScreenShot.takeScreenshot(false);
                        }
                        break;
                }

                if (mobiKey == 0) { //Ignore events from keys not mapped to a phone keypad key
                    return;
                }

                if (config.isRunning) {
                    config.keyPressed(mobiKey);
                } else {
                    if (!pressedKeys[mobiKeyN]) {
                        //~ System.out.println("keyPressed:  " + Integer.toString(mobiKey));
                        Mobile.getPlatform().keyPressed(mobiKey);
                    } else {
                        //~ System.out.println("keyRepeated:  " + Integer.toString(mobiKey));
                        Mobile.getPlatform().keyRepeated(mobiKey);
                    }
                }
                pressedKeys[mobiKeyN] = true;

            }

            public void keyReleased(KeyEvent e) {
                int mobiKey = getMobileKey(e.getKeyCode());
                int mobiKeyN = (mobiKey + 64) & 0x7F; //Normalized value for indexing the pressedKeys array

                if (mobiKey == 0) { //Ignore events from keys not mapped to a phone keypad key
                    return;
                }

                pressedKeys[mobiKeyN] = false;

                if (config.isRunning) {
                    config.keyReleased(mobiKey);
                } else {
                    //~ System.out.println("keyReleased: " + Integer.toString(mobiKey));
                    Mobile.getPlatform().keyReleased(mobiKey);
                }
            }

            public void keyTyped(KeyEvent e) {
            }

        });

        lcd.addMouseListener(new MouseListener() {
            public void mousePressed(MouseEvent e) {
                int x = (int) ((e.getX() - lcd.cx) * lcd.scaleX);
                int y = (int) ((e.getY() - lcd.cy) * lcd.scaleY);
                Mobile.getPlatform().pointerPressed(x, y);
            }

            public void mouseReleased(MouseEvent e) {
                int x = (int) ((e.getX() - lcd.cx) * lcd.scaleX);
                int y = (int) ((e.getY() - lcd.cy) * lcd.scaleY);
                Mobile.getPlatform().pointerReleased(x, y);
            }

            public void mouseExited(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseClicked(MouseEvent e) {
            }

        });

        main.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                resize();
            }
        });

        main.setVisible(true);
        main.pack();

        resize();
        main.setSize(lcdWidth * scaleFactor + xBorder, lcdHeight * scaleFactor + yBorder);

        if (args.length < 1) {
            FileDialog t = new FileDialog(main, "Open JAR File", FileDialog.LOAD);
            t.setFilenameFilter((dir, name) -> name.toLowerCase().endsWith(".jar"));
            t.setVisible(true);
            jarfile = new File(t.getDirectory() + File.separator + t.getFile()).toURI().toString();
        } else {
            jarfile = new File(jarfile).toURI().toString();
        }

        if (Mobile.getPlatform().loadJar(jarfile)) {
            config.init();
            settingsChanged();

            Mobile.getPlatform().runJar();
        } else {
            System.out.println("Couldn't load jar...");
        }
    }

    private void settingsChanged() {
        int w = Integer.parseInt(config.settings.get("width"));
        int h = Integer.parseInt(config.settings.get("height"));

        limitFPS = Integer.parseInt(config.settings.get("fps"));
        if (limitFPS > 0) {
            limitFPS = 1000 / limitFPS;
        }

        String sound = config.settings.get("sound");
        Mobile.sound = false;
        if (sound.equals("on")) {
            Mobile.sound = true;
        }

        String phone = config.settings.get("phone");
        useNokiaControls = false;
        useSiemensControls = false;
        useMotorolaControls = false;
        Mobile.nokia = false;
        Mobile.siemens = false;
        Mobile.motorola = false;
        if (phone.equals("Nokia")) {
            Mobile.nokia = true;
            useNokiaControls = true;
        }
        if (phone.equals("Siemens")) {
            Mobile.siemens = true;
            useSiemensControls = true;
        }
        if (phone.equals("Motorola")) {
            Mobile.motorola = true;
            useMotorolaControls = true;
        }

        if (lcdWidth != w || lcdHeight != h) {
            lcdWidth = w;
            lcdHeight = h;

            Mobile.getPlatform().resizeLCD(w, h);

            resize();
            main.setSize(lcdWidth * scaleFactor + xBorder, lcdHeight * scaleFactor + yBorder);
        }
    }

    private int getMobileKey(int keycode) {
        if (useNokiaControls) {
            switch (keycode) {
                case KeyEvent.VK_UP:
                    return Mobile.NOKIA_UP;
                case KeyEvent.VK_DOWN:
                    return Mobile.NOKIA_DOWN;
                case KeyEvent.VK_LEFT:
                    return Mobile.NOKIA_LEFT;
                case KeyEvent.VK_RIGHT:
                    return Mobile.NOKIA_RIGHT;
                case KeyEvent.VK_ENTER:
                    return Mobile.NOKIA_SOFT3;
            }
        }

        if (useSiemensControls) {
            switch (keycode) {
                case KeyEvent.VK_UP:
                    return Mobile.SIEMENS_UP;
                case KeyEvent.VK_DOWN:
                    return Mobile.SIEMENS_DOWN;
                case KeyEvent.VK_LEFT:
                    return Mobile.SIEMENS_LEFT;
                case KeyEvent.VK_RIGHT:
                    return Mobile.SIEMENS_RIGHT;
                case KeyEvent.VK_Q:
                    return Mobile.SIEMENS_SOFT1;
                case KeyEvent.VK_W:
                    return Mobile.SIEMENS_SOFT2;
                case KeyEvent.VK_ENTER:
                    return Mobile.SIEMENS_FIRE;
            }
        }

        if (useMotorolaControls) {
            switch (keycode) {
                case KeyEvent.VK_UP:
                    return Mobile.MOTOROLA_UP;
                case KeyEvent.VK_DOWN:
                    return Mobile.MOTOROLA_DOWN;
                case KeyEvent.VK_LEFT:
                    return Mobile.MOTOROLA_LEFT;
                case KeyEvent.VK_RIGHT:
                    return Mobile.MOTOROLA_RIGHT;
                case KeyEvent.VK_Q:
                    return Mobile.MOTOROLA_SOFT1;
                case KeyEvent.VK_W:
                    return Mobile.MOTOROLA_SOFT2;
                case KeyEvent.VK_ENTER:
                    return Mobile.MOTOROLA_FIRE;
            }
        }

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

            // Config //
            case KeyEvent.VK_ESCAPE:
                config.start();
        }
        return 0;
    }

    private void resize() {
        xBorder = main.getInsets().left + main.getInsets().right;
        yBorder = main.getInsets().top + main.getInsets().bottom;

        double vw = (main.getWidth() - xBorder);
        double vh = (main.getHeight() - yBorder);

        double nw = lcdWidth;
        double nh = lcdHeight;

        nw = vw;
        nh = nw * ((double) lcdHeight / (double) lcdWidth);

        if (nh > vh) {
            nh = vh;
            nw = nh * ((double) lcdWidth / (double) lcdHeight);
        }

        lcd.updateScale((int) nw, (int) nh);
    }

    private class LCD extends Canvas {
        public int cx = 0;
        public int cy = 0;
        public int cw = 240;
        public int ch = 320;

        public double scaleX = 1;
        public double scaleY = 1;

        public void updateScale(int vw, int vh) {
            cx = (this.getWidth() - vw) / 2;
            cy = (this.getHeight() - vh) / 2;
            cw = vw;
            ch = vh;
            scaleX = (double) lcdWidth / (double) vw;
            scaleY = (double) lcdHeight / (double) vh;
        }

        public void paint(Graphics g) {
            try {
                Graphics2D cgc = (Graphics2D) this.getGraphics();
                if (config.isRunning) {
                    g.drawImage(config.getLCD(), cx, cy, cw, ch, null);
                } else {
                    g.drawImage(Mobile.getPlatform().getLCD(), cx, cy, cw, ch, null);
                    if (limitFPS > 0) {
                        Thread.sleep(limitFPS);
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
