package blueteam;

import battlecode.common.Direction;

public abstract interface TeamConstants {
	public static final int DESIRED_NUMBER_OF_GARDENERS = 6;
	public static final int GARDENERS_COUNT_CHANNEL = 0;

	// The maximum number of tries per turn for finding a direction in which
	// the robot can move.
	public static final int GENERATING_DIR_MAX_TRIES_LIMIT = 100;
	// Probability that the moving direction of a soldier is chosen at random.
	// (Otherwise the soldier tries to move towards the enemy archon's initial
	// location.)
	public static final double SOLDIER_RANDOM_MOVE_PROB = .75;
	/**
	 * robots having less than MINIMUM_HEALTH_PERCENTAGE HP are considered dead
	 */
	public static final double MINIMUM_HEALTH_PERCENTAGE = 0.2;
	/**
	 * minimum absolute amount of HP to consider robot alive
	 */
	public static final int MINIMUM_HEALTH = 5;
	/**
	 * channels for robot counters. this is array, to stay safe, channels 0..10
	 * are reserved for this.
	 */
	public static final int ROBOT_COUNTERS_BEGIN = 0;
	public static final int MINIMUM_BULLETS_TO_SAVE = 150;

	public static final int GARDENERS_DIRECT_PATH_LENGTH = 30;
	public static final float GARDENERS_DEFAULT_FREE_SPOT_RADIUS = 6.5f;

	public static final Direction GARDENERS_GARDEN_ENTRANCE = Direction.WEST;
	public static final int GARDENER_NUM_OF_TREES_TO_BUILD_LUMBER = 5;
}
