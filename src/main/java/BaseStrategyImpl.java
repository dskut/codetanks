
import model.*;

import java.util.*;

import static java.lang.StrictMath.PI;

class Point {
	public double x;
	public double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}
}

class Rectangle {
	public Point[] Points;
	
	public Rectangle(Point p1, Point p2, Point p3, Point p4) {
		Points = new Point[] {p1, p2, p3, p4};
	}
}

class TankDistComparator implements Comparator<Point> {
	protected Tank tank;

	public TankDistComparator(Tank tank) {
		this.tank = tank;
	}

	public int compare(Point first, Point second) {
		double diff = tank.getDistanceTo(first.x, first.y)	- tank.getDistanceTo(second.x, second.y);
		return (int) diff;
	}
}

class EnemiesComparator implements Comparator<Tank> {
	private static final double MIN_ENEMY_HEALTH = 0.4;
	private static final double MIN_SHOOT_DIST = 500;
	private static final double CRITICAL_DIST = 100;
	private Tank self;

	public EnemiesComparator(Tank self) {
		this.self = self;
	}

	// 1 mins first is better to shoot, -1 otherwise
	public int compare(Tank first, Tank second) {
		double firstDist = self.getDistanceTo(first);
		double secondDist = self.getDistanceTo(second);
		if (firstDist < CRITICAL_DIST) {
			return 1;
		}
		if (secondDist < CRITICAL_DIST) {
			return -1;
		}

		double firstHealth = (double) first.getCrewHealth() / first.getCrewMaxHealth();
		double secondHealth = (double) second.getCrewHealth()	/ second.getCrewMaxHealth();
		if (firstHealth <= MIN_ENEMY_HEALTH && secondHealth > MIN_ENEMY_HEALTH) {
			return 1;
		} else if (firstHealth > MIN_ENEMY_HEALTH && secondHealth <= MIN_ENEMY_HEALTH) {
			return -1;
		}

		double firstAngle = Math.abs(self.getTurretAngleTo(first));
		double secondAngle = Math.abs(self.getTurretAngleTo(second));
		if (firstDist <= MIN_SHOOT_DIST && secondDist <= MIN_SHOOT_DIST) {
			return (int) Math.ceil(secondAngle - firstAngle);
		}
		return (int) Math.ceil(secondDist - firstDist);
	}
}
public class BaseStrategyImpl {
	protected static final double MIN_SHOOT_ANGLE = PI / 180;
	protected static final double MIN_DRIVE_ANGLE = PI / 6;
	protected static final double MIN_HEALTH = 0.45;
	protected static final double STABLE_HEALTH = 0.6;
	protected static final double MIN_DURABILITY = 0.45;
	protected static final double STABLE_DURABILITY = 0.6;
	protected static final double MAX_PREMIUM_SHOOT_DIST = 500;
	protected static final double XMIN = 20;
	protected static final double YMIN = 20;
	protected static final double MAX_BONUS_DIST = 600;
	protected static final double SHELTER_DIST = 200;

    protected Tank self;
    protected World world;
    protected Move move;
    protected State state;
    
    public BaseStrategyImpl(Tank self, World world, Move move, State state) {
        this.self = self;
        this.world = world;
        this.move = move;
        this.state = state;
    }
    
	protected void driveForward() {
		move.setLeftTrackPower(1);
		move.setRightTrackPower(1);
	}

	protected void driveBackward() {
		move.setLeftTrackPower(-1);
		move.setRightTrackPower(-1);
	}

	protected static boolean isAlive(Tank tank) {
		return tank.getCrewHealth() > 0 && tank.getHullDurability() > 0;
	}
	
	protected Rectangle getCoordinates(Unit unit) {
		double xc = unit.getX();
		double yc = unit.getY();
		double a = unit.getAngle();
		double w = unit.getWidth();
		double h = unit.getHeight();
		
		double x1 = xc + w / 2 * Math.sin(a) - h / 2 * Math.cos(a);
		double y1 = yc + h / 2 * Math.sin(a) + w / 2 * Math.cos(a);
		Point p1 = new Point(x1, y1);
		
		double x2 = xc + w / 2 * Math.sin(a) + h / 2 * Math.cos(a);
		double y2 = yc - h / 2 * Math.sin(a) + w / 2 * Math.cos(a);
		Point p2 = new Point(x2, y2);
		
		double x3 = xc - w / 2 * Math.sin(a) + h / 2 * Math.cos(a);
		double y3 = yc - h / 2 * Math.sin(a) - w / 2 * Math.cos(a);
		Point p3 = new Point(x3, y3);
		
		double x4 = xc - w / 2 * Math.sin(a) - h / 2 * Math.cos(a);
		double y4 = yc + h / 2 * Math.sin(a) - w / 2 * Math.cos(a);
		Point p4 = new Point(x4, y4);
		
		return new Rectangle(p1, p2, p3, p4);
	}
	
