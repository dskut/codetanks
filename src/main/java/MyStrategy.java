
import model.*;

import static java.lang.StrictMath.PI;

public final class MyStrategy implements Strategy {
	private static double MIN_SHOOT_ANGLE = PI / 180.0;
	private static double MIN_DRIVE_ANGLE = PI / 6.0;
	private static double MIN_SHOOT_DIST = 500;
	private static double MIN_HEALTH = 0.3;
	private static double STABLE_HEALTH = 0.6;
	private static double MIN_DURABILITY = 0.3;
	private static double STABLE_DURABILITY = 0.6;
	private static double MAX_PREMIUM_SHOOT_DIST = 500;
	
	private int getSmallestAngleEnemy(Tank self, World world) {
		int index = -1;
		double minAngle = 1e9;
    	Tank[] tanks = world.getTanks();
    	for (int i = 0; i < tanks.length; ++i) {
    		Tank tank = tanks[i];
    		if (!tank.isTeammate() && tank.getCrewHealth() > 0) {
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
    		if (!tank.isTeammate() && tank.getCrewHealth() > 0) {
    			double dist = self.getDistanceTo(tank);
    			if (dist < minDist) {
    				minDist = dist;
    				index = i;
    			}
    		}
    	}
    	return index;
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
	
	private void drive(Tank self, Move move, double x, double y) {
		double angle = self.getAngleTo(x, y);    		
		if (-PI/2 <= angle && angle <= PI/2) {
			if (angle > MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(0.75);
				move.setRightTrackPower(-1);
			} else if (angle < -MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(-1);
				move.setRightTrackPower(0.75);
			} else {
				move.setLeftTrackPower(1);
				move.setRightTrackPower(1);
			}
		} else {
			if (0 > angle && angle > -PI + MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(0.75);
				move.setRightTrackPower(-1);
			} else if (0 < angle && angle < PI - MIN_DRIVE_ANGLE) {
				move.setLeftTrackPower(-1);
				move.setRightTrackPower(0.75);
			} else {
				move.setLeftTrackPower(-1);
				move.setRightTrackPower(-1);
			}
		}	}
	
    @Override
    public void move(Tank self, World world, Move move) {
    	Tank[] tanks = world.getTanks();
    	int smallestAngleEnemy = getSmallestAngleEnemy(self, world);
    	int nearestEnemy = getNearestEnemy(self, world);
    	
    	int enemyIndex = -1;
    	if (smallestAngleEnemy == nearestEnemy) {
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
    		} else {
    			if (enemyDist > MAX_PREMIUM_SHOOT_DIST) {
    				move.setFireType(FireType.REGULAR);
    			} else {
			        move.setFireType(FireType.PREMIUM_PREFERRED);
    			}
    		}
    	}
    	
    	//double yborder = self.getY() < world.getHeight() / 2 ? 50 : world.getHeight() - 50;
    	//double xborder = self.getX() < world.getWidth() / 2 ? 50 : world.getWidth() - 50;
    		
    	
    	int bonusIndex = getImportantBonus(self, world);
    	if (bonusIndex != -1) {
    		Bonus bonus = world.getBonuses()[bonusIndex];
    		drive(self, move, bonus.getX(), bonus.getY());
    	} else {
    		double randX = Math.random() * world.getWidth();
    		double randY = Math.random() * world.getHeight();
    		drive(self, move, randX, randY);
    	}
    }

    @Override
    public TankType selectTank(int tankIndex, int teamSize) {
        return TankType.MEDIUM;
    }
}
