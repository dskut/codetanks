
import java.util.*;
import static java.lang.StrictMath.PI;
import model.*;

enum State {
	Init,
	InCorner,
	Walk,
	OneOnOne,
}

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
	private Tank tank;

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
public class MediumStrategy {
	private static final double MIN_SHOOT_ANGLE = PI / 180;
	private static final double MAX_SHELL_ANGLE = PI / 6;
	private static final double MIN_DRIVE_ANGLE = PI / 6;
	private static final double MIN_HEALTH = 0.3;
	private static final double STABLE_HEALTH = 0.6;
	private static final double MIN_DURABILITY = 0.3;
	private static final double STABLE_DURABILITY = 0.6;
	private static final double MAX_PREMIUM_SHOOT_DIST = 500;
	private static final double XMIN = 20;
	private static final double YMIN = 20;
	private static final double MAX_BONUS_DIST = 600;
	private static final int INIT_TICKS = 60;
	private static final double SHELTER_DIST = 200;
	
    private Tank self;
    private World world;
    private Move move;
    private State state;
    
    public MediumStrategy(Tank self, World world, Move move, State state) {
        this.self = self;
        this.world = world;
        this.move = move;
        this.state = state;
    }
    
    	private void driveForward() {
		move.setLeftTrackPower(1);
		move.setRightTrackPower(1);
	}

	private void driveBackward() {
		move.setLeftTrackPower(-1);
		move.setRightTrackPower(-1);
	}

	private boolean isAlive(Tank tank) {
		return tank.getCrewHealth() > 0 && tank.getHullDurability() > 0;
	}
	
