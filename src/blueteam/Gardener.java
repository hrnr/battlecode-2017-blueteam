package blueteam;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.TreeInfo;

public class Gardener extends Robot {

	Gardener(RobotController rc) {
		super(rc);
	}

	enum GardenerState {
		FINDING, BUILDING, STARTING, ONLYSOLDIERS, LETSCHOP
	}

	GardenerState state = GardenerState.STARTING;
	Integer roundCounter = 0;
	Integer stuckCounter = 0;
	Direction currentDir = randomDirection();
	Integer lumberjackBuilded = 0;
	boolean scoutBuilded = false;
	boolean soldierBuilded = false;
	boolean stuck = false;
	int initTreeNum = 0;

	/**
	 * Searches for suitable location to start building hexagonal tree garden
	 *
	 * @param radius radius to search for other gardeners
	 * @return true if is gardener in suitable place
	 */
	boolean findSpot(float radius) {
		// get robots in radius
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(radius);

		if (roundCounter > TeamConstants.GARDENERS_DIRECT_PATH_LENGTH) {
			roundCounter = 0;
			stuckCounter++;
			currentDir = randomDirection();
		}
		boolean rdyToBuild = true;
		//check for robots
		for (RobotInfo robot : nearbyRobots)
			if (robot.getTeam().equals(rc.getTeam()))
				if (robot.getType().equals(RobotType.GARDENER) || robot.getType().equals(RobotType.ARCHON))
					rdyToBuild = false;
		try {
			if (!rc.onTheMap(rc.getLocation(), radius)) {
				rdyToBuild = false;
				roundCounter += 20;
			}
		} catch (GameActionException e) {
			System.out.println("bad radius in find loc function!");
			rdyToBuild = false;
		}
		if (stuckCounter > 5 && !stuck) {
			currentDir = rc.getLocation().directionTo(rc.getInitialArchonLocations(enemy)[0]);
			currentDir = currentDir.rotateRightDegrees(rand.nextInt(120) - 60);
			stuck = true;
		}
		if (!rdyToBuild)
			tryMove(currentDir);
		return rdyToBuild;
	}

	/**
	 * Default value for radius should be 3, (gardener,tree,space,tree,gardener),
	 * but should be changed if building tanks (as they take 2 radius)
	 *
	 * @return
	 */
	boolean findSpot() {
		return findSpot(TeamConstants.GARDENERS_DEFAULT_FREE_SPOT_RADIUS);
	}

	/**
	 * Builds tree every 30 degrees
	 *
	 * @return true if was planted
	 */
	boolean buildGarden() {
		boolean ret = false;
		Direction dir = TeamConstants.GARDENERS_GARDEN_ENTRANCE;
		for (int i = 0; i < 5; i++) {
			try {
				if (rc.canPlantTree(dir)) {
					rc.plantTree(dir);
					ret = true;
				}
			} catch (GameActionException e) {
				//should not happen, mby when we check and plant in different rounds
			}
			dir = dir.rotateRightDegrees(60);
		}
		return ret;
	}

	/**
	 * tries to water all plants around robot. Assuming starting dir is WEST
	 */
	void waterGarden() {
		Direction dir = TeamConstants.GARDENERS_GARDEN_ENTRANCE;
		MapLocation loc = rc.getLocation();
		for (int i = 0; i < 5; i++) {
			try {
				loc = loc.add(dir, 1.5f);
				if (rc.canWater(loc)) {
					rc.water(loc);
				}
			} catch (GameActionException e) {
				//should not happen, mby when we check and plant in different rounds
			}
			dir = dir.rotateRightDegrees(60);
			loc = rc.getLocation();
			// assuming watering was successful, we need to wait a rount
			Clock.yield();
		}
	}

