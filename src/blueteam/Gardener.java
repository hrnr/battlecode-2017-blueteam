package blueteam;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.common.TreeInfo;

public class Gardener extends Robot {

	Gardener(RobotController rc) {
		super(rc);
	}

	enum GardenerState {
		FINDING, BUILDING, STARTING, ONLYSOLDIERS, LETSCHOP
	}

	GardenerState state = GardenerState.STARTING;
	int roundCounter = 0;
	int birthRound = 0;
	Direction currentDir = randomDirection();
	Direction gardenEntrance = null;
	int lumberjackBuilded = 0;
	boolean soldierBuilded = false;
	int initTreeNum = 0;
	float currentHealth = rc.getType().getStartingHealth();

	void hideEnemy(){
		if (rc.getRoundNum() > 150)
			return;
		//E
		MapLocation init = rc.getInitialArchonLocations(enemy)[0];
		MapLocation loc = init.add(Direction.NORTH,4);
		loc = loc.add(Direction.WEST,9);
		MapLocation loc2 = init.add(Direction.NORTH,4);
		loc2 = loc2.add(Direction.WEST,1);
		rc.setIndicatorLine(loc,loc2, 10,10,10);

		loc = loc.add(Direction.SOUTH,4);
		loc2 = loc2.add(Direction.SOUTH,4);
		rc.setIndicatorLine(loc,loc2, 10,10,10);

		loc = loc.add(Direction.SOUTH,4);
		loc2 = loc2.add(Direction.SOUTH,4);
		rc.setIndicatorLine(loc,loc2, 10,10,10);

		loc2 = init.add(Direction.WEST,9);
		loc2 = loc2.add(Direction.NORTH,4);
		rc.setIndicatorLine(loc,loc2, 10,10,10);
		//Z

		loc = init.add(Direction.EAST,9);
		loc = loc.add(Direction.NORTH,4);
		loc2 = init.add(Direction.EAST,1);
		loc2 = loc2.add(Direction.NORTH,4);
		rc.setIndicatorLine(loc,loc2, 10,10,10);

		loc2 = loc2.add(Direction.SOUTH,8);
		rc.setIndicatorLine(loc,loc2, 10,10,10);

		loc = loc.add(Direction.SOUTH,8);
		rc.setIndicatorLine(loc,loc2, 10,10,10);
	}

	/**
	 * Searches for suitable location to start building hexagonal tree garden
	 *
	 * @param radius radius to search for other gardeners
	 * @return true if is gardener in suitable place
	 */
	boolean findSpot(float radius) {
		// get robots in radius
		RobotInfo[] nearbyRobots = rc.senseNearbyRobots(radius);

		if (roundCounter > 80) {
			roundCounter = 0;
			currentDir = randomDirection();
		}
		boolean rdyToBuild = true;
		//check for robots
		for (RobotInfo robot : nearbyRobots)
			if (robot.getTeam().equals(rc.getTeam()))
				if (robot.getType().equals(RobotType.GARDENER))
					rdyToBuild = false;
		if (!rdyToBuild) {
			try {
				if (!rc.onTheMap(rc.getLocation(), radius)) {
					rdyToBuild = false;
					roundCounter += 5;
				}

				boolean success = false;
				int angle = 10;
				Direction newWay = currentDir;

				// First, try intended direction
				if (rc.canMove(currentDir)) {
					rc.move(currentDir);
					success = true;
				}
				// Now try a bunch of similar angles
				while (!success) {
					newWay = currentDir.rotateLeftDegrees(rand.nextInt(angle) - angle / 2);
					// Try the offset of the left side
					if (rc.canMove(newWay)) {
						rc.move(newWay);
						success = true;
					}
					angle += 4;
				}
				currentDir = newWay;
				// A move never happened, so return false.
			} catch (GameActionException e) {
				// this can't actually happen since we always ask canMove first
			}
		}
		return rdyToBuild;
	}

	/**
	 * Default value for radius should be 3, (gardener,tree,space,tree,gardener),
	 * but should be changed if building tanks (as they take 2 radius)
	 *
	 * @return
	 */
	boolean findSpot() {
		return findSpot(6.5f);
	}

