package blueteam;

public abstract interface TeamConstants {
	public static final int DESIRED_NUMBER_OF_GARDENERS = 10;
	public static final int GARDENERS_COUNT_CHANNEL = 0;
	// The maximum number of tries per turn for finding a direction in which
	// the robot can move.
	public static final int GENERATING_DIR_MAX_TRIES_LIMIT = 100;
	// Probability that the moving direction of a soldier is chosen at random.
	// (Otherwise the soldier tries to move towards the enemy archon's initial
	// location.)
	public static final double SOLDIER_RANDOM_MOVE_PROB = .75;
}
