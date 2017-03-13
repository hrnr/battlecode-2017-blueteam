package blueteam;

import java.util.ArrayDeque;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Navigation {

	Robot robot;
	RobotController rc;
	// this is the slugs "tail" imagine leaving a trail of sticky goo on the map
	// that you don't want to step in that slowly dissapates over time
	ArrayDeque<MapLocation> oldLocations;

	Navigation(Robot r, RobotController rc) {
		robot = r;
		this.rc = rc;
		oldLocations = new ArrayDeque<>();
	}

	void wander() {
		Direction dir = robot.randomFreeDirection();
		robot.tryMove(dir);
	}

	private boolean slugMoveToTarget(MapLocation target, float strideRadius) {

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
				for (MapLocation loc : oldLocations) {
					if (newLocation.distanceTo(loc) < strideRadius * strideRadius) {
						haveWeMovedThereBefore = true;
						break;
					}
				}
				if (!haveWeMovedThereBefore) {
					oldLocations.addLast(newLocation);
					if (oldLocations.size() > 10) {
						oldLocations.removeFirst();
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