	/**
	 * Builds tree every 30 degrees
	 *
	 * @return true if was planted
	 */
	boolean buildGarden() {

		boolean ret = false;
		if (gardenEntrance == null)
			setGardenEntrance();
		Direction dir = gardenEntrance;
		dir = dir.rotateRightDegrees(60);
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
		if (gardenEntrance == null)
			setGardenEntrance();
		TreeInfo[] trees = rc.senseNearbyTrees(1.5f, rc.getTeam());
		// first water dying tree
		for (int i = 0; i < trees.length; i++) {
			if (trees[i].getHealth() < trees[i].getMaxHealth() / 2) {
				if (rc.canWater(trees[i].getLocation())) {
					try {
						rc.water(trees[i].getLocation());
					} catch (GameActionException e) {
					}
				}
				Clock.yield();
			}
		}
		// now water every tree
		for (int i = 0; i < trees.length; i++) {
			if (rc.canWater(trees[i].getLocation())) {
				try {
					rc.water(trees[i].getLocation());
				} catch (GameActionException e) {
				}
			}
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
		if (gardenEntrance == null)
			setGardenEntrance();
		if (!random)
			dir = gardenEntrance;
		else
			dir = randomFreeDirection();
		if (rc.canBuildRobot(type, dir)) {
			try {
				rc.buildRobot(type, dir);
				return true;
			} catch (GameActionException e) {
				return false;
			}
		} else {
			dir = randomFreeDirection();
			try {
				rc.buildRobot(type, dir);
				return true;
			} catch (GameActionException e) {
				return false;
			}
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
		TreeInfo[] nearbyTrees = rc.senseNearbyTrees(TeamConstants.GARDENERS_DEFAULT_FREE_SPOT_RADIUS, Team.NEUTRAL);
		if (nearbyTrees.length > TeamConstants.GARDENER_NUM_OF_TREES_TO_BUILD_LUMBER)
			return true;
		else
			return false;
	}

	/**
	 * Set garden entrance towards enemy, helps gardeners in defence and soldiers have easier path
	 */
	void setGardenEntrance() {
		gardenEntrance = rc.getLocation().directionTo(rc.getInitialArchonLocations(enemy)[0]);
		gardenEntrance = Direction.EAST.rotateRightDegrees(((int) gardenEntrance.getAngleDegrees() / 60) * 60);
	}

	void checkHealth() {
		if (rc.getHealth() < currentHealth) {
			currentHealth = rc.getHealth();
			combatLocations.reportLocation(rc.getLocation());
		}

	}

	@Override void dodge() {

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
	@Override
	void step() {
		checkHealth();
		hideEnemy();
		roundCounter++;
		switch (state) {
		case STARTING:
			birthRound = rc.getRoundNum();
			currentDir = randomDirection();
			if (gardenEntrance == null)
				setGardenEntrance();
			if (isEnemyArchonNear()) {
				state = GardenerState.ONLYSOLDIERS;
			} else
				state = GardenerState.FINDING;
			break;
		case FINDING:
			// after 100 round of life, give it up and in 160 degree range towards enemy loc., than build.
			if (rc.getRoundNum() - birthRound > 100) {
				Direction dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(enemy)[0]);
				dir = dir.rotateLeftDegrees(rand.nextInt(160) - 80);
				while (rc.getRoundNum() - birthRound < 160) {
					tryMove(dir);
					Clock.yield();
				}
				state = GardenerState.BUILDING;
			}
			if (getRobotCount(RobotType.SCOUT) == 0) {
				build(RobotType.SCOUT);
				Clock.yield();
			}
			if (isInWoods() && lumberjackBuilded < 1) {
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
			//build at least 2 trees
			if (initTreeNum < 2)
				break;
			// force to build robots
			if (!soldierBuilded) {
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
			if (rnd < 0.5) {
				if (getRobotCount(RobotType.LUMBERJACK) < TeamConstants.MAX_NUMBER_LUMBERJACKS)
					build(RobotType.LUMBERJACK);
				else if (getRobotCount(RobotType.SOLDIER) < TeamConstants.MAX_NUMBER_SOLDIERS)
					build(RobotType.SOLDIER);
			} else {
				if (getRobotCount(RobotType.SOLDIER) < TeamConstants.MAX_NUMBER_SOLDIERS)
					build(RobotType.SOLDIER);
			}

			break;
		case ONLYSOLDIERS:
			// while enemy archon is nearby build soldiers !!
			if (isEnemyArchonNear()) {
				if (getRobotCount(RobotType.SOLDIER) < TeamConstants.MAX_NUMBER_SOLDIERS) {
					while (!build(RobotType.SOLDIER)) {
						Clock.yield();
					}
					state = GardenerState.BUILDING;
				}
			}
				else
					state = GardenerState.FINDING;
			break;
		case LETSCHOP:
			// build atleast one lumberjack and than try to build another in next  rounds
			while (!build(RobotType.LUMBERJACK)) {
				Clock.yield();
			}
			state = GardenerState.FINDING;
			break;
		}
	}
}
