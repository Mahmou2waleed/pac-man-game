package com.mycompany.pacman;

import javax.swing.JFrame;

public class PacMan {
    public static void main(String[] args) {
        JFrame f = new JFrame("Pac-Man");
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
        GamePanel gamePanel = new GamePanel();
        f.add(gamePanel);
        f.pack();   
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
