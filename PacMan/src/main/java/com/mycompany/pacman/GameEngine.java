package com.mycompany.pacman;

import java.awt.Point;
import java.util.*;

public class GameEngine {
    // Game state (keep all original variable names)
    public int mouthAngle = 0;
    public boolean mouthOpening = true;
    public int pacmanX = 35;
    public int pacmanY = 25;
    public int dirX = 0;
    public int dirY = 0;
    public int lastDirection = Constants.RIGHT;
    public boolean powerMode = false;
    public int powerTimer = 0;
    public int score = 0;
    public int lives = 3;
    public boolean gameOver = false;
    public boolean gameWon = false;
    public int gameTick = 0;
    public int[][] map;
    public int[][] originalMap;
    public int[][] ghosts = {
        {35,15,0,0}, {35,16,1,0}, {32,15,2,0}, {32,16,3,0}
    };
    public Deque<Point>[] exitPaths;
    private Random rand = new Random();
    private final Point[] scatterTargets;
    private final Point ghostDoor = new Point(33, 13);

    public GameEngine() {
        originalMap = deepCopyMap(Constants.MAP);
        map = deepCopyMap(originalMap);
        exitPaths = new ArrayDeque[ghosts.length];
        for(int i=0; i<ghosts.length; i++) exitPaths[i] = new ArrayDeque<>();
        
        this.scatterTargets = new Point[]{
            new Point(0, 0),
            new Point(map[0].length-1, 0),
            new Point(map[0].length-1, map.length-1),
            new Point(0, map.length-1)
        };
    }

    private int[][] deepCopyMap(int[][] source) {
        int[][] copy = new int[source.length][];
        for(int i=0; i<source.length; i++) copy[i] = source[i].clone();
        return copy;
    }

    // All original game logic methods
    public void updateAnimations() {
        if(mouthOpening) mouthAngle += 10;
        else mouthAngle -= 10;
        if(mouthAngle >= 60) mouthOpening = false;
        else if(mouthAngle <= 0) mouthOpening = true;
        if(powerMode && --powerTimer <= 0) powerMode = false;
    }

    public void movePacman(int desiredDirX, int desiredDirY) {
         // Check if the desired direction is possible
        int desiredNewX = (pacmanX + desiredDirX + map[0].length) % map[0].length;
        int desiredNewY = (pacmanY + desiredDirY + map.length) % map.length;

        if (map[desiredNewY][desiredNewX] != 1) {
            // Update direction to the desired direction if valid
            dirX = desiredDirX;
            dirY = desiredDirY;
        }

        // Proceed with current direction
        int newX = (pacmanX + dirX + map[0].length) % map[0].length;
        int newY = (pacmanY + dirY + map.length) % map.length;

        if (map[newY][newX] != 1) {
            pacmanX = newX;
            pacmanY = newY;
            lastDirection = getDirectionFromDelta(dirX, dirY);
            checkPellet();
        }
    }

    public void moveGhosts() {
        gameTick++;
        boolean chasePhase = (gameTick / 100) % 2 == 0;

        for (int i = 0; i < ghosts.length; i++) {
            int[] ghost = ghosts[i];
            int gx = ghost[0], gy = ghost[1];
            int currDir = ghost[3];

            if (map[gy][gx] == Constants.GHOST_BOX) {
                moveGhostOutOfBox(ghost, i);
                continue;
            }

            List<Integer> moves = new ArrayList<>();
            for (int d = 0; d < 4; d++) {
                if (d == opposite(currDir)) continue;
                int nx = (gx + Constants.DELTAS[d][0] + map[0].length) % map[0].length;
                int ny = (gy + Constants.DELTAS[d][1] + map.length) % map.length;
                if (map[ny][nx] != Constants.WALL) moves.add(d);
            }

            Point target = chasePhase ? 
                new Point(pacmanX, pacmanY) : 
                scatterTargets[ghost[2]];

            if (!moves.isEmpty() && Math.random() < 0.7) {
                Point[] path = aStar(gx, gy, target.x, target.y);
                if (path.length > 1) {
                    Point nextStep = path[1];
                    currDir = getDirectionFromDelta(nextStep.x - gx, nextStep.y - gy);
                }
            } else if (!moves.isEmpty()) {
                currDir = moves.get(rand.nextInt(moves.size()));
            }

            int nx = (gx + Constants.DELTAS[currDir][0] + map[0].length) % map[0].length;
            int ny = (gy + Constants.DELTAS[currDir][1] + map.length) % map.length;

            if (map[ny][nx] != Constants.WALL) {
                ghost[0] = nx;
                ghost[1] = ny;
                ghost[3] = currDir;
            }
        }
    }


    public void checkCollisions() {
        for (int[] ghost : ghosts) {
            if (ghost[0] == pacmanX && ghost[1] == pacmanY) {
                if (powerMode) {
                    // Eat ghost
                    ghost[0] = 32;
                    ghost[1] = 15;
                    score += 200;
                } else {
                    if (--lives <= 0) {
                        gameOver = true;
                        gameWon = false; // Explicit loss
                    }
                    resetPositions(false); // Don't reset pellets
                }
            }
        }
    }

