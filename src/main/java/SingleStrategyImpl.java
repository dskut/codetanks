
import java.util.*;
import model.*;


public class SingleStrategyImpl extends BaseStrategyImpl {
    
    public SingleStrategyImpl(Tank self, World world, Move move, State state) {
        super(self, world, move, state);
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
		List<Shell> dangerShells = getDangerShells();
		Point corner = getNearestFreeCorner();
		Point dest = corner != null ? corner : getNearestWall();
		if (!dangerShells.isEmpty()) {
			avoidDanger(dangerShells);
		} else {
    		quickDrive(dest);
		}
		
		if (isTwoOnOne()) {
		    state = State.TwoOnOne;
		} else if (isOneOnOne()) {
		    state = State.OneOnOne;
    	} else if (self.getDistanceTo(dest.x, dest.y) < self.getWidth() / 2) {
		    state = State.InCorner;
		} else if (existUnit(dest)) {
		    state = State.InCorner;
		}
	}
	
	private void inCornerMove() {
		List<Shell> dangerShells = getDangerShells();
		if (!dangerShells.isEmpty()) {
			avoidDanger(dangerShells);
		}
		
		if (isTwoOnOne()) {
		    state = State.TwoOnOne;
		} else if (isOneOnOne()) {
		    state = State.OneOnOne;
    	} else if (shouldLeaveCorner()) {
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
		
		if (isOneOnOne()) {
		    state = State.OneOnOne;
		}
	}
	
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
		} else if (isStronger(self, enemy)) {
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
