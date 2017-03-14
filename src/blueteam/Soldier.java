package blueteam;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

/**
 * Unless some enemy location is reported, moves randomly and changes the
 * direction only if the current direction is blocked. However, with some
 * probability, it moves in the direction of the opponents archon location. If
 * it can shoot, it shoots and reports the enemy location to other soldiers.
 *
 * @author Tomas
 *
 */
public class Soldier extends Robot {

	private Direction moveDir;
	private boolean stuckDetected = false;

	Soldier(RobotController rc) {
		super(rc);
		moveDir = randomDirection();
	}

	@Override
	void step() throws GameActionException {
		// See if there is an enemy which we can shoot
		RobotInfo victim = getVictim();

		// First move...
		if (!rc.hasMoved()) {
			if (victim != null && shouldApproachToEnemy(victim)) {
				// Move in the direction of the enemy
				moveDir = rc.getLocation().directionTo(victim.getLocation());
			} else {
				MapLocation[] activeLocations = combatLocations.getActiveLocations();

				if (activeLocations.length != 0 && !stuckDetected) {
					moveDir = rc.getLocation().directionTo(activeLocations[0]);
					rc.setIndicatorLine(rc.getLocation(), activeLocations[0], 255, 255, 0);
					if (moveDir == null || !rc.canMove(moveDir)) {
						stuckDetected = true;
						rc.setIndicatorDot(rc.getLocation(), 255, 0, 0);
						moveDir = randomFreeDirection();
					}
				}
				if (!rc.canMove(moveDir)) {
					// Move randomly

					changeMoveDirection();
					stuckDetected = false;
					rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
				}
			}

			tryMove(moveDir);
		}
		// ...then shoot (so that the robot does not kill itself).
		if (victim != null) {
			combatLocations.reportLocation(victim.getLocation());
			if (shouldFireTriad(victim)) {
				rc.fireTriadShot(rc.getLocation().directionTo(victim.getLocation()));
				return;
			} else if (rc.canFireSingleShot()) {
				// ...Then fire a bullet in the direction of the enemy.
				rc.fireSingleShot(rc.getLocation().directionTo(victim.getLocation()));
				// Do not move..
				return;
			}
		}

	}

	private boolean shouldFireTriad(RobotInfo victim) {
		Direction dir = rc.getLocation().directionTo(victim.getLocation());
		float offset = GameConstants.TRIAD_SPREAD_DEGREES;
		return haveEnoughBullets() && isEnemy(nearestInDirection(dir.rotateLeftDegrees(offset)))
				&& isEnemy(nearestInDirection(dir.rotateRightDegrees(offset)));
	}

	private boolean haveEnoughBullets() {
		return rc.canFireTriadShot() && rc.getTeamBullets() > TeamConstants.MINIMUM_BULLETS_TO_SAVE_BY_SOLDIER;
	}

	private boolean shouldApproachToEnemy(RobotInfo victim) {
		return (victim.getType() != RobotType.SOLDIER && victim.getType() != RobotType.TANK)
				|| rc.getLocation().distanceTo(victim.getLocation()) > TeamConstants.MAX_SOLDIER_TO_SOLDIER_DISTANCE;
	}

	private void changeMoveDirection() {
		if (Math.random() < TeamConstants.SOLDIER_RANDOM_MOVE_PROB) {
			moveDir = randomFreeDirection();
		} else {
			moveDir = getDirToEnemyArchonInitLoc();
		}
	}

	private Direction getDirToEnemyArchonInitLoc() {
		MapLocation[] archons = rc.getInitialArchonLocations(enemy);
		MapLocation rndArchon = archons[(int) Math.floor(Math.random() * archons.length)];
		Direction directionToArchonInit = rc.getLocation().directionTo(rndArchon);
		return directionToArchonInit;
	}

	private RobotInfo getVictim() {
		RobotInfo[] enemies = rc.senseNearbyRobots(-1, enemy);
		MapLocation myLoc = rc.getLocation();
		if (enemies.length == 0)
			return null;
		RobotInfo[] friends = rc.senseNearbyRobots(-1, rc.getTeam());
		TreeInfo[] myTrees = rc.senseNearbyTrees(-1, rc.getTeam());
		for (RobotInfo enemy : enemies) {
			float distanceToEnemy = enemy.getLocation().distanceTo(myLoc);
			Direction dirToEnemy = myLoc.directionTo(enemy.getLocation());
			if (!willIHitSomething(dirToEnemy, friends, distanceToEnemy)
					&& !willIHitSomething(dirToEnemy, myTrees, distanceToEnemy))
				return enemy;
		}
		return null;
	}

}
