package blueteam;

import battlecode.common.Clock;
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

	@Override
	void step() throws GameActionException {
		roundCounter++;
		// Dont move purely randomly each turn, go in some rand direction that is not in 80 degree range of enemy
		// spawn location. Go for 40 turns somewhere, dont hug the edge of map too.
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
		// Hardcoded first 180 rounds. Just build gardener each 60 rounds.
		// Easiest way to give gardeners enough time to expand. After round 180 continue in general approach.
		Direction dir = randomDirection();
		if (rc.getRoundNum() >= 0 && rc.getRoundNum() < 60 && step == 0) {
			if (rc.getTeamBullets()> 110) {
				rc.hireGardener(dir);
				step++;
			}
		} else if (rc.getRoundNum() >= 60 && rc.getRoundNum() < 120 && step == 1) {
			if (rc.getTeamBullets()> 110) {
				rc.hireGardener(dir);
				step++;
			}
		} else if (rc.getRoundNum() >= 120 && rc.getRoundNum() < 180 && step == 2) {
			if (rc.getTeamBullets()> 110) {
				rc.hireGardener(dir);
				step++;
			}
		} else {
			if (rc.getRoundNum() > 100 && rc.getRoundNum() < 150) {
				while (getRobotCount(RobotType.SOLDIER) == 0) {
					if (rc.getRoundNum() > 150)
						break;
					Clock.yield();
				}
			}
			// we want to preserve some bullet points for gardener
			if (rc.getTeamBullets() < TeamConstants.MINIMUM_BULLETS_TO_SAVE && getRobotCount(RobotType.GARDENER) > 2) {
				return;
			}
			// randomly attempt to build a gardener if we need more

			if (rc.canHireGardener(dir) && TeamConstants.DESIRED_NUMBER_OF_GARDENERS > getRobotCount(RobotType.GARDENER)) {
				if (rc.getTeamBullets()> 110)
					rc.hireGardener(dir);
			}
		}
	}
}
