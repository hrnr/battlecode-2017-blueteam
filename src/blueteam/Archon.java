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

	@Override
	void step() throws GameActionException {
		// move away from enemy
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
		if (robots.length > 0) {
			Direction toEnemy = rc.getLocation().directionTo(robots[0].getLocation());
			tryMove(toEnemy.opposite());
		} else {
			tryMove(randomDirection());
		}

		// we want to preserve some bullet points for gardener
		if (rc.getTeamBullets() < TeamConstants.MINIMUM_BULLETS_TO_SAVE && getRobotCount(RobotType.GARDENER) > 1) {
			return;
		}

		// randomly attempt to build a gardener if we need more
		Direction dir = randomDirection();
		System.out.println(getRobotCount(RobotType.GARDENER));
		if (rc.canHireGardener(dir) && TeamConstants.DESIRED_NUMBER_OF_GARDENERS > getRobotCount(RobotType.GARDENER)) {
			rc.hireGardener(dir);
		}
	}
}
