
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.StrictMath.PI;
import model.*;

class Point {
	public double x;
	public double y;
	
	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
}

class TankDistComparator implements Comparator<Point> {
	private Tank tank;
	public TankDistComparator(Tank tank) {
		this.tank = tank;
	}
	public int compare(Point first, Point second) {
		double diff = tank.getDistanceTo(first.x, first.y) - tank.getDistanceTo(second.x, second.y);
		return (int)diff;
	}
}

enum States {
	Init, // in the start phase - sit in corner
	Walk, // then - walk around map
}

public final class MyStrategy implements Strategy {
	private static final double MIN_SHOOT_ANGLE = PI / 180;
	private static final double MAX_SHELL_ANGLE = PI / 6;
	private static final double MAX_TARGET_ANGLE = PI / 30;
	private static final double MIN_DRIVE_ANGLE = PI / 6;
	private static final double MIN_SHOOT_DIST = 500;
	private static final double MIN_HEALTH = 0.3;
	private static final double STABLE_HEALTH = 0.6;
	private static final double MIN_DURABILITY = 0.3;
	private static final double STABLE_DURABILITY = 0.6;
	private static final double MAX_PREMIUM_SHOOT_DIST = 500;
	private static final double XMIN = 100;
	private static final double YMIN = 100;
	
	private States state;
	
	public MyStrategy() {
		state = States.Init;
	}
	
	private void driveForward(Move move) {
		move.setLeftTrackPower(1);
		move.setRightTrackPower(1);
	}
	
	private void driveBackward(Move move) {
		move.setLeftTrackPower(-1);
		move.setRightTrackPower(-1);
	}
	
	private boolean isAlive(Tank tank) {
		return tank.getCrewHealth() > 0 && tank.getHullDurability() > 0;
	}
	
	private int getSmallestAngleEnemy(Tank self, World world) {
		int index = -1;
		double minAngle = 1e9;
    	Tank[] tanks = world.getTanks();
    	for (int i = 0; i < tanks.length; ++i) {
    		Tank tank = tanks[i];
    		if (!tank.isTeammate() && isAlive(tank)) {
    			double angle = Math.abs(self.getTurretAngleTo(tank));
    			if (angle < minAngle) {
    				minAngle = angle;
    				index = i;
    			}
    		}
    	}
    	return index;
	}
	
	private int getNearestEnemy(Tank self, World world) {
		int index = -1;
		double minDist = 1e9;
    	Tank[] tanks = world.getTanks();
    	for (int i = 0; i < tanks.length; ++i) {
    		Tank tank = tanks[i];
    		if (!tank.isTeammate() && isAlive(tank)) {
    			double dist = self.getDistanceTo(tank);
    			if (dist < minDist) {
    				minDist = dist;
    				index = i;
    			}
    		}
    	}
    	return index;
	}
	
	// FIXME
	private int getDyingEnemy(World world) {
		Tank[] tanks = world.getTanks();
    	for (int i = 0; i < tanks.length; ++i) {
    		Tank tank = tanks[i];
    		if (!tank.isTeammate() && isAlive(tank) && tank.getCrewHealth() < 0.5 * tank.getCrewMaxHealth()) {
    			return i;
    		}
		}
		return -1;
	}
	
	private int getNearestBonus(Tank self, World world) {
		int res = -1;
		double minDist = 1e9;
		Bonus[] bonuses = world.getBonuses();
		for (int i = 0; i < bonuses.length; ++i) {
			Bonus bonus = bonuses[i];
			double dist = self.getDistanceTo(bonus);
			if (dist < minDist) {
				minDist = dist;
				res = i;
			}
		}
		return res;
	}
	
