import model.*;

import static java.lang.StrictMath.PI;

public final class MyStrategy implements Strategy {
	private static double MIN_SHOOT_ANGLE = PI / 180.0;
	private static double MIN_BONUS_ANGLE = PI / 6.0;
	private static double MIN_SHOOT_DIST = 100;
	
	private int getSmallestAngleEnemy(Tank self, World world) {
		int index = -1;
		double minAngle = 1e9;
    	Tank[] tanks = world.getTanks();
    	for (int i = 0; i < tanks.length; ++i) {
    		Tank tank = tanks[i];
    		if (!tank.isTeammate()) {
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
    		if (!tank.isTeammate()) {
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
	
    @Override
    public void move(Tank self, World world, Move move) {
    	Tank[] tanks = world.getTanks();
    	int smallestAngleEnemy = getSmallestAngleEnemy(self, world);
    	int nearestEnemy = getNearestEnemy(self, world);
    	
    	int enemyIndex = -1;
//    	if (smallestAngleEnemy.index == nearestEnemy.index) {
//    		enemyIndex = nearestEnemy.index;
//    	} else {
//    		if (nearestEnemy.dist < MIN_SHOOT_DIST) {
//    			enemyIndex = nearestEnemy.index;
//    		} else {
    			enemyIndex = smallestAngleEnemy;
//    		}
//    	}
    	
    	if (enemyIndex != -1) {
    		double enemyAngle = self.getTurretAngleTo(tanks[enemyIndex]);
    		if (enemyAngle > MIN_SHOOT_ANGLE) {
    			move.setTurretTurn(1);
    		} else if (enemyAngle < -MIN_SHOOT_ANGLE) {
    			move.setTurretTurn(-1);
    		} else {
		        move.setFireType(FireType.PREMIUM_PREFERRED);
    		}
    	}
    	
    	int bonusIndex = getNearestBonus(self, world);
    	if (bonusIndex != -1) {
    		Bonus bonus = world.getBonuses()[bonusIndex];
    		double angle = self.getAngleTo(bonus);
    		if (angle > MIN_BONUS_ANGLE) {
    			move.setLeftTrackPower(0.75);
    			move.setRightTrackPower(-1);
    		} else if (angle < -MIN_BONUS_ANGLE) {
    			move.setLeftTrackPower(-1);
    			move.setRightTrackPower(0.75);
    		} else {
    			move.setLeftTrackPower(1);
    			move.setRightTrackPower(1);
    		}
    	}
    }

    @Override
    public TankType selectTank(int tankIndex, int teamSize) {
        return TankType.MEDIUM;
    }
}
