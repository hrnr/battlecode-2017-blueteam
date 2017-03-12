package blueteam;

import battlecode.common.MapLocation;

/**
 * Proxy class for ImporatantLocations, stores data loaded from the broadcast.
 *
 * @author Tomas
 *
 */
public class LocationRecord {

	public MapLocation location;
	public int lastActiveRound;
	// Two locations closer than DISTANCE_EPSILON will be considered to be the
	// same location.
	private static final int DISTANCE_EPSILON = 5;

	public LocationRecord(MapLocation location, int turnLastActive) {
		super();
		this.location = location;
		this.lastActiveRound = turnLastActive;
	}

	/**
	 * Partially auto-generated equals (uses DISTANCE_EPSILON to compare
	 * locations)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocationRecord other = (LocationRecord) obj;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (location.distanceTo(other.location) > DISTANCE_EPSILON)
			return false;
		return true;
	}

}
