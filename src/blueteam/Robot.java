package blueteam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import battlecode.common.BodyInfo;
import battlecode.common.BulletInfo;
import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

abstract public class Robot {
	RobotController rc;
	Team enemy;
	Random rand;
	private boolean alive;

	ImportantLocations combatLocations;
	// this is the slugs "tail" imagine leaving a trail of sticky goo on the map
	// that you don't want to step in that slowly dissapates over time
	ArrayList<MapLocation> oldLocations = new ArrayList<>();
	static boolean swarm = false;

	Robot(RobotController rc) {
		this.rc = rc;
		enemy = rc.getTeam().opponent();
		rand = new Random();
		combatLocations = new ImportantLocations(rc, TeamConstants.COMBAT_LOCATIONS_FIRST_CHANNEL);
		newRobotBorn();
	}

	void run() throws GameActionException {
		while (true) {
			try {
				checkHealth();
				buyVictoryPoints();
				dodge();
				step();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
			Clock.yield();
		}
	}

	abstract void step() throws GameActionException;

	/**
	 * buys victory points for a team
	 */
	void buyVictoryPoints() throws GameActionException {
		// donate all bullets if we can win immediately
		if (rc.getTeamBullets() / rc.getVictoryPointCost()
				+ rc.getTeamVictoryPoints() >= GameConstants.VICTORY_POINTS_TO_WIN) {
			rc.donate(rc.getTeamBullets());
		}

		float bullets = rc.getTeamBullets();
		if (bullets > TeamConstants.MAXIMUM_BULLETS_TO_SAVE) {
			float bulletsToDonate = bullets - TeamConstants.MINIMUM_BULLETS_TO_SAVE;
			// round bullets not to donate more than needed
			bulletsToDonate -= bulletsToDonate % rc.getVictoryPointCost();
			rc.donate(bulletsToDonate);
		}
	}

	/**
	 * advertise to others that this robot is alive
	 */
	private void newRobotBorn() {
		alive = true;
		updateRobotCount(1);
	}

	/**
	 * advertise imminent death to all friends, so they could mourn their buddy
	 */
	private void checkHealth() {
		if (!alive) {
			return;
		}

		if (rc.getHealth() < rc.getType().getStartingHealth() * TeamConstants.MINIMUM_HEALTH_PERCENTAGE
				|| rc.getHealth() < TeamConstants.MINIMUM_HEALTH) {
			updateRobotCount(-1);
			alive = false;
			reportDeath();
		}
	}

	/**
	 * reports death to all the friends so they could revenge their buddy
	 */
	protected void reportDeath() {
		combatLocations.reportLocation(rc.getLocation());
	}

	/**
	 * updates shared information about how much robots we have of given type
	 *
	 * @param i
	 *            how much to update
	 */
	private void updateRobotCount(int i) {
		int channel = TeamConstants.ROBOT_COUNTERS_BEGIN + rc.getType().ordinal();
		try {
			int current = rc.readBroadcast(channel);
			rc.broadcast(channel, current + i);
		} catch (GameActionException e) {
			// should never happen
			e.printStackTrace();
		}
	}

	/**
	 * returns how many robots of the given type we have in game.
	 *
	 * @return robot count
	 */
	int getRobotCount(RobotType type) {
		int channel = TeamConstants.ROBOT_COUNTERS_BEGIN + type.ordinal();
		try {
			int count = rc.readBroadcast(channel);
			return count;
		} catch (GameActionException e) {
			// should never happen
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * returns how many robots of the same type we have in game.
	 *
	 * @return robot count
	 */
	int getRobotCount() {
		return getRobotCount(rc.getType());
	}

	/**
	 * Returns a random Direction
	 *
	 * @return a random Direction
	 */
	Direction randomDirection() {
		return new Direction((float) Math.random() * 2 * (float) Math.PI);
	}

	/**
	 * Tries to generate a random direction into which the robot can move freely
	 * by at least half of its stride radius. If it fails, returns a random
	 * direction.
	 *
	 * @return
	 */
	protected Direction randomFreeDirection() {
		Direction rndDir = null;
		for (int i = 0; i < TeamConstants.GENERATING_DIR_MAX_TRIES_LIMIT; i++) {
			rndDir = randomDirection();
			if (rc.canMove(rndDir, rc.getType().strideRadius / 2))
				return rndDir;
		}
		return rndDir;
	}

	Direction randomFreeDirection(Direction dir, float range) {
		for (int i = 0; i < TeamConstants.GENERATING_DIR_MAX_TRIES_LIMIT; i++) {
			float angle = rand.nextFloat() * range - (range / 2);
			Direction rndDir = dir.rotateRightDegrees(angle);
			if (rc.canMove(rndDir))
				return rndDir;
		}
		return randomFreeDirection();
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * directly in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	boolean tryMove(Direction dir) {
		return tryMove(dir, 20, 3);
	}

	/**
	 * Attempts to move in a given direction, while avoiding small obstacles
	 * direction in the path.
	 *
	 * @param dir
	 *            The intended direction of movement
	 * @param degreeOffset
	 *            Spacing between checked directions (degrees)
	 * @param checksPerSide
	 *            Number of extra directions checked on each side, if intended
	 *            direction was unavailable
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) {
		try {
			// First, try intended direction
			if (rc.canMove(dir)) {
				rc.move(dir);
				return true;
			}

			// Now try a bunch of similar angles
			int currentCheck = 1;

			while (currentCheck <= checksPerSide) {
				// Try the offset of the left side
				if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
					rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
					return true;
				}
				// Try the offset on the right side
				if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
					rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
					return true;
				}
				// No move performed, try slightly further
				currentCheck++;
			}

			// A move never happened, so return false.
			return false;
		} catch (GameActionException e) {
			// this can't actually happen since we always ask canMove first
			return false;
		}
	}

	/**
	 * A slightly more complicated example function, this returns true if the
	 * given bullet is on a collision course with the current robot. Doesn't
	 * take into account objects between the bullet and this robot.
	 *
	 * @param bullet
	 *            The bullet in question
	 * @return True if the line of the bullet's path intersects with this
	 *         robot's current position.
	 */
	boolean willCollideWithMe(BulletInfo bullet) {
		MapLocation myLocation = rc.getLocation();

		// Get relevant bullet information
		Direction propagationDirection = bullet.dir;
		MapLocation bulletLocation = bullet.location;

		// Calculate bullet relations to this robot
		Direction directionToRobot = bulletLocation.directionTo(myLocation);
		float distToRobot = bulletLocation.distanceTo(myLocation);
		float theta = propagationDirection.radiansBetween(directionToRobot);

		// If theta > 90 degrees, then the bullet is traveling away from us and
		// we can break early
		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}

		// distToRobot is our hypotenuse, theta is our angle, and we want to
		// know this length of the opposite leg.
		// This is the distance of a line that goes from myLocation and
		// intersects perpendicularly with propagationDirection.
		// This corresponds to the smallest radius circle centered at our
		// location that would intersect with the
		// line that is the path of the bullet.
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));

		return (perpendicularDist <= rc.getType().bodyRadius);
	}

	/**
	 * Tests if I will hit the given object providing I shoot into the direction
	 * "shootingDirection". Can be used with trees or robots.
	 *
	 * @param shootingDirection
	 * @param body
	 * @return
	 */
	boolean willIHitBody(Direction shootingDirection, BodyInfo body) {
		MapLocation myLocation = rc.getLocation();
		MapLocation robotLocation = body.getLocation();

		Direction directionToRobot = myLocation.directionTo(robotLocation);
		float theta = shootingDirection.radiansBetween(directionToRobot);
		if (Math.abs(theta) > Math.PI / 2) {
			return false;
		}
		float distToRobot = myLocation.distanceTo(robotLocation);
		float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
		return perpendicularDist <= body.getRadius();
	}

	/**
	 * Alias for the function willIHitBody
	 *
	 * @param shootingDirection
	 * @param robot
	 * @return
	 */
	boolean willIHitRobot(Direction shootingDirection, RobotInfo robot) {
		return willIHitBody(shootingDirection, robot);
	}

	/**
	 * Tests if I will hit some object from the list of trees/robots providing I
	 * shoot into the direction "shootingDirection"
	 *
	 * @param shootingDirection
	 * @param bodies
	 *            List of robots/trees sorted by distance from me.
	 * @param maxRadius
	 *            Skips robots/trees that are further from me than the given
	 *            distance.
	 * @return
	 */
	<T extends BodyInfo> boolean willIHitSomething(Direction shootingDirection, T[] bodies, float maxRadius) {
		for (BodyInfo friend : bodies) {
			if (friend.getLocation().distanceTo(rc.getLocation()) > maxRadius)
				return false;
			if (willIHitBody(shootingDirection, friend))
				return true;
		}
		return false;
	}

	/**
	 * The nearest object that would be hit by a shot in the given direction.
	 *
	 * @param dir
	 * @return Might return null
	 */
	BodyInfo nearestInDirection(Direction dir) {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		BodyInfo nearest = null;
		float distance = Float.POSITIVE_INFINITY;
		for (RobotInfo robot : nearbyRobots) {
			if (willIHitRobot(dir, robot)) {
				nearest = robot;
				distance = rc.getLocation().distanceTo(nearest.getLocation());
				break;
			}
		}
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
		for (TreeInfo tree : nearbyTrees) {
			if (willIHitBody(dir, tree)) {
				if (rc.getLocation().distanceTo(tree.getLocation()) < distance) {
					return tree;
				}
			}
		}
		return nearest;
	}

	boolean isEnemy(BodyInfo body) {
		if (body == null)
			return false;
		return (body.isTree() ? ((TreeInfo) body).getTeam() : ((RobotInfo) body).getTeam()) == enemy;
	}

	boolean isFriend(BodyInfo body) {
		if (body == null)
			return false;
		return (body.isTree() ? ((TreeInfo) body).getTeam() : ((RobotInfo) body).getTeam()) == rc.getTeam();
	}

	/**
	 * Try to move in direction perpendicular to bullet trajectory
	 *
	 * @param bullet
	 * @return true if move succeeded
	 */
	boolean trySidestep(BulletInfo bullet) {
		Direction towards = bullet.getDir();
		return (tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
	}

	/**
	 * Try to dodge all bullets in range.
	 */
	void dodge() {
		BulletInfo[] bullets = rc.senseNearbyBullets();
		Arrays.stream(bullets).filter(x -> willCollideWithMe(x)).forEach(x -> trySidestep(x));
	}

	ArrayList<RobotInfo> filterByType(RobotInfo[] robots, RobotType type) {
		ArrayList<RobotInfo> res = new ArrayList<>();
		for (RobotInfo robot : robots) {
			if (robot.getType() == type)
				res.add(robot);
		}
		return res;
	}

	Optional<RobotInfo> getNearestRobot(RobotType type, float maxRadius) {
		RobotInfo[] close_enemies = rc.senseNearbyRobots(maxRadius, enemy);
		ArrayList<RobotInfo> robots = filterByType(close_enemies, type);
		if (robots.size() > 0) {
			rc.setIndicatorDot(robots.get(0).getLocation(), 0, 250, 0);
			return Optional.of(robots.get(0));
		}
		return Optional.empty();
	}

	/**
	 *
	 * @param map
	 *            A MapLocation to convert to integer representation
	 * @return An array arr such that: arr[0] - integer part of x arr[1] -
	 *         decimal part of x * 10^6 and rounded arr[2] - integer part of y
	 *         arr[3] - decimal part of y * 10^6 and rounded
	 */
	int[] convertMapLocation(MapLocation map) {
		float xcoord = map.x;
		float ycoord = map.y;
		int[] returnarray = new int[4];
		returnarray[0] = Math.round(xcoord - (xcoord % 1));
		returnarray[1] = Math.toIntExact(Math.round((xcoord % 1) * Math.pow(10, 6)));
		returnarray[2] = Math.round(ycoord - (ycoord % 1));
		returnarray[3] = Math.toIntExact(Math.round((ycoord % 1) * Math.pow(10, 6)));
		return (returnarray);
	}

	/**
	 *
	 * @param arr
	 *            An array arr such that: arr[0] - integer part of x arr[1] -
	 *            decimal part of x * 10^6 and rounded arr[2] - integer part of
	 *            y arr[3] - decimal part of y * 10^6 and rounded
	 * @return A MapLocation instantiated from the coordinates given by array
	 */
	MapLocation convertLocationInts(int[] arr) {
		float xcoord = (float) (arr[0] + arr[1] / Math.pow(10, 6));
		float ycoord = (float) (arr[2] + arr[3] / Math.pow(10, 6));
		return (new MapLocation(xcoord, ycoord));
	}

	MapLocation readLocation(int firstChannel) throws GameActionException {
		int[] array = new int[4];
		array[0] = rc.readBroadcast(firstChannel);
		array[1] = rc.readBroadcast(firstChannel + 1);
		array[2] = rc.readBroadcast(firstChannel + 2);
		array[3] = rc.readBroadcast(firstChannel + 3);
		return convertLocationInts(array);
	}

	void writeLocation(MapLocation map, int firstChannel) throws GameActionException {
		int[] arr = convertMapLocation(map);
		rc.broadcast(firstChannel, arr[0]);
		rc.broadcast(firstChannel + 1, arr[1]);
		rc.broadcast(firstChannel + 2, arr[2]);
		rc.broadcast(firstChannel + 3, arr[3]);
	}

	void wander() {
		Direction dir = randomFreeDirection();
		tryMove(dir);
	}

	boolean slugMoveToTarget(MapLocation target, float strideRadius) {

		// when trying to move, let's look forward, then incrementing left and
		// right.
		float[] toTry = { 0, (float) Math.PI / 4, (float) -Math.PI / 4, (float) Math.PI / 2, (float) -Math.PI / 2,
				3 * (float) Math.PI / 4, -3 * (float) Math.PI / 4, -(float) Math.PI };

		MapLocation ourLoc = rc.getLocation();
		Direction toMove = ourLoc.directionTo(target);

		// let's try to find a place to move!
		for (int i = 0; i < toTry.length; i++) {
			Direction dirToTry = toMove.rotateRightDegrees(toTry[i]);
			if (rc.canMove(dirToTry, strideRadius)) {
				// if that location is free, let's see if we've already moved
				// there before (aka, it's in our tail)
				MapLocation newLocation = ourLoc.add(dirToTry, strideRadius);
				boolean haveWeMovedThereBefore = false;
				for (int j = 0; j < oldLocations.size(); j++) {
					if (newLocation.distanceTo(oldLocations.get(j)) < strideRadius * strideRadius) {
						haveWeMovedThereBefore = true;
						break;
					}
				}
				if (!haveWeMovedThereBefore) {
					oldLocations.add(newLocation);
					if (oldLocations.size() > 10) {
						// remove the head and chop the list down to size 10 (or
						// whatever you want to use)
					}
					if (!rc.hasMoved() && rc.canMove(dirToTry, strideRadius)) {
						try {
							rc.move(dirToTry, strideRadius);
						} catch (GameActionException e) {
							e.printStackTrace();
						}
					}
					return (true);
				}

			}
		}
		// looks like we can't move anywhere
		return (false);

	}

	boolean moveToTarget(MapLocation location) {
		// try to take a big step
		if (slugMoveToTarget(location, rc.getType().strideRadius)) {
			return (true);
		}
		// try to take a smaller step
		if (slugMoveToTarget(location, rc.getType().strideRadius / 2)) {
			return (true);
		}
		// try to take a baby step
		if (slugMoveToTarget(location, rc.getType().strideRadius / 4)) {
			return (true);
		} else {
			wander();
			return (false);
		}
		// insert move randomly code here

	}
}