	private int getNearestBonus(Tank self, World world, BonusType type) {
		int res = -1;
		double minDist = 1e9;
		Bonus[] bonuses = world.getBonuses();
		for (int i = 0; i < bonuses.length; ++i) {
			Bonus bonus = bonuses[i];
			if (bonus.getType() != type) {
				continue;
			}
			double dist = self.getDistanceTo(bonus);
			if (dist < minDist) {
				minDist = dist;
				res = i;
			}
		}
		return res;
	}	
	private int getImportantBonus(Tank self, World world) {
		double relativeHealth = (double)self.getCrewHealth() / self.getCrewMaxHealth();
		if (relativeHealth < MIN_HEALTH) {
			int nearestMedicine = getNearestBonus(self, world, BonusType.MEDIKIT);
			if (nearestMedicine != -1) {
				return nearestMedicine;
			}
		}
		double relativeDurability = (double)self.getHullDurability() / self.getHullMaxDurability();
		if (relativeDurability < MIN_DURABILITY) {
			int nearestArmor = getNearestBonus(self, world, BonusType.REPAIR_KIT);
			if (nearestArmor != -1) {
				return nearestArmor;
			}
		}
		if (relativeHealth >= STABLE_HEALTH && relativeDurability >= STABLE_DURABILITY) {
			int nearestWeapon = getNearestBonus(self, world, BonusType.AMMO_CRATE);
			if (nearestWeapon != -1) {
				return nearestWeapon;
			}
		}
    	return getNearestBonus(self, world);
	}
	
	private void drive(Tank self, Move move, Point point) {
		drive(self, move, point.x, point.y);
	}
	
	private void drive(Tank self, Move move, double x, double y) {
		if (self.getDistanceTo(x, y) < self.getHeight()) {
			return;
		}
		double angle = self.getAngleTo(x, y);    		
		if (-PI/2 <= angle && angle <= PI/2) {
			if (angle > MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(1);
				move.setRightTrackPower(-1);
			} else if (angle < -MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(-1);
				move.setRightTrackPower(1);
			} else {
				driveForward(move);
			}
		} else {
			if (0 > angle && angle > -PI + MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(1);
				move.setRightTrackPower(-1);
			} else if (0 < angle && angle < PI - MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(-1);
				move.setRightTrackPower(1);
			} else {
				driveBackward(move);
			}
		}	}
	
	private boolean existObstacle(Tank self, int enemyIndex, World world) {
		Tank[] tanks = world.getTanks();
		for (int i = 0; i < tanks.length; ++i) {
			if (i == enemyIndex) {
				continue;
			}
			Tank tank = tanks[i];
			double angle = self.getTurretAngleTo(tank);
			if (!tank.isTeammate() && isAlive(tank)) {
				continue;
			}
//			if (Math.abs(angle) < MIN_SHOOT_ANGLE/3) {
//				return true;
//			}
//			if (self.getDistanceTo(tank) < self.getHeight() && Math.abs(angle) < PI/6) {
//				return true;
//			}
		}
//		for (Bonus bonus: world.getBonuses()) {
//			if (Math.abs(self.getTurretAngleTo(bonus)) < MIN_SHOOT_ANGLE/3) {
//				return true;
//			}
//		}
		return false;
	}
	
// TODO
//	private ArrayList<Tank> getShootingEnemies(Tank self, World world) {
//		ArrayList<Tank> res = new ArrayList<Tank>();
//		for (Tank tank: world.getTanks()) {
//			if (tank.isTeammate() || !isAlive(tank)) {
//				continue;
//			}
//			if (Math.abs(tank.getTurretAngleTo(self)) < MIN_SHOOT_ANGLE && tank.getRemainingReloadingTime() < 10) {
//				res.add(tank);
//			}
//		}
//		return res;
//	}
	
	// FIXME: these two methods are wrong
	private boolean shouldHideForward(Tank self, World world, List<Shell> shells) {
		if (self.getY() < 2*self.getHeight() && Math.abs(self.getAngle() - PI/2) < PI/6) {
			return true;
		}
		if (self.getY() > world.getHeight() - 2*self.getHeight() && Math.abs(-PI/2 - self.getAngle()) < PI/6) {
			return false;
		}
		int directShells = 0;
		for (Shell shell: shells) {
			double angle = shell.getAngleTo(self);
			//System.out.println("angle = " + PI / angle + "; dist = " + shell.getDistanceTo(self));
			if (angle > 0) {
				++directShells;
			}
		}
		return directShells >= shells.size() - directShells;
	}
	
