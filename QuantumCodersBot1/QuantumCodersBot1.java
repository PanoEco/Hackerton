import dev.robocode.tankroyale.botapi.*;
import dev.robocode.tankroyale.botapi.events.*;
import dev.robocode.tankroyale.botapi.graphics.Color;

public class QuantumCodersBot1 extends Bot {

    boolean peek; 
    double moveAmount; 
    
    private double enemyDistance = 0;
    private double enemyBearing = 0;
    private double enemyVelocity = 0;
    private double enemyHeading = 0;
    private int moveDirection = 1; 
    private int turnDirection = 1; 
    private long lastScanTime = 0;
    private double previousEnemyBearing = 0;
    private boolean dodging = false;

    public static void main(String[] args) {
        new QuantumCodersBot1().start();
    }

    
    QuantumCodersBot1() {
        super(BotInfo.fromFile("QuantumCodersBot1.json"));
    }

    
    @Override
    public void run() {
        setBodyColor(Color.DARK_GRAY);
        setTurretColor(Color.BLACK);
        setRadarColor(Color.RED);
        setBulletColor(Color.YELLOW);
        setScanColor(Color.YELLOW);

        
        moveAmount = Math.max(getArenaWidth(), getArenaHeight());
        peek = false;

       
        setAdjustGunForBodyTurn(true);
        setAdjustRadarForGunTurn(true);

       
        turnRight(getDirection() % 90);
        forward(moveAmount);

      
        turnRadarRight(360);

     
        while (isRunning()) {
           
            if (dodging) {
                performEvasiveManeuvers();
            } else if (getTurnNumber() - lastScanTime > 20) {
               
                wallFollowingMovement();
            } else {
               
                combatMovement();
            }
            
           
            turnRadarRight(45);
        }
    }
    
    
    private void wallFollowingMovement() {
        peek = true;
        forward(moveAmount);
        peek = false;
        turnLeft(90);
    }
    
    
    private void combatMovement() {
        
        if (getTurnNumber() % 20 == 0) {
            moveDirection *= -1; 
        }
        
        if (moveDirection == 1) {
            forward(100);
        } else {
            back(100);
        }
        
        
        double gunTurnAmt = normalizeAngle(enemyBearing - getGunDirection());
        turnGunRight(gunTurnAmt);
    }
    
    private void performEvasiveManeuvers() {
        turnDirection *= -1;
        turnRight(30 * turnDirection);
        
        if (moveDirection == 1) {
            forward(80);
        } else {
            back(80);
        }
        
        dodging = false; 
    }
    
    
    private double normalizeAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    
    @Override
    public void onHitBot(HitBotEvent e) {
        
        var bearing = bearingTo(e.getX(), e.getY());
        if (bearing > -90 && bearing < 90) {
            back(100);
        } else { 
            forward(100);
        }
    }

    
    @Override
    public void onScannedBot(ScannedBotEvent e) {
       
        enemyDistance = distanceTo(e.getX(), e.getY());
        enemyBearing = bearingTo(e.getX(), e.getY());
        enemyVelocity = e.getSpeed();
        enemyHeading = e.getDirection();
        lastScanTime = getTurnNumber();
        
      
        double bulletPower = calculateOptimalBulletPower(enemyDistance);
        double bulletSpeed = 20 - 3 * bulletPower;
        double timeToTarget = enemyDistance / bulletSpeed;
        
        double predictedX = e.getX() + Math.sin(Math.toRadians(enemyHeading)) * enemyVelocity * timeToTarget;
        double predictedY = e.getY() + Math.cos(Math.toRadians(enemyHeading)) * enemyVelocity * timeToTarget;
        
      
        double angleToTarget = Math.toDegrees(Math.atan2(predictedX - getX(), predictedY - getY()));
        double gunTurnAmt = normalizeAngle(angleToTarget - getGunDirection());
        
       
        turnGunRight(gunTurnAmt);
        
        
        if (Math.abs(gunTurnAmt) < 10) { 
            fire(bulletPower);
        }
        
        
        double radarTurnAmt = normalizeAngle(getRadarDirection() - (getDirection() + enemyBearing));
        turnRadarLeft(radarTurnAmt);
        
        
        dodging = false; 
        
        
        if (peek) {
            rescan();
        }
    }
    
    
    private double calculateOptimalBulletPower(double distance) {
        if (distance < 100) {
            return 3.0; 
        } else if (distance < 300) {
            return 2.0; 
        } else {
            return 1.0; 
        }
    }
    
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        
        var bearing = calcBearing(e.getBullet().getDirection());

        
        dodging = true;
        
       
        turnRight(90 - bearing + (Math.random() * 20 - 10)); 
        moveDirection *= -1; 
        
        
        if (getEnergy() < 20) {
           
            back(150);
        } else {
            
            forward(100);
        }
    }
    
    
    @Override
    public void onHitWall(HitWallEvent e) {
        
        moveDirection *= -1;
        turnDirection *= -1;
        
        turnRight(90 * turnDirection);
        
        if (moveDirection == 1) {
            forward(100);
        } else {
            back(100);
        }
    }
    
   
    @Override
    public void onBotDeath(BotDeathEvent e) {
      
        lastScanTime = 0;
        dodging = false;
        
        
        turnRadarRight(360);
    }
    
    
    @Override
    public void onWonRound(WonRoundEvent e) {
        
        for (int i = 0; i < 10; i++) {
            turnRight(360);
            fire(0.1); 
        }
    }
}