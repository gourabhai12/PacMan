import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Random;
import javax.swing.*;

public class PacMan extends JPanel implements ActionListener, KeyListener {
    class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;

        int startX;
        int startY;
        char direction = 'U'; // U D L R
        int velocityX = 0;
        int velocityY = 0;
        boolean isPowerful = false;
        int fruitType = 0; // 0=none, 1=cherry, 2=strawberry, 3=orange

        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }

        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        void updateVelocity() {
            int baseSpeed = tileSize/4;
            int actualSpeed = baseSpeed + (speed * baseSpeed / 5);
            
            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -actualSpeed;
            }
            else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = actualSpeed;
            }
            else if (this.direction == 'L') {
                this.velocityX = -actualSpeed;
                this.velocityY = 0;
            }
            else if (this.direction == 'R') {
                this.velocityX = actualSpeed;
                this.velocityY = 0;
            }
        }

        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    private int rowCount = 21;
    private int columnCount = 19;
    private int tileSize = 32;
    private int boardWidth = columnCount * tileSize;
    private int boardHeight = rowCount * tileSize;

    private Image wallImage;
    private Image blueGhostImage;
    private Image orangeGhostImage;
    private Image pinkGhostImage;
    private Image redGhostImage;

    private Image pacmanUpImage;
    private Image pacmanDownImage;
    private Image pacmanLeftImage;
    private Image pacmanRightImage;

    //X = wall, O = skip, P = pac man, ' ' = food
    //Ghosts: b = blue, o = orange, p = pink, r = red, g = powerful ghost
    private String[] tileMap = {
        "XXXXXXXXXXXXXXXXXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X                 X",
        "X XX X XXXXX X XX X",
        "X    X       X    X",
        "XXXX XXXX XXXX XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXrXX X XXXX",
        "O       bpo  g    O",
        "XXXX X XXXXX X XXXX",
        "OOOX X       X XOOO",
        "XXXX X XXXXX X XXXX",
        "X        X        X",
        "X XX XXX X XXX XX X",
        "X  X     P     X  X",
        "XX X X XXXXX X X XX",
        "X    X   X   X    X",
        "X XXXXXX X XXXXXX X",
        "X                 X",
        "XXXXXXXXXXXXXXXXXXX" 
    };

    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    HashSet<Block> fruits;
    Block pacman;
    Block powerfulGhost;

    Timer gameLoop;
    Timer fruitTimer;
    char[] directions = {'U', 'D', 'L', 'R'}; //up down left right
    Random random = new Random();
    int score = 0;
    int lives = 3;
    boolean gameOver = false;
    boolean paused = false;
    int speed = 0; // Speed level
    int lastSpeedIncreaseScore = 0;

    PacMan() {
        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        //load images
        wallImage = new ImageIcon(getClass().getResource("./wall.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("./blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("./orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("./pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("./redGhost.png")).getImage();

        pacmanUpImage = new ImageIcon(getClass().getResource("./pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("./pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("./pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("./pacmanRight.png")).getImage();

        loadMap();
        for (Block ghost : ghosts) {
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        
        // Powerful ghost with red ghost image but different behavior
        if (powerfulGhost != null) {
            powerfulGhost.isPowerful = true;
            powerfulGhost.image = redGhostImage;
            char newDirection = directions[random.nextInt(4)];
            powerfulGhost.updateDirection(newDirection);
        }
        
        //how long it takes to start timer, milliseconds gone between frames
        gameLoop = new Timer(50, this); //20fps (1000/50)
        gameLoop.start();
        
        // Timer for randomly spawning fruits
        fruitTimer = new Timer(10000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!paused && !gameOver && fruits.isEmpty()) {
                    spawnRandomFruit();
                }
            }
        });
        fruitTimer.start();
    }

    public void loadMap() {
        walls = new HashSet<Block>();
        foods = new HashSet<Block>();
        ghosts = new HashSet<Block>();
        fruits = new HashSet<Block>();
        powerfulGhost = null;

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = tileMap[r];
                char tileMapChar = row.charAt(c);

                int x = c*tileSize;
                int y = r*tileSize;

                if (tileMapChar == 'X') { //block wall
                    Block wall = new Block(wallImage, x, y, tileSize, tileSize);
                    walls.add(wall);
                }
                else if (tileMapChar == 'b') { //blue ghost
                    Block ghost = new Block(blueGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'o') { //orange ghost
                    Block ghost = new Block(orangeGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'p') { //pink ghost
                    Block ghost = new Block(pinkGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'r') { //red ghost
                    Block ghost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(ghost);
                }
                else if (tileMapChar == 'g') { //powerful ghost
                    powerfulGhost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    powerfulGhost.isPowerful = true;
                }
                else if (tileMapChar == 'P') { //pacman
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                }
                else if (tileMapChar == ' ') { //food
                    Block food = new Block(null, x + 14, y + 14, 4, 4);
                    foods.add(food);
                }
            }
        }
    }

    public void spawnRandomFruit() {
        // Find a random empty position (not on wall, ghost, food)
        boolean validPosition = false;
        int x = 0, y = 0;
        
        while (!validPosition) {
            int randomRow = random.nextInt(rowCount - 2) + 1; // Avoid borders
            int randomCol = random.nextInt(columnCount - 2) + 1;
            
            x = randomCol * tileSize;
            y = randomRow * tileSize;
            
            // Check if position is valid (not on wall, ghost, etc.)
            validPosition = true;
            Block tempBlock = new Block(null, x, y, tileSize, tileSize);
            
            for (Block wall : walls) {
                if (collision(tempBlock, wall)) {
                    validPosition = false;
                    break;
                }
            }
            
            if (validPosition) {
                for (Block ghost : ghosts) {
                    if (collision(tempBlock, ghost)) {
                        validPosition = false;
                        break;
                    }
                }
            }
            
            if (validPosition && powerfulGhost != null) {
                if (collision(tempBlock, powerfulGhost)) {
                    validPosition = false;
                }
            }
            
            if (validPosition && collision(tempBlock, pacman)) {
                validPosition = false;
            }
        }
        
        // Create fruit
        int fruitType = random.nextInt(3) + 1; // 1=cherry, 2=strawberry, 3=orange
        Block fruit = new Block(null, x, y, tileSize, tileSize);
        fruit.fruitType = fruitType;
        fruits.add(fruit);
    }
    
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        g.drawImage(pacman.image, pacman.x, pacman.y, pacman.width, pacman.height, null);

        for (Block ghost : ghosts) {
            g.drawImage(ghost.image, ghost.x, ghost.y, ghost.width, ghost.height, null);
        }
        
        if (powerfulGhost != null) {
            // Draw powerful ghost slightly larger and with a red glow
            g.drawImage(powerfulGhost.image, powerfulGhost.x - 2, powerfulGhost.y - 2, 
                        powerfulGhost.width + 4, powerfulGhost.height + 4, null);
            
            // Add red glow outline
            g.setColor(Color.RED);
            g.drawRect(powerfulGhost.x - 3, powerfulGhost.y - 3, 
                       powerfulGhost.width + 6, powerfulGhost.height + 6);
        }

        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x, wall.y, wall.width, wall.height, null);
        }

        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x, food.y, food.width, food.height);
        }
        
        // Draw fruits
        for (Block fruit : fruits) {
            if (fruit.fruitType == 1) {
                // Cherry (red circle)
                g.setColor(Color.RED);
                g.fillOval(fruit.x + 8, fruit.y + 8, 16, 16);
                g.setColor(new Color(139, 69, 19)); // Brown for stem
                g.fillRect(fruit.x + 16, fruit.y + 4, 2, 8);
            } else if (fruit.fruitType == 2) {
                // Strawberry (red triangle with green top)
                g.setColor(Color.RED);
                int[] xPoints = {fruit.x + 16, fruit.x + 8, fruit.x + 24};
                int[] yPoints = {fruit.y + 24, fruit.y + 8, fruit.y + 8};
                g.fillPolygon(xPoints, yPoints, 3);
                g.setColor(Color.GREEN);
                g.fillRect(fruit.x + 14, fruit.y + 4, 4, 4);
            } else if (fruit.fruitType == 3) {
                // Orange (orange circle)
                g.setColor(Color.ORANGE);
                g.fillOval(fruit.x + 8, fruit.y + 8, 16, 16);
                g.setColor(new Color(0, 100, 0)); // Dark green for leaf
                g.fillOval(fruit.x + 16, fruit.y + 4, 4, 4);
            }
        }
        
        //score
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        if (gameOver) {
            g.setColor(Color.RED);
            g.drawString("Game Over: " + String.valueOf(score), tileSize/2, tileSize/2);
        }
        else {
            g.setColor(Color.WHITE);
            g.drawString("x" + String.valueOf(lives) + " Score: " + String.valueOf(score) + " Speed: " + String.valueOf(speed), tileSize/2, tileSize/2);
            
            if (paused) {
                g.setColor(new Color(255, 255, 255, 150)); // Semi-transparent white
                g.fillRect(0, 0, boardWidth, boardHeight);
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 32));
                String pauseMsg = "PAUSED";
                FontMetrics fm = g.getFontMetrics();
                int textWidth = fm.stringWidth(pauseMsg);
                g.drawString(pauseMsg, (boardWidth - textWidth) / 2, boardHeight / 2);
                g.setFont(new Font("Arial", Font.PLAIN, 16));
                String resumeMsg = "Press 'R' to resume";
                textWidth = fm.stringWidth(resumeMsg);
                g.drawString(resumeMsg, (boardWidth - textWidth) / 2, boardHeight / 2 + 40);
            }
        }
    }

    public void move() {
        if (paused) return;
        
        pacman.x += pacman.velocityX;
        pacman.y += pacman.velocityY;

        //check wall collisions
        for (Block wall : walls) {
            if (collision(pacman, wall)) {
                pacman.x -= pacman.velocityX;
                pacman.y -= pacman.velocityY;
                break;
            }
        }

        //check ghost collisions
        for (Block ghost : ghosts) {
            if (collision(ghost, pacman)) {
                lives -= 1;
                if (lives == 0) {
                    gameOver = true;
                    return;
                }
                resetPositions();
            }

            if (ghost.y == tileSize*9 && ghost.direction != 'U' && ghost.direction != 'D') {
                ghost.updateDirection('U');
            }
            ghost.x += ghost.velocityX;
            ghost.y += ghost.velocityY;
            for (Block wall : walls) {
                if (collision(ghost, wall) || ghost.x <= 0 || ghost.x + ghost.width >= boardWidth) {
                    ghost.x -= ghost.velocityX;
                    ghost.y -= ghost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    ghost.updateDirection(newDirection);
                }
            }
        }
        
        // Powerful ghost movement
        if (powerfulGhost != null) {
            // Move towards pacman (simple AI)
            if (random.nextInt(4) == 0) { // 25% chance to change direction based on pacman's position
                if (powerfulGhost.x < pacman.x) {
                    powerfulGhost.updateDirection('R');
                } else if (powerfulGhost.x > pacman.x) {
                    powerfulGhost.updateDirection('L');
                } else if (powerfulGhost.y < pacman.y) {
                    powerfulGhost.updateDirection('D');
                } else {
                    powerfulGhost.updateDirection('U');
                }
            }
            
            powerfulGhost.x += powerfulGhost.velocityX;
            powerfulGhost.y += powerfulGhost.velocityY;
            
            // Check wall collision for powerful ghost
            boolean collided = false;
            for (Block wall : walls) {
                if (collision(powerfulGhost, wall) || powerfulGhost.x <= 0 || powerfulGhost.x + powerfulGhost.width >= boardWidth) {
                    powerfulGhost.x -= powerfulGhost.velocityX;
                    powerfulGhost.y -= powerfulGhost.velocityY;
                    char newDirection = directions[random.nextInt(4)];
                    powerfulGhost.updateDirection(newDirection);
                    collided = true;
                    break;
                }
            }
            
            // Check collision with pacman - instant game over for powerful ghost
            if (collision(powerfulGhost, pacman)) {
                lives = 0;
                gameOver = true;
                return;
            }
        }
        
        // Check fruit collision
        Block fruitEaten = null;
        for (Block fruit : fruits) {
            if (collision(pacman, fruit)) {
                fruitEaten = fruit;
                int points = fruit.fruitType * 10; // 10, 20, or 30 points
                score += points;
                
                // Update speed if score threshold reached
                checkSpeedIncrease();
                
                break;
            }
        }
        if (fruitEaten != null) {
            fruits.remove(fruitEaten);
        }

        //check food collision
        Block foodEaten = null;
        for (Block food : foods) {
            if (collision(pacman, food)) {
                foodEaten = food;
                score += 10;
                
                // Update speed if score threshold reached
                checkSpeedIncrease();
            }
        }
        foods.remove(foodEaten);

        if (foods.isEmpty()) {
            loadMap();
            resetPositions();
        }
    }
    
    private void checkSpeedIncrease() {
        // Increase speed every 100 points
        if (score >= lastSpeedIncreaseScore + 100) {
            speed++;
            lastSpeedIncreaseScore = score;
            
            // Update velocities for all moving objects
            pacman.updateVelocity();
            for (Block ghost : ghosts) {
                ghost.updateVelocity();
            }
            if (powerfulGhost != null) {
                powerfulGhost.updateVelocity();
            }
        }
    }

    public boolean collision(Block a, Block b) {
        return  a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    public void resetPositions() {
        pacman.reset();
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        
        for (Block ghost : ghosts) {
            ghost.reset();
            char newDirection = directions[random.nextInt(4)];
            ghost.updateDirection(newDirection);
        }
        
        if (powerfulGhost != null) {
            powerfulGhost.reset();
            char newDirection = directions[random.nextInt(4)];
            powerfulGhost.updateDirection(newDirection);
        }
    }
    
    public void togglePause() {
        paused = !paused;
        if (paused) {
            // Stop timers
            fruitTimer.stop();
        } else {
            // Resume timers
            fruitTimer.start();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        move();
        repaint();
        if (gameOver) {
            gameLoop.stop();
            fruitTimer.stop();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {
        if (gameOver) {
            loadMap();
            resetPositions();
            lives = 3;
            score = 0;
            speed = 0;
            lastSpeedIncreaseScore = 0;
            gameOver = false;
            gameLoop.start();
            fruitTimer.start();
            return;
        }
        
        // Handle pause/resume
        if (e.getKeyCode() == KeyEvent.VK_P) {
            togglePause();
            return;
        }
        
        if (e.getKeyCode() == KeyEvent.VK_R) {
            if (paused) {
                togglePause();
            }
            return;
        }
        
        // Don't process movement if game is paused
        if (paused) {
            return;
        }
        
        // System.out.println("KeyEvent: " + e.getKeyCode());
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            pacman.updateDirection('U');
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            pacman.updateDirection('D');
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            pacman.updateDirection('L');
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            pacman.updateDirection('R');
        }

        if (pacman.direction == 'U') {
            pacman.image = pacmanUpImage;
        }
        else if (pacman.direction == 'D') {
            pacman.image = pacmanDownImage;
        }
        else if (pacman.direction == 'L') {
            pacman.image = pacmanLeftImage;
        }
        else if (pacman.direction == 'R') {
            pacman.image = pacmanRightImage;
        }
    }
}