	private boolean shouldHideForwardFromTargeting(Tank self, World world, List<Tank> enemies) {
		if (self.getY() < 2*self.getHeight() && Math.abs(self.getAngle() - PI/2) < PI/6) {
			return true;
		}
		if (self.getY() > world.getHeight() - 2*self.getHeight() && Math.abs(-PI/2 - self.getAngle()) < PI/6) {
			return false;
		}
		int directShells = 0;
		for (Tank tank: enemies) {
			double angle = tank.getTurretAngleTo(self);
			//System.out.println("angle = " + PI / angle + "; dist = " + shell.getDistanceTo(self));
			if (angle > 0) {
				++directShells;
			}
		}
		return directShells >= enemies.size() - directShells;
	}
		
    private List<Shell> getDangerShells(Tank self, World world) {
    	List<Shell> res = new ArrayList<Shell>();
    	for (Shell shell: world.getShells()) {
    		double angle = shell.getAngleTo(self);
    		double dist = shell.getDistanceTo(self);
    		if (Math.abs(angle) < MAX_TARGET_ANGLE || (Math.abs(angle) < PI/2 && dist * Math.sin(angle) < self.getHeight())) {
    			res.add(shell);
    		}
    	}
    	return res;
	}	
    // FIXME: wrong
	private void avoidDanger(Tank self, World world, Move move, List<Shell> dangerShells) {
		//System.out.println("avoid danger, tick = " + world.getTick());
		if (shouldHideForward(self, world, dangerShells)) {
			//System.out.println("forward");
			driveForward(move);
		} else {
			//System.out.println("backward");
			driveBackward(move);
		}
	}
	
