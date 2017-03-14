package blueteam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

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
 * <p>
 * Chops the nearest (non-friendly) tree. (Regardless on the
 * treeInfo.containedBullets) Is aggressive: prefers attacking if enemy is in
 * sight. Moves randomly, changes direction only if the current direction is
 * blocked.
 *
 * @author Tomas
 */
public class LumberJack extends Robot {

	private Direction moveDirection;
	boolean avoidingFriendlyHit;
	int counter;
	MapLocation enemyLocation;
	int attackRoundsCount;

	LumberJack(RobotController rc) {
		super(rc);
		moveDirection = randomDirection();
		avoidingFriendlyHit = false;
		counter = 0;
		enemyLocation = rc.getInitialArchonLocations(rc.getTeam().opponent())[0];
		attackRoundsCount = 0;
	}

	boolean friendlyStrikeDamage() {
		return rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, rc.getTeam()).length == 0 ? false : true;
	}

	boolean strikeNearRobot(RobotInfo[] robots) {
		if (robots.length > 0 && !rc.hasAttacked()) {
			// Avoid friendly fire
			// friendly trees can be damaged if we can potentially kill enemy
			if (!friendlyStrikeDamage()) {
				avoidingFriendlyHit = false;
				try {
					rc.strike();
				} catch (GameActionException e) {
					// this should never happen
					e.printStackTrace();
				}
				return true;
			} else {
				avoidingFriendlyHit = true;
				moveDirection = randomFreeDirection(moveDirection.opposite(), 180);
				return false;

			}

		}
		return false;

	}

	boolean isAttackTime() {
		return rc.getRoundNum() > TeamConstants.LUMBERJACK_START_ATTACKING_FROM_ROUND;
	}

	@Override
	void step() throws GameActionException {
		MapLocation myLocation = rc.getLocation();
		RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
		// See if there are any enemy robots within striking range
		if (strikeNearRobot(robots))
			return;

		if (avoidingFriendlyHit) {
			// rc.setIndicatorDot(rc.getLocation(), 100, 255, 0);
			if (tryMove(moveDirection))
				moveDirection = randomFreeDirection(moveDirection, 180);
			if (counter > 4) {
				avoidingFriendlyHit = false;
				counter = 0;
			}
			++counter;
			return;
		}

		TreeInfo[] trees = rc.senseNearbyTrees();
		// priority has a enemy tree
		Optional<TreeInfo> nearEnemyTree = Arrays.stream(trees).filter(tree -> tree.getTeam() == enemy).findFirst();
		if (nearEnemyTree.isPresent()) {
			if (rc.canChop(nearEnemyTree.get().ID)) {
				rc.chop(nearEnemyTree.get().ID);
				return;
			}
		}

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
		if (bonusTree.isPresent() && !isAttackTime()) {
			moveDirection = myLocation.directionTo(bonusTree.get().getLocation());
			// rc.setIndicatorDot(myLocation, 255, 0, 0);
			nav.moveToTarget(bonusTree.get().getLocation());
			return;

		}

		// No close robots, so search for robots within sight radius
		Optional<RobotInfo> nearRobot = Arrays.stream(rc.senseNearbyRobots(-1, enemy))
				.filter(robot -> robot.getType() != RobotType.SOLDIER && robot.getType() != RobotType.TANK).findFirst();
		// If there is a robot, move towards it
		if (nearRobot.isPresent()) {

			MapLocation enemyLocation = nearRobot.get().getLocation();
			Direction toEnemy = myLocation.directionTo(enemyLocation);
			if (tryMove(toEnemy))
				return;
		}
		if (rc.hasMoved())
			return;

		// define predicates for tree filtering
		Predicate<TreeInfo> isEnemyTree = tree -> tree.getTeam() == enemy;
		Predicate<TreeInfo> isOurTree = tree -> tree.getTeam() == rc.getTeam();
		Predicate<TreeInfo> isInEnemyDir = tree -> {
			Direction treeDir = rc.getLocation().directionTo(tree.getLocation());
			float deltaAngle = Math.abs(treeDir.degreesBetween(rc.getLocation().directionTo(enemyLocation)));
			return (deltaAngle < 45);
		};

		ArrayList<TreeInfo> filteredTrees = new ArrayList<>();
		if (isAttackTime()) {
			filteredTrees = filterTreeBy(trees, isEnemyTree);
			if (filteredTrees.size() == 0) {
				filteredTrees = filterTreeBy(trees, isInEnemyDir.and(isOurTree.negate()));
			}
		} else {
			if (filteredTrees.size() == 0) {
				filteredTrees = filterTreeBy(trees, isOurTree.negate());
			}
		}
		// no tree can be chopped -> go to the nearest (enemy) tree:
		for (TreeInfo tree : filteredTrees) {
			Direction dirToTree = myLocation.directionTo(tree.getLocation());
			if (rc.canMove(dirToTree, rc.getType().strideRadius)) {
				rc.move(dirToTree, rc.getType().strideRadius);
				// rc.setIndicatorDot(tree.getLocation(), 0, 255, 0);
				return;
			}
		}
		if (rc.hasMoved())
			return;
		// No tree in sight -> move randomly or attack:
		if (isAttackTime()) {
			if (myLocation.distanceTo(enemyLocation) < TeamConstants.LUMBERJACK_ATTACK_RADIUS) {
				moveRandomly();
				++attackRoundsCount;
			} else {
				nav.moveToTarget(enemyLocation);
				moveDirection = myLocation.directionTo(enemyLocation);
			}
			if (attackRoundsCount > TeamConstants.LUMBERJACK_ROUNDS_IN_ATACK_ZONE) {
				attackRoundsCount = 0;
				Optional<MapLocation> loc = Arrays.stream(rc.getInitialArchonLocations(enemy))
						.filter(location -> !location.equals(enemyLocation)).findFirst();
				if (loc.isPresent()) {
					enemyLocation = loc.get();
				} else {
					enemyLocation = combatLocations.getActiveLocations()[0];
				}
			}
		} else {
			moveRandomly();
		}

	}

	void moveRandomly() {
		// rc.setIndicatorDot(rc.getLocation(), 0, 0, 0);
		if (!rc.canMove(moveDirection, rc.getType().strideRadius / 2)) {
			moveDirection = randomFreeDirection();
		}
		if (!tryMove(moveDirection)) {
			if (rc.canMove(moveDirection, rc.getType().strideRadius / 2))
				try {
					rc.move(moveDirection, rc.getType().strideRadius / 2);
				} catch (GameActionException e) {
					e.printStackTrace();
				}
		}
	}

}
