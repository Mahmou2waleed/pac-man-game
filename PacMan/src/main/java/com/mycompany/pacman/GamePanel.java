package com.mycompany.pacman;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.RadialGradientPaint;
import java.awt.BasicStroke;

public class GamePanel extends JPanel implements KeyListener {
    private final GameEngine engine = new GameEngine();
    public boolean inMainMenu = true;
    public int selectedMenuItem = -1;
    private int desiredDirX = 0;
    private int desiredDirY = 0;

    public GamePanel() {
        setFocusable(true);
        addKeyListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(inMainMenu) {
                    handleMenuClick(e.getY());
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if(inMainMenu) {
                    updateMenuSelection(e.getY());
                    repaint();
                }
            }
        });
        
        new Timer(100, e -> {
            if(!inMainMenu && !engine.gameOver) {
                engine.movePacman(desiredDirX, desiredDirY);
                engine.moveGhosts();
                engine.checkCollisions();
                engine.updateAnimations();
                repaint();
            }
        }).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
        if(inMainMenu) drawMainMenu(g2);
        else if(engine.gameOver) drawGameOver(g2);
        else drawGame(g2);
    }

    // Keep original drawing methods
    private void drawGame(Graphics g) {
        drawMap((Graphics2D) g);
        drawPacman((Graphics2D) g);
        drawGhosts((Graphics2D) g);
        drawHUD(g);
    }

    private void drawMap(Graphics2D g2) {
        int S = Constants.TILE_SIZE;
        for (int y = 0; y < engine.map.length; y++) {
          for (int x = 0; x < engine.map[y].length; x++) {
            int px = x*S, py = y*S;

            // floor
            g2.setColor(new Color(10,10,30));
            g2.fillRect(px, py, S, S);

            switch(engine.map[y][x]) {
              case Constants.WALL:
                // 1) glow
                g2.setColor(new Color(0,120,255,60));
                g2.fillRoundRect(px-3, py-3, S+6, S+6, 8,8);
                // 2) stroke
                g2.setStroke(new BasicStroke(2));
                g2.setColor(new Color(0,180,255));
                g2.drawRoundRect(px+1, py+1, S-2, S-2, 6,6);
                break;
              case Constants.PELLET:
                drawPellet(g2, px+S/2f, py+S/2f, 3, new Color(255,255,255,200));
                break;
              case Constants.POWER_PELLET:
                drawPellet(g2, px+S/2f, py+S/2f, 6, new Color(255,255,200,220));
                break;
              case Constants.GHOST_BOX:
                g2.setColor(new Color(20,20,80,180));
                g2.fillRect(px,py,S,S);
                break;
            }
          }
        }
    }
    
    private void drawPellet(Graphics2D g2, float cx, float cy, float r, Color glow) {
        Point2D center = new Point2D.Float(cx,cy);
        float[] dist = {0f,1f};
        Color[] colors = { glow, new Color(glow.getRed(), glow.getGreen(), glow.getBlue(),0) };
        RadialGradientPaint p = new RadialGradientPaint(center, r, dist, colors);
        g2.setPaint(p);
        g2.fill(new Ellipse2D.Float(cx-r, cy-r, 2*r, 2*r));
    }

    private void drawPacman(Graphics2D g2) {
        int S = Constants.TILE_SIZE;    
        int px = engine.pacmanX * S;
        int py = engine.pacmanY * S;
        
        int direction = engine.getDirectionFromDelta(engine.dirX, engine.dirY);
        int baseAngle = 0;

        switch (direction) {
            case Constants.LEFT:
                baseAngle = 180;
                break;
            case Constants.RIGHT:
                baseAngle = 0;
                break;
            case Constants.UP:
                baseAngle = 90;
                break;
            case Constants.DOWN:
                baseAngle = 270;
                break;
        }

        int startAngle = (baseAngle + engine.mouthAngle) % 360;
        int extent = 360 - 2 * engine.mouthAngle;

        // 1) Soft halo glow behind Pac‑Man
        g2.setColor(new Color(255, 255,  0,  80));
        g2.fillOval(px - 4, py - 4, S + 8, S + 8);

        // 2) The crisp, yellow Pac‑Man on top
        g2.setColor(Color.YELLOW);
        g2.fillArc(px, py, S, S, startAngle, extent);
    }

    private void drawGhosts(Graphics2D g2) {
        int S = Constants.TILE_SIZE;
        Color[] ghostColors = { Color.RED, Color.PINK, Color.CYAN, Color.ORANGE };

        for (int i = 0; i < engine.ghosts.length; i++) {
            int[] ghost = engine.ghosts[i];
            int gx = ghost[0], gy = ghost[1], type = ghost[2];

            Color base;
            if (engine.powerMode) {
                base = ((engine.powerTimer / 10) % 2 == 0)
                     ? Color.BLUE
                     : Color.WHITE;
            } else {
                base = ghostColors[type];
            }

            int px = gx * S, py = gy * S;

            // 1) glow halo
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 80));
            g2.fillOval(px - 4, py - 4, S + 8, S + 8);

            // 2) body
            g2.setColor(base);
            g2.fillOval(px, py, S, S);
            g2.setColor(base.brighter());
            g2.fillArc(px, py, S, S, 0, 180);

            // 3) eyes
            g2.setColor(Color.WHITE);
            g2.fillOval(px + 4, py + 4, 6, 6);
            g2.fillOval(px + 10, py + 4, 6, 6);

            // 4) pupils
            g2.setColor(base == Color.WHITE ? Color.BLACK : Color.BLUE);
            g2.fillOval(px + 5, py + 5, 3, 3);
            g2.fillOval(px + 11, py + 5, 3, 3);
        }
    }

    private void drawHUD(Graphics g) {
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("SCORE: " + engine.score, 20, 25);

        for(int i = 0; i < engine.lives; i++) {
            g.fillArc(20 + (i * 30), 40, 25, 25, 30, 300);
        }

        g.drawString("LEVEL 01", getWidth() - 150, 25);
    }

    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(engine.gameWon ? Color.GREEN : Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        String message = engine.gameWon ? "YOU WIN!" : "GAME OVER";
        g.drawString(message, getWidth()/2 - 120, getHeight()/2);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Final Score: " + engine.score, getWidth()/2 - 80, getHeight()/2 + 50);
        g.drawString("Press SPACE to restart", getWidth()/2 - 120, getHeight()/2 + 100);
    }

    private void drawMainMenu(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        g.drawString("PAC-MAN", getWidth()/2 - 110, 150);

        String[] menuItems = {
            "1. START GAME",
            "2. HIGH SCORES",
            "3. QUIT"
        };

        g.setFont(new Font("Arial", Font.BOLD, 24));
        for(int i = 0; i < menuItems.length; i++) {
            if(i == selectedMenuItem) {
                g.setColor(Color.YELLOW);
                g.fillOval(getWidth()/2 - 120, 210 + i * 50, 15, 15);
            } else {
                g.setColor(Color.WHITE);
            }
            g.drawString(menuItems[i], getWidth()/2 - 90, 230 + i * 50);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        switch (key) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                desiredDirX = 0;
                desiredDirY = -1;
                engine.lastDirection = Constants.UP;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                desiredDirX = 0;
                desiredDirY = 1;
                engine.lastDirection = Constants.DOWN;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                desiredDirX = -1;
                desiredDirY = 0;
                engine.lastDirection = Constants.LEFT;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                desiredDirX = 1;
                desiredDirY = 0;
                engine.lastDirection = Constants.RIGHT;
                break;
            case KeyEvent.VK_SPACE:
                if (inMainMenu || engine.gameOver) {
                    inMainMenu = false;
                    engine.gameOver = false;
                    engine.gameWon = false;
                    engine.score = 0;
                    engine.lives = 3;
                    engine.resetPositions(true);
                }
                break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    private void updateMenuSelection(int mouseY) {
        if(mouseY >= 200 && mouseY <= 250) {
            selectedMenuItem = 0;
        } else if(mouseY >= 250 && mouseY <= 300) {
            selectedMenuItem = 1;
        } else if(mouseY >= 300 && mouseY <= 350) {
            selectedMenuItem = 2;
        } else {
            selectedMenuItem = -1;
        }
    }

    private void handleMenuClick(int mouseY) {
        updateMenuSelection(mouseY);
        switch(selectedMenuItem) {
            case 0:
                inMainMenu = false;
                break;
            case 1:
                // Handle high scores
                break;
            case 2:
                System.exit(0);
                break;
        }
        repaint();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(engine.map[0].length * Constants.TILE_SIZE, 
                           engine.map.length * Constants.TILE_SIZE);
    }
}