	private List<Tank> getTargetingEnemies(Tank self, World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank: getAliveEnemies(world)) {
			double angle = tank.getTurretAngleTo(self);
			if (Math.abs(angle) < MAX_SHELL_ANGLE) {
				res.add(tank);
			}
		}
		return res;
	}
	
    // FIXME: wrong
	private void avoidTargeting(Tank self, World world, Move move, List<Tank> targetingEnemies) {
		if (shouldHideForwardFromTargeting(self, world, targetingEnemies)) {
			driveForward(move);
		} else {
			driveBackward(move);
		}
	}
	
	private Tank getCloserEnemy(Tank self, World world, Point point) {
		double selfDist = self.getDistanceTo(point.x, point.y);
		for (Tank tank: world.getTanks()) {
			if (isAlive(tank) && tank.getDistanceTo(point.x, point.y) < selfDist) {
				return tank;
			}
		}
		return null;
	}
	
	private Point getNearestFreeCorner(Tank self, World world) {
		Point[] corners = new Point[] {new Point(XMIN, YMIN), 
									   new Point(XMIN, world.getHeight() - YMIN), 
									   new Point(world.getWidth() - XMIN, YMIN), 
									   new Point(world.getWidth() - XMIN, world.getHeight() - YMIN)};
		Arrays.sort(corners, new TankDistComparator(self));
		for (Point corner: corners) {
			Tank tank = getCloserEnemy(self, world, corner);
			if (tank == null || tank.getDistanceTo(corner.x, corner.y) - self.getDistanceTo(corner.x, corner.y) < 200) {
				return corner;
			}
		}
		return null;
	}
	
	private Point getNearestWall(Tank self, World world) {
		List<Point> walls = new ArrayList<Point>();
		walls.add(new Point(XMIN, world.getHeight() / 2));
		walls.add(new Point(world.getWidth() - XMIN, world.getHeight() / 2));
		walls.add(new Point(world.getWidth() / 2, YMIN));
		walls.add(new Point(world.getWidth() / 2, world.getHeight() - YMIN));
		
		Collections.sort(walls, new TankDistComparator(self));
		for (Point wall: walls) {
			if (getCloserEnemy(self, world, wall) == null) {
				return wall;
			}
		}
		return walls.get(0);
	}
	
	private List<Tank> getAliveTanks(World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank: world.getTanks()) {
			if (isAlive(tank)) {
				res.add(tank);
			}
		}
		return res;
	}
	
	private List<Tank> getAliveEnemies(World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank: getAliveTanks(world)) {
			if (!tank.isTeammate()) {
				res.add(tank);
			}
		}
		return res;
	}	
	private boolean shouldChangeState(Tank self, World world) {
		if (self.getCrewHealth() < self.getCrewMaxHealth() * 0.66) {
			return true;
		}
		if (self.getHullDurability() < self.getHullMaxDurability() * 0.66) {
			return true;
		}
		List<Tank> alive = getAliveTanks(world);
		if (alive.size() <= 4) {
			return true;
		}
		return false;
	}
	
	private void selectShootMove(Tank self, World world, Move move) {
		Tank[] tanks = world.getTanks();
    	int smallestAngleEnemy = getSmallestAngleEnemy(self, world);
    	int nearestEnemy = getNearestEnemy(self, world);
    	int dyingEnemy = getDyingEnemy(world);
    	
    	int enemyIndex = -1;
    	if (dyingEnemy != -1) {
    		enemyIndex = dyingEnemy;
    	} else if (smallestAngleEnemy == nearestEnemy) {
    		enemyIndex = nearestEnemy;
    	} else if (nearestEnemy == -1) {
    		enemyIndex = smallestAngleEnemy;
    	} else if (smallestAngleEnemy == -1) {
    		enemyIndex = nearestEnemy;
    	} else {
    		double nearestEnemyDist = self.getDistanceTo(tanks[nearestEnemy]);
    		if (nearestEnemyDist < MIN_SHOOT_DIST) {
    			enemyIndex = nearestEnemy;
    		} else {
    			enemyIndex = smallestAngleEnemy;
    		}
    	}
    	
    	if (enemyIndex != -1) {
    		Tank enemy = tanks[enemyIndex];
    		double enemyAngle = self.getTurretAngleTo(enemy);
    		double enemyDist = self.getDistanceTo(enemy);
    		if (enemyAngle > MIN_SHOOT_ANGLE) {
    			move.setTurretTurn(1);
    		} else if (enemyAngle < -MIN_SHOOT_ANGLE) {
    			move.setTurretTurn(-1);
    		} else if (!existObstacle(self, enemyIndex, world)) {
    			if (enemyDist > MAX_PREMIUM_SHOOT_DIST) {
    				move.setFireType(FireType.REGULAR);
    			} else {
			        move.setFireType(FireType.PREMIUM_PREFERRED);
    			}
    		}
    	}
	}
	
	private void selectDriveMove(Tank self, World world, Move move) {
		if (state == States.Init) {
			Point corner = getNearestFreeCorner(self, world);
			if (corner == null) {
				Point wall = getNearestWall(self, world);
				drive(self, move, wall);
			} else {
				drive(self, move, corner);
			}
			if (shouldChangeState(self, world)) {
				state = States.Walk;
			}
			return;
		} 
		if (state == States.Walk) {
	    	int bonusIndex = getImportantBonus(self, world);
	    	List<Shell> dangerShells = getDangerShells(self, world);
	    	List<Tank> targetingEnemies = getTargetingEnemies(self, world);
	    	//if (!dangerShells.isEmpty()) {
	    		//avoidDanger(self, world, move, dangerShells);
	    	//} else if (!targetingEnemies.isEmpty()) {
	    		//avoidTargeting(self, world, move, targetingEnemies);
	    	//} else 
	    		if (bonusIndex != -1) {
	    		Bonus bonus = world.getBonuses()[bonusIndex];
	    		drive(self, move, bonus.getX(), bonus.getY());
	    	} else {
	    		double randX = Math.random() * world.getWidth();
	    		double randY = Math.random() * world.getHeight();
	    		drive(self, move, randX, randY);
	    	}
		}	}


	@Override
    public void move(Tank self, World world, Move move) {
    	//System.out.println("tick: " + world.getTick());
    	selectShootMove(self, world, move);
    	selectDriveMove(self, world, move);
    }

    @Override
    public TankType selectTank(int tankIndex, int teamSize) {
        return TankType.MEDIUM;
    }
}