	private Rectangle getCoordinates(Unit unit) {
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
	
	private boolean isInRange(double a, double b, double x) {
		return a <= x && x <= b;
	}

	private int getNearestBonus() {
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

	private int getNearestBonus(BonusType type) {
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

	private int getImportantBonus() {
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

	private void drive(double x, double y, double frontPower, double rearPower) {
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
	
	private void drive(double x, double y) {
		drive(x, y, 0.75, -1);
	}
	
	private void drive(Point point) {
		drive(point.x, point.y);
	}
	
	private void drive(Unit unit) {
		drive(unit.getX(), unit.getY());
	}
	
	private void quickDrive(double x, double y) {
		drive(x, y, 1, -0.75);
	}
	
	private void quickDrive(Point p) {
		quickDrive(p.x, p.y);
	}

	// TODO
	// private ArrayList<Tank> getShootingEnemies(Tank self, World world) {
	// ArrayList<Tank> res = new ArrayList<Tank>();
	// for (Tank tank: world.getTanks()) {
	// if ((tank.isTeammate() && tang.getId() != self.getId()) || !isAlive(tank)) {
	// continue;
	// }
	// if (Math.abs(tank.getTurretAngleTo(self)) < MIN_SHOOT_ANGLE &&
	// tank.getRemainingReloadingTime() < 10) {
	// res.add(tank);
	// }
	// }
	// return res;
	// }

	private boolean shouldHideForwardFromTargeting(List<Tank> enemies) {
		if (self.getY() < 2 * self.getHeight() && Math.abs(self.getAngle() - PI / 2) < PI / 6) {
			return true;
		}
		if (self.getY() > world.getHeight() - 2 * self.getHeight() && Math.abs(-PI / 2 - self.getAngle()) < PI / 6) {
			return false;
		}
		int directShells = 0;
		for (Tank tank : enemies) {
			double angle = tank.getTurretAngleTo(self);
			// System.out.println("angle = " + PI / angle + "; dist = " + shell.getDistanceTo(self));
			if (angle > 0) {
				++directShells;
			}
		}
		return directShells >= enemies.size() - directShells;
	}
	
	private boolean isRearToWall() {
		double x = self.getX() - self.getHeight() * Math.cos(self.getAngle());
		double y = self.getY() - self.getHeight() * Math.sin(self.getAngle());
		return !isInRange(0, world.getWidth(), x) || !isInRange(0, world.getHeight(), y);
	}
	
	private boolean isFrontToWall() {
		double x = self.getX() + self.getHeight() * Math.cos(self.getAngle());
		double y = self.getY() + self.getHeight() * Math.sin(self.getAngle());
		return !isInRange(0, world.getWidth(), x) || !isInRange(0, world.getHeight(), y);
	}

	private List<Shell> getDangerShells() {
		List<Shell> res = new ArrayList<Shell>();
		for (Shell shell : world.getShells()) {
			double angle = shell.getAngleTo(self);
			double dist = shell.getDistanceTo(self);
			if (Math.abs(angle) < PI/2 && dist*Math.sin(angle) < self.getHeight()) {
				res.add(shell);
			}
		}
		return res;
	}    
	private void avoidDanger(List<Shell> dangerShells) {
		if (isRearToWall()) {
			driveForward();
		} else if (isFrontToWall()) {
			driveBackward();
		} else {
			Shell shell = dangerShells.get(0); // FIXME
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

	private List<Tank> getTargetingEnemies() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveEnemies()) {
			double angle = tank.getTurretAngleTo(self);
			if (Math.abs(angle) < MAX_SHELL_ANGLE) {
				res.add(tank);
			}
		}
		return res;
	}

	// FIXME: wrong
	private void avoidTargeting(List<Tank> targetingEnemies) {
		if (shouldHideForwardFromTargeting(targetingEnemies)) {
			driveForward();
		} else {
			driveBackward();
		}
	}

	private Tank getCloserEnemy(Point point) {
		double selfDist = self.getDistanceTo(point.x, point.y);
		for (Tank tank : getAliveEnemies()) {
			if (tank.getDistanceTo(point.x, point.y) < selfDist) {
				return tank;
			}
		}
		return null;
	}

	private Point getNearestFreeCorner() {
		Point[] corners = new Point[] { new Point(XMIN, YMIN),
										new Point(XMIN, world.getHeight() - YMIN),
										new Point(world.getWidth() - XMIN, YMIN),
										new Point(world.getWidth() - XMIN, world.getHeight() - YMIN) };
		Arrays.sort(corners, new TankDistComparator(self));
		for (Point corner : corners) {
			Tank tank = getCloserEnemy(corner);
			if (tank == null || self.getDistanceTo(corner.x, corner.y) - tank.getDistanceTo(corner.x, corner.y) < 100) {
				return corner;
			}
		}
		return null;
	}

	private Point getNearestWall() {
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

	private List<Tank> getAliveTanks() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : world.getTanks()) {
			if (isAlive(tank)) {
				res.add(tank);
			}
		}
		return res;
	}

	private List<Tank> getDeadTanks() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : world.getTanks()) {
			if (!isAlive(tank)) {
				res.add(tank);
			}
		}
		return res;
	}

	private List<Tank> getAliveEnemies() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveTanks()) {
			if (!tank.isTeammate()) {
				res.add(tank);
			}
		}
		return res;
	}