	protected boolean isInRange(double a, double b, double x) {
		return a <= x && x <= b;
	}

	protected boolean isInBoundsX(double x) {
	    return isInRange(0, world.getWidth(), x);
	}
	
	protected boolean isInBoundsY(double y) {
	    return isInRange(0, world.getHeight(), y);
	}
	
	protected int getNearestBonus() {
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

	protected int getNearestBonus(BonusType type) {
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

	protected int getImportantBonus() {
		double relativeHealth = (double) self.getCrewHealth()	/ self.getCrewMaxHealth();
		if (relativeHealth < MIN_HEALTH) {
			int nearestMedicine = getNearestBonus(BonusType.MEDIKIT);
			if (nearestMedicine != -1) {
				return nearestMedicine;
			}
		}
		double relativeDurability = (double) self.getHullDurability() 	/ self.getHullMaxDurability();
		if (relativeDurability < MIN_DURABILITY) {
			int nearestArmor = getNearestBonus(BonusType.REPAIR_KIT);
			if (nearestArmor != -1) {
				return nearestArmor;
			}
		}
		if (relativeHealth >= STABLE_HEALTH	&& relativeDurability >= STABLE_DURABILITY && self.getPremiumShellCount() < 2) {
			int nearestWeapon = getNearestBonus(BonusType.AMMO_CRATE);
			if (nearestWeapon != -1) {
				return nearestWeapon;
			}
		}
		int index = getNearestBonus();
		if (index != -1 && self.getDistanceTo(world.getBonuses()[index]) < MAX_BONUS_DIST) {
			return index;
		}
		return -1;
	}

	protected void drive(double x, double y, double frontPower, double rearPower) {
		if (self.getDistanceTo(x, y) < self.getHeight() / 2) {
			return;
		}
		double angle = self.getAngleTo(x, y);
		if (-PI / 2 <= angle && angle <= PI / 2) {
			if (angle > MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(frontPower);
				move.setRightTrackPower(rearPower);
			} else if (angle < -MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(rearPower);
				move.setRightTrackPower(frontPower);
			} else {
				driveForward();
			}
		} else {
			if (0 > angle && angle > -PI + MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(frontPower);
				move.setRightTrackPower(rearPower);
			} else if (0 < angle && angle < PI - MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(rearPower);
				move.setRightTrackPower(frontPower);
			} else {
				driveBackward();
			}
		}
	}
	
	protected void drive(double x, double y) {
		drive(x, y, 0.75, -1);
	}
	
	protected void drive(Point point) {
		drive(point.x, point.y);
	}
	
	protected void drive(Unit unit) {
		drive(unit.getX(), unit.getY());
	}
	
	protected void quickDrive(double x, double y) {
		drive(x, y, 1, -0.75);
	}
	
	protected void quickDrive(Point p) {
		quickDrive(p.x, p.y);
	}
	protected boolean isRearToWall() {
		double x = self.getX() - self.getHeight() * Math.cos(self.getAngle());
		double y = self.getY() - self.getHeight() * Math.sin(self.getAngle());
		return !isInRange(0, world.getWidth(), x) || !isInRange(0, world.getHeight(), y);
	}
	
	protected boolean isFrontToWall() {
		double x = self.getX() + self.getHeight() * Math.cos(self.getAngle());
		double y = self.getY() + self.getHeight() * Math.sin(self.getAngle());
		return !isInRange(0, world.getWidth(), x) || !isInRange(0, world.getHeight(), y);
	}

	protected List<Shell> getDangerShells() {
		List<Shell> res = new ArrayList<Shell>();
		for (Shell shell : world.getShells()) {
			double angle = shell.getAngleTo(self);
			double dist = shell.getDistanceTo(self);
			if (Math.abs(angle) < PI/2 && Math.abs(dist*Math.sin(angle)) < self.getHeight()) {
				res.add(shell);
			}
		}
		return res;
	}
	
	Shell getNearestShell(List<Shell> shells) {
	    Shell res = shells.get(0);
	    for (Shell shell: shells) {
	        if (self.getDistanceTo(shell) < self.getDistanceTo(res)) {
	            res = shell;
	        }
	    }
	    return res;
	}
	
	protected List<Tank> getTargetingEnemies() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveEnemies()) {
			double angle = tank.getTurretAngleTo(self);
			if (Math.abs(angle) < 4*MIN_SHOOT_ANGLE) {
				res.add(tank);
			}
		}
		return res;
	}
	protected Point getNearestWall() {
		List<Point> walls = new ArrayList<Point>();
		walls.add(new Point(XMIN, world.getHeight() / 2));
		walls.add(new Point(world.getWidth() - XMIN, world.getHeight() / 2));
		walls.add(new Point(world.getWidth() / 2, YMIN));
		walls.add(new Point(world.getWidth() / 2, world.getHeight() - YMIN));

		Collections.sort(walls, new TankDistComparator(self));
		for (Point wall : walls) {
			if (getCloserEnemy(wall) == null) {
				return wall;
			}
		}
		return walls.get(0);
	}

