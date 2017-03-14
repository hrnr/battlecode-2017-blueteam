package blueteam;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Archon extends Robot {

	Archon(RobotController rc) {
		super(rc);
	}

	int roundCounter = 0;
	Direction currentDir = randomDirection();
	int step = 0;

	@Override void step() throws GameActionException {
		roundCounter++;
		if (roundCounter > 40) {
			roundCounter = 0;
			currentDir = randomDirection();
			while (Math.abs(currentDir.getAngleDegrees() - rc.getLocation()
					.directionTo(rc.getInitialArchonLocations(enemy)[0]).getAngleDegrees()) < 40)
				currentDir = randomDirection();

		}
		if (!rc.onTheMap(rc.getLocation(), 3)) {
			roundCounter += 5;
		}
		// move away from enemy
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
		if (robots.length > 0) {
			Direction toEnemy = rc.getLocation().directionTo(robots[0].getLocation());
			tryMove(toEnemy.opposite());
		} else {
			tryMove(currentDir);
		}
		//
		Direction dir = randomDirection();
		if (rc.getRoundNum() >= 0 && rc.getRoundNum() < 60 && step == 0) {
			rc.hireGardener(dir);
			step++;
		} else if (rc.getRoundNum() >= 60 && rc.getRoundNum() < 120 && step == 1) {
			rc.hireGardener(dir);
			step++;
		} else if (rc.getRoundNum() >= 120 && rc.getRoundNum() < 180 && step == 2) {
			rc.hireGardener(dir);
			step++;
		} else {
			// we want to preserve some bullet points for gardener
			if (rc.getTeamBullets() < 200 && getRobotCount(RobotType.GARDENER) > 5
					&& getRobotCount(RobotType.GARDENER) < 2) {
				return;
			}
			// randomly attempt to build a gardener if we need more

			//System.out.println(getRobotCount(RobotType.GARDENER));
			if (rc.canHireGardener(dir) && 15 > getRobotCount(RobotType.GARDENER)) {
				rc.hireGardener(dir);
			}
		}
	}
}