	/**
	 * builds desired robot
	 *
	 * @param type of robot
	 * @return if successful
	 */
	boolean build(RobotType type, boolean random) {
		// wait until build is rdy
		if (!rc.isBuildReady())
			Clock.yield();
		Direction dir;
		if (!random)
			dir = TeamConstants.GARDENERS_GARDEN_ENTRANCE.rotateLeftDegrees(60);
		else
			dir = randomDirection();
		if (rc.canBuildRobot(type, dir)) {
			try {
				rc.buildRobot(type, dir);
			} catch (GameActionException e) {
				return false;
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * assuming were in garden so build direction is NOT random
	 *
	 * @param type of robot
	 * @return if successful
	 */
	boolean build(RobotType type) {
		return build(type, false);
	}

	/**
	 * checks whether enemy Archon is nearby,
	 * used in init phase
	 *
	 * @return true if is near
	 */
	boolean isEnemyArchonNear() {
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		for (RobotInfo robot : nearbyRobots) {
			if (robot.getType() == RobotType.ARCHON && robot.getTeam() == enemy)
				return true;
		}
		return false;
	}

	/**
	 * Checks whether there is a lot (const) trees around
	 *
	 * @return true if is more than TeamConstants.GARDENER_NUM_OF_TREES_TO_BUILD_LUMBER
	 */
	boolean isInWoods() {
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees(TeamConstants.GARDENERS_DEFAULT_FREE_SPOT_RADIUS);
		Integer curr = 0;
		for (TreeInfo tree : nearbyTrees)
			if (!tree.getTeam().equals(rc.getTeam()))
				curr++;
		if (curr > TeamConstants.GARDENER_NUM_OF_TREES_TO_BUILD_LUMBER)
			return true;
		else
			return false;
	}

	/**
	 * States:
	 * <p>
	 * STARTING <-> ONLYSOLDIER
	 * \
	 * \-----> FINDING <-> LETSCHOP
	 * \
	 * \-----> BUILDING
	 */
	@Override void step() {
		roundCounter++;
		switch (state) {
		case STARTING:
			currentDir = randomDirection();
			if (isEnemyArchonNear()) {
				state = GardenerState.ONLYSOLDIERS;
			} else
				state = GardenerState.FINDING;
			break;
		case FINDING:
			if (isInWoods() && lumberjackBuilded < 2) {
				state = GardenerState.LETSCHOP;
				lumberjackBuilded++;
			} else if (findSpot())
				state = GardenerState.BUILDING;
			break;
		case BUILDING:
			// try to build one tree in garden
			if (buildGarden())
				initTreeNum++;
			// water garden
			waterGarden();
			//build atleast 3 trees
			if (initTreeNum < 3)
				break;
			// force to build robots
			if (!scoutBuilded && getRobotCount(RobotType.SCOUT) == 0) {
				while (!build(RobotType.SCOUT)) {
					waterGarden();
					Clock.yield();
				}
				scoutBuilded = true;
			} else if (!soldierBuilded) {
				while (!build(RobotType.SOLDIER)) {
					waterGarden();
					Clock.yield();
				}
				soldierBuilded = true;
			}
			if (filterByType(rc.senseNearbyRobots(TeamConstants.GARDENERS_DEFAULT_FREE_SPOT_RADIUS, enemy),
					RobotType.SOLDIER).size() > 0)
				build(RobotType.SOLDIER);
			// try to build again
			double rnd = rand.nextDouble();
			if (rnd < 0.25) {
				if (getRobotCount(RobotType.SCOUT) < 2)
					build(RobotType.SCOUT);
				else if (getRobotCount(RobotType.SOLDIER) < 40)
					build(RobotType.SOLDIER);
			} else if (rnd < 0.5) {
				if (getRobotCount(RobotType.LUMBERJACK) < 3)
					build(RobotType.LUMBERJACK);
				else if (getRobotCount(RobotType.SOLDIER) < 40)
					build(RobotType.SOLDIER);
			} else if (rnd < 0.75) {
				if (getRobotCount(RobotType.SOLDIER) < 40)
					build(RobotType.SOLDIER);
			} else {
				if (getRobotCount(RobotType.TANK) < 3)
					build(RobotType.TANK);
			}

			break;
		case ONLYSOLDIERS:
			// while enemy archon is nearby build soldiers !!
			if (isEnemyArchonNear())
				build(RobotType.SOLDIER);
			else
				state = GardenerState.FINDING;
			break;
		case LETSCHOP:
			// build atleast one lumberjack and than try to build another in next  rounds
			while (!build(RobotType.LUMBERJACK, true)) {
				Clock.yield();
			}
			state = GardenerState.FINDING;
			break;
		}
	}
}
