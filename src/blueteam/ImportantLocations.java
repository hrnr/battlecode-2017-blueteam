package blueteam;

import java.util.Arrays;
import java.util.Comparator;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/**
 * Provides access to the locations saved in the broadcast. Enables robots to
 * report new important locations.
 *
 * @author Tomas
 *
 */
public class ImportantLocations {
	private static final int RECORD_SIZE = 3;
	private static final int DEFAULT_LOCATIONS_NUMBER = 5;
	private static final int DEFAULT_ROUNDS_TO_GET_OUTDATED = 15;

	private final int first_locations_channel;
	private final int locations_number;
	private final int rounds_to_get_outdated;
	private RobotController rc;
	private LocationRecord[] proxy;
	private int roundProxyUpdated = -1;

	public ImportantLocations(RobotController rc, int first_location_channel, int locations_number,
			int rounds_to_get_outdated) {
		this.rc = rc;
		this.first_locations_channel = first_location_channel;
		this.locations_number = locations_number;
		this.rounds_to_get_outdated = rounds_to_get_outdated;
	}

	public ImportantLocations(RobotController rc, int first_location_channel) {
		this(rc, first_location_channel, DEFAULT_LOCATIONS_NUMBER, DEFAULT_ROUNDS_TO_GET_OUTDATED);
	}

	/**
	 * Gets a list of active locations, sorted by distance from the robot.
	 *
	 * @return
	 */
	public MapLocation[] getActiveLocations() {
		LocationRecord[] loc = getLocations();
		int outdatedTime = rc.getRoundNum() - rounds_to_get_outdated;
		MapLocation me = rc.getLocation();
		return Arrays.stream(loc).filter(x -> x.lastActiveRound != 0 && x.lastActiveRound >= outdatedTime)
				.map(x -> x.location).sorted(Comparator.comparingDouble(x -> x.distanceTo(me)))
				.toArray(x -> new MapLocation[x]);
	}

	/**
	 * Puts the location into the shared list of important locations. If the
	 * capacity is full, replaces the oldest location.
	 *
	 * @param loc
	 */
	public void reportLocation(MapLocation loc) {
		LocationRecord[] records = getLocations();
		for (int i = 0; i < records.length; i++) {
			if (records[i].equals(loc)) {
				writeLocation(i, loc);
				return;
			}
		}
		// replace the least active location
		int minIndex = 0;
		int minRound = Integer.MAX_VALUE;
		for (int i = 0; i < records.length; i++) {
			if (minRound > records[i].lastActiveRound) {
				minIndex = i;
				minRound = records[i].lastActiveRound;
			}
		}
		writeLocation(minIndex, loc);
	}

	private LocationRecord loadLocation(int id) {
		int record_start = record_start(id);
		float mapX = 0;
		float mapY = 0;
		int lastActive = 0;
		try {
			mapX = rc.readBroadcastFloat(record_start);
			mapY = rc.readBroadcastFloat(record_start + 1);
			lastActive = rc.readBroadcast(record_start + 2);
		} catch (GameActionException e) {
			e.printStackTrace();
		}

		return new LocationRecord(new MapLocation(mapX, mapY), lastActive);
	}

	private void writeLocation(int id, MapLocation loc) {
		int record_start = record_start(id);
		try {
			rc.broadcastFloat(record_start, loc.x);
			rc.broadcastFloat(record_start + 1, loc.y);
			rc.broadcast(record_start + 2, rc.getRoundNum());
		} catch (GameActionException e) {
			e.printStackTrace();
		}

	}

	private int record_start(int id) {
		return first_locations_channel + id * RECORD_SIZE;
	}

	private LocationRecord[] getLocations() {
		if (roundProxyUpdated != rc.getRoundNum()) {
			proxy = new LocationRecord[locations_number];
			for (int i = 0; i < proxy.length; i++) {
				proxy[i] = loadLocation(i);
			}
		}
		roundProxyUpdated = rc.getRoundNum();
		return proxy;
	}

}