	protected List<Tank> getAliveTanks() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : world.getTanks()) {
			if (isAlive(tank)) {
				res.add(tank);
			}
		}
		return res;
	}
	
	protected static List<Tank> getAliveTanks(World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : world.getTanks()) {
			if (isAlive(tank)) {
				res.add(tank);
			}
		}
		return res;
	}

	protected List<Tank> getDeadTanks() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : world.getTanks()) {
			if (!isAlive(tank)) {
				res.add(tank);
			}
		}
		return res;
	}

	protected List<Tank> getAliveEnemies() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveTanks()) {
			if (!tank.isTeammate()) {
				res.add(tank);
			}
		}
		return res;
	}
	
	protected List<Tank> getAliveTeammates() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveTanks()) {
			if (tank.isTeammate() && tank.getId() != self.getId()) {
				res.add(tank);
			}
		}
		return res;
	}
	
    public static int getAliveTeammates(Tank self, World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveTanks(world)) {
			if (tank.isTeammate() && tank.getId() != self.getId()) {
				res.add(tank);
			}
		}
		return res.size();
    }

	protected List<Tank> getAliveTeam() {
	    List<Tank> team = getAliveTeammates();
	    team.add(self);
	    return team;
	}

	protected List<Unit> getObstacles() {
		List<Unit> res = new ArrayList<Unit>();
		res.addAll(getDeadTanks());
		res.addAll(Arrays.asList(world.getBonuses()));
		return res;
	}
	
	

	protected boolean isObstacle(Tank enemy, Unit obstacle) {
		if (self.getDistanceTo(enemy) < self.getDistanceTo(obstacle)) {
			return false;
		}
		double enemyAngle = self.getAngleTo(enemy);
		double[] obstacleAngles = getSortedAngles(obstacle);
		double minObstacleAngle = obstacleAngles[0];
		double maxObstacleAngle = obstacleAngles[3];
		if (maxObstacleAngle - minObstacleAngle > PI) {
		    return enemyAngle <= minObstacleAngle || enemyAngle >= maxObstacleAngle;
		} else {
		    return minObstacleAngle <= enemyAngle && enemyAngle <= maxObstacleAngle;
		}
	}

	protected boolean existObstacle(Tank enemy, List<Unit> obstacles) {
		for (Unit obstacle : obstacles) {
			if (isObstacle(enemy, obstacle)) {
				return true;
			}
		}
		return false;
	}

	protected List<Tank> selectOpenEnemies(List<Tank> enemies) {
		List<Tank> res = new ArrayList<Tank>();
		List<Unit> obstacles = getObstacles();
		obstacles.addAll(getAliveTeammates());
		for (Tank enemy : enemies) {
			if (!existObstacle(enemy, obstacles)) {
				res.add(enemy);
			}
		}
		return res;
	}
	
	protected double[] getSortedAngles(Unit unit) {
		Rectangle rect = getCoordinates(unit);
		double a1 = self.getAngleTo(rect.Points[0].x, rect.Points[0].y);
		double a2 = self.getAngleTo(rect.Points[1].x, rect.Points[1].y);
		double a3 = self.getAngleTo(rect.Points[2].x, rect.Points[2].y);
		double a4 = self.getAngleTo(rect.Points[3].x, rect.Points[3].y);
		double[] angles = new double[] {a1, a2, a3, a4};
		Arrays.sort(angles);
		return angles;
	}
	
	protected double[] getSortedTurretAngles(Unit unit) {
		Rectangle rect = getCoordinates(unit);
		double a1 = self.getTurretAngleTo(rect.Points[0].x, rect.Points[0].y);
		double a2 = self.getTurretAngleTo(rect.Points[1].x, rect.Points[1].y);
		double a3 = self.getTurretAngleTo(rect.Points[2].x, rect.Points[2].y);
		double a4 = self.getTurretAngleTo(rect.Points[3].x, rect.Points[3].y);
		double[] angles = new double[] {a1, a2, a3, a4};
		Arrays.sort(angles);
		return angles;
	}
	
	protected Tank getCloserEnemy(Point point) {
		double selfDist = self.getDistanceTo(point.x, point.y);
		for (Tank tank : getAliveEnemies()) {
			if (tank.getDistanceTo(point.x, point.y) < selfDist) {
				return tank;
			}
		}
		return null;
	}
	
	protected Tank getCloserTeammate(Point point) {
		double selfDist = self.getDistanceTo(point.x, point.y);
		for (Tank tank : getAliveTeammates()) {
			if (tank.getDistanceTo(point.x, point.y) < selfDist) {
				return tank;
			}
		}
		return null;
	}
	protected Point[] getCorners() {
		return new Point[] { new Point(XMIN, YMIN),
							 new Point(XMIN, world.getHeight() - YMIN),
							 new Point(world.getWidth() - XMIN, YMIN),
							 new Point(world.getWidth() - XMIN, world.getHeight() - YMIN) };
	}
	
	protected Point getNearestCorner() {
		Point[] corners = getCorners();
		Arrays.sort(corners, new TankDistComparator(self));
		return corners[0];
	}
	
	protected Point getNearestFreeCorner() {
		Point[] corners = getCorners();
		Arrays.sort(corners, new TankDistComparator(self));
		for (Point corner : corners) {
		    if (getCloserTeammate(corner) != null) {
		        continue;
		    }
			Tank tank = getCloserEnemy(corner);
			if (tank == null || self.getDistanceTo(corner.x, corner.y) - tank.getDistanceTo(corner.x, corner.y) < 100) {
				return corner;
			}
		}
		return null;
	}
		protected Point getNearestCornerWithoutTeammate() {
		Point[] corners = getCorners();
		Arrays.sort(corners, new TankDistComparator(self));
		for (Point corner : corners) {
		    if (getCloserTeammate(corner) == null) {
		        return corner;
		    }
		}
		return null;
	}
		
	// FIXME: must observe enemy's movement
	protected int selectTurretMove(Tank enemy) {
		double angle = self.getTurretAngleTo(enemy);
		if (angle > MIN_SHOOT_ANGLE) {
		    return 1;
		} else if (angle < -MIN_SHOOT_ANGLE) {
		    return -1;
		} else {
		    return 0;
		}
	}
		
	protected void turnTurretTo(Tank enemy) {
	    int turretMove = selectTurretMove(enemy);
	    if (turretMove != 0) {
	        move.setTurretTurn(turretMove);
	    }
	}

	protected void tryShoot(Tank enemy) {
	    int turretMove = selectTurretMove(enemy);
		if (turretMove == 0) {
			double dist = self.getDistanceTo(enemy);
			if (dist > MAX_PREMIUM_SHOOT_DIST) {
				move.setFireType(FireType.REGULAR);
			} else {
				move.setFireType(FireType.PREMIUM_PREFERRED);
			}
		} else {
		    move.setTurretTurn(turretMove);
		}
	}
	
	protected Tank getNearestTank(List<Tank> tanks) {
		Tank res = tanks.get(0);
		for (Tank tank: tanks) {
			if (self.getDistanceTo(tank) < self.getDistanceTo(res)) {
				res = tank;
			}
		}
		return res;
	}
	
	protected Point findShelter(Tank enemy) {
		List<Tank> deads = getDeadTanks();
		if (deads.isEmpty()) {
			return null;
		}
		Tank shelter = getNearestTank(deads);
		double angle = enemy.getAngleTo(shelter);
		double x = shelter.getX() + SHELTER_DIST * Math.cos(angle);
		double y = shelter.getY() + SHELTER_DIST * Math.sin(angle);
		return new Point(x, y);
	}
	
	protected void avoidDanger(List<Shell> dangerShells) {
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

	protected void avoidTargeting(List<Tank> targetingEnemies) {
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
	}    protected boolean isStronger(Tank first, Tank second) {
        if (first.getCrewHealth() != second.getCrewHealth()) {
            return first.getCrewHealth() > second.getCrewHealth();
        }
        if (first.getHullDurability() != second.getHullDurability()) {
            return first.getHullDurability() > second.getHullDurability();
        }
        return first.getRemainingReloadingTime() < second.getRemainingReloadingTime();
    }
    
    protected List<Tank> getStrongerTeammates(Tank enemy) {
        List<Tank> teammates = getAliveTeammates();
        List<Tank> res = new ArrayList<Tank>();
        for (Tank teammate: teammates) {
            if (isStronger(teammate, enemy)) {
                res.add(teammate);
            }
        }
        return res;
    }
    
    protected boolean isOneOnOne() {
        return getAliveEnemies().size() == 1 && getAliveTeammates().size() == 0;
    }
    
    protected boolean isTwoOnOne() {
        return getAliveEnemies().size() == 1 && getAliveTeammates().size() == 1;
    }
}
