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
		double diff = tank.getDistanceTo(first.x, first.y)
				- tank.getDistanceTo(second.x, second.y);
		return (int) diff;
	}
}

class EnemiesComparator implements Comparator<Tank> {
	private static final double MIN_ENEMY_HEALTH = 0.4;
	private static final double MIN_SHOOT_DIST = 500;
	private Tank self;

	public EnemiesComparator(Tank self) {
		this.self = self;
	}

	// 1 mins first is better to shoot, -1 otherwise
	public int compare(Tank first, Tank second) {
		double firstHealth = (double) first.getCrewHealth()
				/ first.getCrewMaxHealth();
		double secondHealth = (double) second.getCrewHealth()
				/ second.getCrewMaxHealth();
		if (firstHealth <= MIN_ENEMY_HEALTH && secondHealth > MIN_ENEMY_HEALTH) {
			return 1;
		} else if (firstHealth > MIN_ENEMY_HEALTH
				&& secondHealth <= MIN_ENEMY_HEALTH) {
			return -1;
		}

		double firstDist = self.getDistanceTo(first);
		double secondDist = self.getDistanceTo(second);
		double firstAngle = Math.abs(self.getTurretAngleTo(first));
		double secondAngle = Math.abs(self.getTurretAngleTo(second));
		if (firstDist <= MIN_SHOOT_DIST && secondDist <= MIN_SHOOT_DIST) {
			return (int) Math.ceil(secondAngle - firstAngle);
		}
		return (int) Math.ceil(secondDist - firstDist);
	}
}

enum States {
	Init, // in the start phase - sit in corner
	Walk, // then - walk around map
}

