package blueteam;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Archon extends Robot {



	Archon(RobotController rc) {
		super(rc);
	}

	@Override
	void step() throws GameActionException {
		int numberOfArchons = getRobotCount();

		// move away from enemy
		RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
		if (robots.length > 0) {
			Direction toEnemy = rc.getLocation().directionTo(robots[0].getLocation());
			tryMove(toEnemy.opposite());
		} else {
			tryMove(randomDirection());
		}

		// randomly attempt to build a gardener if we need more
		Direction dir = randomDirection();
		int numOfGardeners = getRobotCount(RobotType.GARDENER);
		int gardenersNeeded = TeamConstants.DESIRED_NUMBER_OF_GARDENERS - numOfGardeners;
		if (rc.canHireGardener(dir)
				&& Math.random() < (double) gardenersNeeded / numberOfArchons) {
			rc.hireGardener(dir);
		}

		// donate all bullets if we can win immediately
		if (rc.getTeamBullets() / rc.getVictoryPointCost()
				+ rc.getTeamVictoryPoints() >= GameConstants.VICTORY_POINTS_TO_WIN) {
			rc.donate(rc.getTeamBullets());
		}
		// buy some victory points randomly
		if (Math.random() < 0.05 / numberOfArchons) {
			rc.donate(rc.getTeamBullets() / 4);
		}
	}

}