	private List<Tank> getAliveTeammates() {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveTanks()) {
			if (tank.isTeammate() && tank.getId() != self.getId()) {
				res.add(tank);
			}
		}
		return res;
	}

	private List<Unit> getObstacles() {
		List<Unit> res = new ArrayList<Unit>();
		res.addAll(getDeadTanks());
		res.addAll(Arrays.asList(world.getBonuses()));
		return res;
	}
	
	private boolean isInSight(Unit obstacle) {
		Rectangle rect = getCoordinates(obstacle);
		double a1 = self.getTurretAngleTo(rect.Points[0].x, rect.Points[0].y);
		double a2 = self.getTurretAngleTo(rect.Points[1].x, rect.Points[1].y);
		double a3 = self.getTurretAngleTo(rect.Points[2].x, rect.Points[2].y);
		double a4 = self.getTurretAngleTo(rect.Points[3].x, rect.Points[3].y);
		double[] angles = new double[] {a1, a2, a3, a4};
		Arrays.sort(angles);
		return angles[0] * angles[3] < 0;
	}

	private boolean isObstacle(Tank enemy, Unit obstacle) {
		if (self.getDistanceTo(enemy) < self.getDistanceTo(obstacle)) {
			return false;
		}
		return isInSight(obstacle);
	}

	private boolean existObstacle(Tank enemy, List<Unit> obstacles) {
		for (Unit obstacle : obstacles) {
			if (isObstacle(enemy, obstacle)) {
				return true;
			}
		}
		return false;
	}

	private List<Tank> selectOpenEnemies(List<Tank> enemies) {
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

	private boolean shouldWalk() {
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
		if (getTargetingEnemies().size() > 1) {
			return true;
		}
		return false;
	}
	
	private void turnTurretTo(Tank enemy) {
		double angle = self.getTurretAngleTo(enemy);
		if (angle > MIN_SHOOT_ANGLE) {
			move.setTurretTurn(1);
		} else if (angle < -MIN_SHOOT_ANGLE) {
			move.setTurretTurn(-1);
		}
	}

	private void tryShoot(Tank enemy) {
		double angle = self.getTurretAngleTo(enemy);
		if (-MIN_SHOOT_ANGLE <= angle && angle <= MIN_SHOOT_ANGLE) {
			double dist = self.getDistanceTo(enemy);
			if (dist > MAX_PREMIUM_SHOOT_DIST) {
				move.setFireType(FireType.REGULAR);
			} else {
				move.setFireType(FireType.PREMIUM_PREFERRED);
			}
		} else {
			turnTurretTo(enemy);
		}
	}
	
	private Tank getNearestTank(List<Tank> tanks) {
		Tank res = tanks.get(0);
		for (Tank tank: tanks) {
			if (self.getDistanceTo(tank) < self.getDistanceTo(res)) {
				res = tank;
			}
		}
		return res;
	}
	
	private Point findShelter(Tank enemy) {
		List<Tank> deads = getDeadTanks();
		if (deads.isEmpty()) {
			return getNearestFreeCorner();
		}
		Tank shelter = getNearestTank(deads);
		double angle = enemy.getAngleTo(shelter);
		double x = shelter.getX() + SHELTER_DIST * Math.cos(angle);
		double y = shelter.getY() + SHELTER_DIST * Math.sin(angle);
		return new Point(x, y);
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
	
	private void selectDriveMove() {
		if (state == State.Init) {
			driveBackward();
			if (world.getTick() >= INIT_TICKS) {
				state = State.InCorner;
			}
			return;
		}
		if (state == State.InCorner) {
			Point corner = getNearestFreeCorner();
			if (corner == null) {
				Point wall = getNearestWall();
				quickDrive(wall);
			} else {
				quickDrive(corner);
			}
			if (shouldWalk()) {
				state = State.Walk;
			}
			return;
		}
		if (state == State.Walk) {
			int bonusIndex = getImportantBonus();
			List<Shell> dangerShells = getDangerShells();
			List<Tank> targetingEnemies = getTargetingEnemies();
			Point nearestCorner = getNearestFreeCorner();
			if (!dangerShells.isEmpty()) {
				avoidDanger(dangerShells);
			// } else if (!targetingEnemies.isEmpty()) {
			// avoidTargeting(self, world, move, targetingEnemies);
			} else if (bonusIndex != -1) {
				Bonus bonus = world.getBonuses()[bonusIndex];
				drive(bonus);
			} else if (nearestCorner != null) {
				drive(nearestCorner);
			} else {
				drive(getNearestWall());
			}
			if (getAliveEnemies().size() == 1) {
				state = State.OneOnOne;
			}
			return;
		}
		if (state == State.OneOnOne) {
			Tank enemy = getAliveEnemies().get(0);
			List<Shell> dangerShells = getDangerShells();
			int bonusIndex = getImportantBonus();
			if (!dangerShells.isEmpty()) {
				avoidDanger(dangerShells);
			} else if (bonusIndex != -1) {
				Bonus bonus = world.getBonuses()[bonusIndex];
				drive(bonus);
			} else if (enemy.getCrewHealth() < self.getCrewHealth()) {
				drive(enemy);
			} else {
				drive(findShelter(enemy));
			}
			return;
		}
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
