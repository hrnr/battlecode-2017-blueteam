package blueteam;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.TreeInfo;

/**
 * Naive implementation: Moves randomly, changes direction only if the current
 * direction is blocked. However, with 25 % probability, it moves in the
 * direction of the opponents archon location. If it can shoot, it shoots and
 * stays in place
 *
 * @author Tomas
 *
 */
public class Soldier extends Robot {

	private Direction moveDir;
	// probability that the chosen direction is random.
	private double RANDOM_MOVE_PROB = .75;

	Soldier(RobotController rc) {
		super(rc);
		moveDir = randomDirection();
	}

	@Override
	void step() throws GameActionException {
		// See if there is an enemy which we can shoot
		RobotInfo victim = getVictim();
		if (victim != null) {
			// And we have enough bullets, and haven't attacked yet this
			// turn...
			if (rc.canFireSingleShot()) {

				// ...Then fire a bullet in the direction of the enemy.
				rc.fireSingleShot(rc.getLocation().directionTo(victim.getLocation()));
				// Do not move..
				return;
			}
		}
		if (rc.hasMoved()) {
			return;
		}

		// Move randomly
		if (!rc.canMove(moveDir, rc.getType().strideRadius / 2)) {
			changeMoveDirection();
		}
		if (!tryMove(moveDir)) {
			if (rc.canMove(moveDir, rc.getType().strideRadius / 2))
				rc.move(moveDir, rc.getType().strideRadius / 2);
		}
	}

	private void changeMoveDirection() {
		if (Math.random() < RANDOM_MOVE_PROB) {
			moveDir = randomFreeDirection();
		} else {
			moveDir = getDirToEnemyArchonInitLoc();
			rc.setIndicatorDot(rc.getLocation(), 0, 255, 255);
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
		RobotInfo[] friends = rc.senseNearbyRobots(-1, enemy.opponent());
		TreeInfo[] myTrees = rc.senseNearbyTrees(-1, enemy.opponent());
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