public final class MyStrategy implements Strategy {
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
		double relativeHealth = (double) self.getCrewHealth()	/ self.getCrewMaxHealth();
		if (relativeHealth < MIN_HEALTH) {
			int nearestMedicine = getNearestBonus(self, world, BonusType.MEDIKIT);
			if (nearestMedicine != -1) {
				return nearestMedicine;
			}
		}
		double relativeDurability = (double) self.getHullDurability() 	/ self.getHullMaxDurability();
		if (relativeDurability < MIN_DURABILITY) {
			int nearestArmor = getNearestBonus(self, world, BonusType.REPAIR_KIT);
			if (nearestArmor != -1) {
				return nearestArmor;
			}
		}
		if (relativeHealth >= STABLE_HEALTH	&& relativeDurability >= STABLE_DURABILITY && self.getPremiumShellCount() < 2) {
			int nearestWeapon = getNearestBonus(self, world, BonusType.AMMO_CRATE);
			if (nearestWeapon != -1) {
				return nearestWeapon;
			}
		}
		int index = getNearestBonus(self, world);
		if (index != -1 && self.getDistanceTo(world.getBonuses()[index]) < MAX_BONUS_DIST) {
			return index;
		}
		return -1;
	}

	private void drive(Tank self, Move move, Point point) {
		drive(self, move, point.x, point.y);
	}

	private void drive(Tank self, Move move, double x, double y, double frontPower, double rearPower) {
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
				driveForward(move);
			}
		} else {
			if (0 > angle && angle > -PI + MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(frontPower);
				move.setRightTrackPower(rearPower);
			} else if (0 < angle && angle < PI - MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(rearPower);
				move.setRightTrackPower(frontPower);
			} else {
				driveBackward(move);
			}
		}
	}
	
	private void drive(Tank self, Move move, double x, double y) {
		drive(self, move, x, y, 0.75, -1);
	}
	
	private void quickDrive(Tank self, Move move, double x, double y) {
		drive(self, move, x, y, 1, -0.75);
	}
	
	private void quickDrive(Tank self, Move move, Point p) {
		quickDrive(self, move, p.x, p.y);
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

	private boolean shouldHideForwardFromTargeting(Tank self, World world, List<Tank> enemies) {
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
	
	private boolean isRearToWall(Tank self, World world) {
		double x = self.getX() - self.getHeight() * Math.cos(self.getAngle());
		double y = self.getY() - self.getHeight() * Math.sin(self.getAngle());
		return !isInRange(0, world.getWidth(), x) || !isInRange(0, world.getHeight(), y);
	}
	
	private boolean isFrontToWall(Tank self, World world) {
		double x = self.getX() + self.getHeight() * Math.cos(self.getAngle());
		double y = self.getY() + self.getHeight() * Math.sin(self.getAngle());
		return !isInRange(0, world.getWidth(), x) || !isInRange(0, world.getHeight(), y);
	}

	private List<Shell> getDangerShells(Tank self, World world) {
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

	private void avoidDanger(Tank self, World world, Move move, List<Shell> dangerShells) {
		if (isRearToWall(self, world)) {
			driveForward(move);
		} else if (isFrontToWall(self, world)) {
			driveBackward(move);
		} else {
			Shell shell = dangerShells.get(0); // FIXME
			double angle = shell.getAngleTo(self);
			double dist = shell.getDistanceTo(self);
			double dist1 = dist * Math.cos(angle);
			double shellAngle = shell.getAngle();
			double xInter = shell.getX() + dist1 * Math.cos(shellAngle);
			double yInter = shell.getY() + dist1 * Math.sin(shellAngle);			double myAngle = self.getAngleTo(xInter, yInter);
			if (Math.abs(myAngle) > PI/2) {
				driveForward(move);
			} else {
				driveBackward(move);
			}
		}
	}

	private List<Tank> getTargetingEnemies(Tank self, World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveEnemies(world)) {
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
		for (Tank tank : world.getTanks()) {
			if (isAlive(tank) && tank.getDistanceTo(point.x, point.y) < selfDist) {
				return tank;
			}
		}
		return null;
	}

	private Point getNearestFreeCorner(Tank self, World world) {
		Point[] corners = new Point[] { new Point(XMIN, YMIN),
										new Point(XMIN, world.getHeight() - YMIN),
										new Point(world.getWidth() - XMIN, YMIN),
										new Point(world.getWidth() - XMIN, world.getHeight() - YMIN) };
		Arrays.sort(corners, new TankDistComparator(self));
		for (Point corner : corners) {
			Tank tank = getCloserEnemy(self, world, corner);
			if (tank == null || tank.getDistanceTo(corner.x, corner.y)	- self.getDistanceTo(corner.x, corner.y) < 200) {
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
		for (Point wall : walls) {
			if (getCloserEnemy(self, world, wall) == null) {
				return wall;
			}
		}
		return walls.get(0);
	}

	private List<Tank> getAliveTanks(World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : world.getTanks()) {
			if (isAlive(tank)) {
				res.add(tank);
			}
		}
		return res;
	}

	private List<Tank> getDeadTanks(World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : world.getTanks()) {
			if (!isAlive(tank)) {
				res.add(tank);
			}
		}
		return res;
	}

	private List<Tank> getAliveEnemies(World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveTanks(world)) {
			if (!tank.isTeammate()) {
				res.add(tank);
			}
		}
		return res;
	}

	private List<Tank> getAliveTeammates(Tank self, World world) {
		List<Tank> res = new ArrayList<Tank>();
		for (Tank tank : getAliveTanks(world)) {
			if (tank.isTeammate() && tank.getId() != self.getId()) {
				res.add(tank);
			}
		}
		return res;
	}

	private List<Unit> getObstacles(World world) {
		List<Unit> res = new ArrayList<Unit>();
		res.addAll(getDeadTanks(world));
		res.addAll(Arrays.asList(world.getBonuses()));
		return res;
	}
	
	private boolean isInSight(Tank self, Unit obstacle) {
		Rectangle rect = getCoordinates(obstacle);
		double a1 = self.getTurretAngleTo(rect.Points[0].x, rect.Points[0].y);
		double a2 = self.getTurretAngleTo(rect.Points[1].x, rect.Points[1].y);
		double a3 = self.getTurretAngleTo(rect.Points[2].x, rect.Points[2].y);
		double a4 = self.getTurretAngleTo(rect.Points[3].x, rect.Points[3].y);
		double[] angles = new double[] {a1, a2, a3, a4};
		Arrays.sort(angles);
		return angles[0] * angles[3] < 0;
	}

	private boolean isObstacle(Tank self, Tank enemy, Unit obstacle) {
		if (self.getDistanceTo(enemy) < self.getDistanceTo(obstacle)) {
			return false;
		}
		return isInSight(self, obstacle);
	}

	private boolean existObstacle(Tank self, Tank enemy, List<Unit> obstacles) {
		for (Unit obstacle : obstacles) {
			if (isObstacle(self, enemy, obstacle)) {
				return true;
			}
		}
		return false;
	}

	private List<Tank> selectOpenEnemies(Tank self, World world, List<Tank> enemies) {
		List<Tank> res = new ArrayList<Tank>();
		List<Unit> obstacles = getObstacles(world);
		obstacles.addAll(getAliveTeammates(self, world));
		for (Tank enemy : enemies) {
			if (!existObstacle(self, enemy, obstacles)) {
				res.add(enemy);
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

	private void turnTurretTo(Tank self, Move move, Tank enemy) {
		double angle = self.getTurretAngleTo(enemy);
		if (angle > MIN_SHOOT_ANGLE) {
			move.setTurretTurn(1);
		} else if (angle < -MIN_SHOOT_ANGLE) {
			move.setTurretTurn(-1);
		}
	}

	private void tryShoot(Tank self, Move move, Tank enemy) {
		double angle = self.getTurretAngleTo(enemy);
		if (-MIN_SHOOT_ANGLE <= angle && angle <= MIN_SHOOT_ANGLE) {
			double dist = self.getDistanceTo(enemy);
			if (dist > MAX_PREMIUM_SHOOT_DIST) {
				move.setFireType(FireType.REGULAR);
			} else {
				move.setFireType(FireType.PREMIUM_PREFERRED);
			}
		} else {
			turnTurretTo(self, move, enemy);
		}
	}

	private void selectShootMove(Tank self, World world, Move move) {
		List<Tank> enemies = getAliveEnemies(world);
		if (enemies.isEmpty()) {
			// something is wrong!
			return;
		}
		List<Tank> openEnemies = selectOpenEnemies(self, world, enemies);
		if (openEnemies.isEmpty()) {
			Tank enemy = Collections.max(enemies, new EnemiesComparator(self));
			turnTurretTo(self, move, enemy);
			return;
		}
		Tank enemy = Collections.max(openEnemies, new EnemiesComparator(self));
		tryShoot(self, move, enemy);
	}

	private void selectDriveMove(Tank self, World world, Move move) {
		if (state == States.Init) {
			Point corner = getNearestFreeCorner(self, world);
			if (corner == null) {
				Point wall = getNearestWall(self, world);
				quickDrive(self, move, wall);
			} else {
				quickDrive(self, move, corner);
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
			if (!dangerShells.isEmpty()) {
				avoidDanger(self, world, move, dangerShells);
			// } else if (!targetingEnemies.isEmpty()) {
			// avoidTargeting(self, world, move, targetingEnemies);
			} else if (bonusIndex != -1) {
				Bonus bonus = world.getBonuses()[bonusIndex];
				drive(self, move, bonus.getX(), bonus.getY());
			} else {
				drive(self, move, getNearestFreeCorner(self, world));
			}
		}
	}

	@Override
	public void move(Tank self, World world, Move move) {
		// System.out.println("tick: " + world.getTick());
		selectShootMove(self, world, move);
		selectDriveMove(self, world, move);
	}

	@Override
	public TankType selectTank(int tankIndex, int teamSize) {
		return TankType.MEDIUM;
	}
}
