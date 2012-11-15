
import java.util.*;
import static java.lang.StrictMath.PI;
import model.*;


public class SingleStrategyImpl extends BaseStrategyImpl {
    
    public SingleStrategyImpl(Tank self, World world, Move move, State state) {
        super(self, world, move, state);
    }
 
	private void avoidDanger(List<Shell> dangerShells) {
		if (isRearToWall()) {
			driveForward();
		} else if (isFrontToWall()) {
			driveBackward();
		} else {
			Shell shell = getNearestShell(dangerShells);
			double angle = shell.getAngleTo(self);
			double dist = shell.getDistanceTo(self);
			double dist1 = dist * Math.cos(angle);
			double shellAngle = shell.getAngle();
			double xInter = shell.getX() + dist1 * Math.cos(shellAngle);
			double yInter = shell.getY() + dist1 * Math.sin(shellAngle);
			double myAngle = self.getAngleTo(xInter, yInter);
			if (Math.abs(myAngle) > PI/2) {
				driveForward();
			} else {
				driveBackward();
			}
		}
	}

	private void avoidTargeting(List<Tank> targetingEnemies) {
	    Tank enemy = getNearestTank(targetingEnemies);
	    Point shelter = findShelter(enemy);
	    int nearestBonus = getNearestBonus();
	    if (shelter != null) {
	        drive(shelter);
	    } else if (nearestBonus != -1) {
	        drive(world.getBonuses()[nearestBonus]);
	    } else if (!isFrontToWall() && Math.abs(self.getAngleTo(enemy)) > PI/6) {
    		driveForward();
	    } else {
	        drive(world.getWidth() / 2, world.getHeight() / 2);
	    }
	}

	private boolean shouldLeaveCorner() {
		if (self.getCrewHealth() < self.getCrewMaxHealth() * 0.66) {
			return true;
		}
		if (self.getHullDurability() < self.getHullMaxDurability() * 0.66) {
			return true;
		}
		List<Tank> alive = getAliveTanks();
		if (alive.size() <= 4) {
			return true;
		}
		Point nearestCorner = getNearestFreeCorner();
		if (getTargetingEnemies().size() > 1 && 
		    (nearestCorner == null || self.getDistanceTo(nearestCorner.x, nearestCorner.y) < self.getWidth()))
		{
			return true;
		}
		return false;
	}
	
	private void initMove() {
	    driveBackward();
		double x = self.getX();
		double y = self.getY();
		if (x <= self.getHeight() || x >= world.getWidth() - self.getHeight() ||
		    y <= self.getHeight() || y >= world.getHeight() - self.getHeight())
		{
			state = State.ToCorner;
		}
	}
	
	private void toCornerMove() {
		Point corner = getNearestFreeCorner();
		if (corner == null) {
			Point wall = getNearestWall();
			quickDrive(wall);
		} else {
			quickDrive(corner);
		}
		
		if (self.getDistanceTo(corner.x, corner.y) < self.getWidth() / 2) {
		    state = State.InCorner;
		}
	}
	
	private void inCornerMove() {
		List<Shell> dangerShells = getDangerShells();
		if (!dangerShells.isEmpty()) {
			avoidDanger(dangerShells);
		}
		
		if (shouldLeaveCorner()) {
		    state = State.Walk;
		} else {
		    Point corner = getNearestCorner();
		    if (self.getDistanceTo(corner.x, corner.y) >= self.getWidth() / 2) {
		        state = State.ToCorner;
		    }
		}
	}
	
	private void walkMove() {
		int bonusIndex = getImportantBonus();
		List<Shell> dangerShells = getDangerShells();
		List<Tank> targetingEnemies = getTargetingEnemies();
		List<Tank> enemies = getAliveEnemies();
		Point nearestCorner = getNearestFreeCorner();
		if (!dangerShells.isEmpty()) {
			avoidDanger(dangerShells);
		} else if (!targetingEnemies.isEmpty()) {
		    avoidTargeting(targetingEnemies);
		} else if (bonusIndex != -1) {
			Bonus bonus = world.getBonuses()[bonusIndex];
			drive(bonus);
		} else if (enemies.size() < 3 && !getDeadTanks().isEmpty()) {
		    Tank enemy = !targetingEnemies.isEmpty() ? getNearestTank(targetingEnemies) : getNearestTank(enemies);
		    Point shelter = findShelter(enemy);
		    if (shelter != null) {
			    drive(shelter);
		    } else {
		        drive(nearestCorner);
		    }
		} else if (nearestCorner != null) {
			drive(nearestCorner);
		} else {
			drive(getNearestWall());
		}
		
		if (getAliveEnemies().size() == 1) {
			state = State.OneOnOne;
		}	}
	
	private void oneOnOneMove() {
		Tank enemy = getAliveEnemies().get(0);
		List<Shell> dangerShells = getDangerShells();
		int bonusIndex = getImportantBonus();
		Point shelter = findShelter(enemy);
		
		if (!dangerShells.isEmpty()) {
			avoidDanger(dangerShells);
		} else if (bonusIndex != -1) {
			Bonus bonus = world.getBonuses()[bonusIndex];
			drive(bonus);
		} else if (enemy.getCrewHealth() < self.getCrewHealth()) {
			drive(enemy);
		} else if (shelter != null) {
			drive(shelter);
		} else {
		    drive(getNearestFreeCorner());
		}	}
	
	private void selectDriveMove() {
	    switch (state) {
	    case Init: initMove(); break;
	    case ToCorner: toCornerMove(); break;
	    case InCorner: inCornerMove(); break;
	    case Walk: walkMove(); break;
	    case OneOnOne: oneOnOneMove(); break;
	    }
	}
	
	private void selectShootMove() {
		List<Tank> enemies = getAliveEnemies();
		if (enemies.isEmpty()) {
			// something is wrong!
			return;
		}
		List<Tank> openEnemies = selectOpenEnemies(enemies);
		if (openEnemies.isEmpty()) {
			Tank enemy = Collections.max(enemies, new EnemiesComparator(self));
			turnTurretTo(enemy);
			return;
		}
		Tank enemy = Collections.max(openEnemies, new EnemiesComparator(self));
		tryShoot(enemy);
	}
    
    public static TankType getTankType() {
        return TankType.MEDIUM;        
    }
    
    public void run() {
		selectShootMove();
		selectDriveMove();
    }

    public State getState() {
        return state;
    }
}