    public void checkPellet() {
        int cell = map[pacmanY][pacmanX];
        if (cell == 0) {
            score += 10;
            map[pacmanY][pacmanX] = 4;
        } else if (cell == 2) {
            score += 50;
            powerMode = true;
            powerTimer = 300;
            map[pacmanY][pacmanX] = 4;
        }
        checkWin();
    }

    public void checkWin() {
        for (int[] row : map) {
            for (int c : row) {
                if (c == 0 || c == 2) return;
            }
        }
        gameWon = true;
        gameOver = true;
    }

    public void resetPositions(boolean fullReset) {
        if (fullReset) {
            // Restore pellets from original map
            for (int y = 0; y < map.length; y++) {
                System.arraycopy(originalMap[y], 0, map[y], 0, map[y].length);
            }
        }

        // Reset positions (same for both cases)
        pacmanX = 35;
        pacmanY = 25;
        dirX = 0;
        dirY = 0;

        ghosts[0][0] = 35; ghosts[0][1] = 15;
        ghosts[1][0] = 35; ghosts[1][1] = 16;
        ghosts[2][0] = 32; ghosts[2][1] = 15;
        ghosts[3][0] = 32; ghosts[3][1] = 16;

        for (int i = 0; i < ghosts.length; i++) {
            exitPaths[i].clear();
        }
    }

    // Keep all helper methods
    private Point[] aStar(int sx, int sy, int tx, int ty) {
        int H = map.length, W = map[0].length;
        class Node implements Comparable<Node> {
            int x, y, g, f;
            Node parent;
            Node(int x,int y,int g,int f,Node p){this.x=x;this.y=y;this.g=g;this.f=f;this.parent=p;}
            public int compareTo(Node o){return this.f - o.f;}
        }
        PriorityQueue<Node> open = new PriorityQueue<>();
        boolean[][] closed = new boolean[H][W];
        open.add(new Node(sx, sy, 0, Math.abs(sx-tx)+Math.abs(sy-ty), null));

        while (!open.isEmpty()) {
            Node cur = open.poll();
            if (cur.x==tx && cur.y==ty) {
                // reconstruct
                List<Point> path = new ArrayList<>();
                for (Node n=cur; n!=null; n=n.parent)
                    path.add(0, new Point(n.x,n.y));
                return path.toArray(new Point[0]);
            }
            if (closed[cur.y][cur.x]) continue;
            closed[cur.y][cur.x] = true;

            for (int d=0; d<4; d++) {
                int nx = (cur.x + Constants.DELTAS[d][0] + W)%W;
                int ny = (cur.y + Constants.DELTAS[d][1] + H)%H;
                if (!closed[ny][nx] && map[ny][nx] != 1) {
                    int g2 = cur.g + 1;
                    int h2 = Math.abs(nx-tx) + Math.abs(ny-ty);
                    open.add(new Node(nx, ny, g2, g2+h2, cur));
                }
            }
        }
        return new Point[0];
    }

    private int opposite(int dir) {
        switch(dir) {
            case Constants.RIGHT: return Constants.LEFT;
            case Constants.LEFT: return Constants.RIGHT;
            case Constants.UP: return Constants.DOWN;
            case Constants.DOWN: return Constants.UP;
        }
        return -1;
    }

    public int getDirectionFromDelta(int dx, int dy) {
        if (dx > 0) return Constants.RIGHT;
        if (dx < 0) return Constants.LEFT;
        if (dy < 0) return Constants.UP;
        if (dy > 0) return Constants.DOWN;
        return lastDirection;
    }

    private void moveGhostOutOfBox(int[] ghost, int idx) {
        Deque<Point> path = exitPaths[idx];
        if (path.isEmpty()) {
            path.addAll(findPath(ghost[0], ghost[1], ghostDoor.x, ghostDoor.y));
        }
        if (!path.isEmpty()) {
            Point next = path.removeFirst();
            ghost[0] = next.x;
            ghost[1] = next.y;
        }
    }

    private Deque<Point> findPath(int sx, int sy, int tx, int ty) {
        int H = map.length, W = map[0].length;
        boolean[][] seen = new boolean[H][W];
        Point[][] parent = new Point[H][W];
        Deque<Point> q = new ArrayDeque<>();
        seen[sy][sx] = true;
        q.addLast(new Point(sx, sy));

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (!q.isEmpty()) {
            Point p = q.removeFirst();
            if (p.x == tx && p.y == ty) break;
            for (int d = 0; d < 4; d++) {
                int nx = (p.x + dx[d] + W) % W;
                int ny = (p.y + dy[d] + H) % H;
                if (!seen[ny][nx] && map[ny][nx] != Constants.WALL) {
                    seen[ny][nx] = true;
                    parent[ny][nx] = p;
                    q.addLast(new Point(nx, ny));
                }
            }
        }

        Deque<Point> path = new ArrayDeque<>();
        Point current = new Point(tx, ty);
        while (current != null && !(current.x == sx && current.y == sy)) {
            path.addFirst(current);
            current = parent[current.y][current.x];
        }
        return path;
    }
}