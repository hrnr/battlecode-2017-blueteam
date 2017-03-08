package blueteam;

import java.util.Arrays;
import java.util.Optional;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

/**
 * Naive implementation:
 *
 * Chops the nearest (non-friendly) tree. (Regardless on the
 * treeInfo.containedBullets) Is aggressive: prefers attacking if enemy is in
 * sight. Moves randomly, changes direction only if the current direction is
 * blocked.
 *
 * @author Tomas
 *
 */
public class LumberJack extends Robot {

	private Direction moveDirection;

	LumberJack(RobotController rc) {
		super(rc);
		moveDirection = randomDirection();
	}

	@Override
	void step() throws GameActionException {
		// See if there are any enemy robots within striking range
		// (distance 1 from lumberjack's radius)
		Optional<RobotInfo> robots = Arrays.stream(rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy))
				.findFirst();
		MapLocation myLocation = rc.getLocation();
		if (robots.isPresent() && !rc.hasAttacked()) {
			// Use strike() to hit all nearby robots!
			rc.strike();
			return;
		}

		TreeInfo[] trees = rc.senseNearbyTrees();

		for (TreeInfo treeInfo : trees) {
			if (rc.canChop(treeInfo.ID) && treeInfo.team != rc.getTeam()) {
				rc.chop(treeInfo.ID);
				return;
			}
		}

		// trying to find some trees with robot inside. The priority is to cut
		// down this tree.
		Optional<TreeInfo> bonusTree = Arrays.stream(trees).filter(tree -> tree.getContainedRobot() != null)
				.findFirst();
		if (bonusTree.isPresent()) {
			moveDirection = myLocation.directionTo(bonusTree.get().getLocation());
			rc.setIndicatorDot(myLocation, 255, 0, 0);
			if (rc.canMove(moveDirection)) {
				tryMove(moveDirection);
			} else {
				// two robots are behind each other and both are trying to
				// access the same tree. This let's them move avoid each other
				if (rand.nextFloat() < 0.5)
					tryMove(moveDirection.rotateLeftDegrees(90));
				else
					tryMove(moveDirection.rotateRightDegrees(90));
			}
			return;

		}

		// No close robots, so search for robots within sight radius
		robots = Arrays.stream(rc.senseNearbyRobots(-1, enemy))
				.filter(robot -> robot.getType() != RobotType.SOLDIER && robot.getType() != RobotType.TANK).findFirst();
		// If there is a robot, move towards it
		if (robots.isPresent()) {

			MapLocation enemyLocation = robots.get().getLocation();
			Direction toEnemy = myLocation.directionTo(enemyLocation);
			if (tryMove(toEnemy))
				return;
		}

		// no tree can be chopped -> go to the nearest (enemy) tree:
		Optional<TreeInfo> nearest = Arrays.stream(trees).filter(x -> x.team != rc.getTeam()).findFirst();
		if (nearest.isPresent()) {
			Direction dirToTree = myLocation.directionTo(nearest.get().getLocation());
			tryMove(dirToTree);
			rc.setIndicatorDot(myLocation, 0, 255, 0);
			return;
		}
		if (rc.hasMoved())
			return;
		// No tree in sight -> move randomly:
		if (!rc.canMove(moveDirection, rc.getType().strideRadius / 2)) {
			moveDirection = randomFreeDirection();
		}
		if (!tryMove(moveDirection)) {
			if (rc.canMove(moveDirection, rc.getType().strideRadius / 2))
				rc.move(moveDirection, rc.getType().strideRadius / 2);
		}

	}

}
