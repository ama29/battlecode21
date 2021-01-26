package v6;

import battlecode.common.*;

public class Politician extends RobotPlayer {
    static Boolean directionFixed = false;
    static Direction moveDir;

    static MapLocation politicianTarget = null;
    static Boolean MOVE_HERE = false;
    static Boolean GUARD = false;
    static int threshold = 1;
    static boolean guardDistance;
    static boolean EXPLODE = false;
    static MapLocation centralStation;
    static int STOPPED = 0;
    static Direction currentDir = Direction.NORTH;
    static boolean PROTECT = false;

    public static void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        RobotInfo[] movable = rc.senseNearbyRobots(sensorRadius, enemy);
        RobotInfo[] checkCenters = rc.senseNearbyRobots(actionRadius);

        if (EXPLODE) {
            rc.empower(actionRadius);
            return;
        }

        rc.setFlag(0);

        if (enlightenmentCenterId == -1) {
            for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    enlightenmentCenterId = robot.ID;
                    centralStation = robot.location;
                    currentDir = rc.getLocation().directionTo(robot.location).opposite();
                }
            }
        }

        checkGuard();
        protect();

        for (RobotInfo robot : checkCenters) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getTeam() != rc.getTeam()) {
                sendLocation(robot.location);
                if (rc.canEmpower(actionRadius) && rc.getConviction() >= 10 && !PROTECT) {
                    EXPLODE = true;
                    return;
                }
            }
        }

        if (rc.canGetFlag(enlightenmentCenterId) && !PROTECT) {
            if (rc.getFlag(enlightenmentCenterId) != 0) {
                politicianTarget = getLocationFromFlag(rc.getFlag(enlightenmentCenterId));
            } else {
                politicianTarget = null;
            }
        }

        if (politicianTarget != null && !PROTECT) {
            basicBug(politicianTarget);
            return;
        } else if (movable.length >= threshold) {
            int x = 0;
            int y = 0;
            for (RobotInfo info : movable) {
                MapLocation enemyLoc = info.getLocation();
                x += enemyLoc.x;
                y += enemyLoc.y;
            }
            MapLocation finalLocation = new MapLocation((int) x / movable.length, (int) y / movable.length);
            myDirection = rc.getLocation().directionTo(finalLocation);
        } else {
            myDirection = Search(rc.getLocation(), currentDir);
        }

        if (attackable.length >= threshold && rc.canEmpower(actionRadius) && rc.getConviction() > 10) {
            rc.empower(actionRadius);
            // System.out.println("empowered");
            return;
        }

        if (guardDistance) {
            if (rc.onTheMap(rc.getLocation().add(myDirection))) {
                if (rc.isReady()) {
                    if (tryMove(myDirection)) {

                    } else if (tryMove(myDirection.rotateLeft())) {

                    } else if (tryMove(myDirection.rotateRight())) {

                    } else {
                        currentDir = myDirection.opposite();
                    }

                }
            } else {
                currentDir = myDirection.opposite();
            }
        }
    }

    static void checkGuard() {
        if (rc.getRoundNum() > 1200) {
            threshold = 1;
        } else {
            threshold = (int) Math.floor(rc.getConviction() / 10);
        }
        guardDistance = true;
    }

    static void protect() {
        if (rc.getConviction() < 20) {
            PROTECT = true;
        } else {
            PROTECT = false;
        }
    }

    // static void sendLocation(MapLocation location, int extraInformation) throws
    // GameActionException {
    // int x = location.x, y = location.y;
    // int encodedLocation = (extraInformation << (2 * NBITS)) + ((x & BITMASK) <<
    // NBITS) + (y & BITMASK);
    // if (rc.canSetFlag(encodedLocation)) {
    // rc.setFlag(encodedLocation);
    // }
    // }

    static void stopLocation(MapLocation location) throws GameActionException {
        int x = location.x, y = location.y;
        int encodedLocation = ((x & BITMASK) << NBITS) + (y & BITMASK);
        if (rc.canSetFlag(encodedLocation - 1)) {
            rc.setFlag(encodedLocation - 1);
        }
    }

    static MapLocation getLocationFromFlag(int flag) {
        int y = flag & BITMASK;
        int x = (flag >> NBITS) & BITMASK;
        // int extraInformation = flag >> (2*NBITS);

        MapLocation currentLocation = rc.getLocation();
        int offsetX128 = currentLocation.x >> NBITS;
        int offsetY128 = currentLocation.y >> NBITS;
        MapLocation actualLocation = new MapLocation((offsetX128 << NBITS) + x, (offsetY128 << NBITS) + y);

        // You can probably code this in a neater way, but it works
        MapLocation alternative = actualLocation.translate(-(1 << NBITS), 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(1 << NBITS, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, -(1 << NBITS));
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, 1 << NBITS);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        return actualLocation;
    }

    ////////////////////////////////////////////////////////////////////////////
    // BASIC BUG - just follow the obstacle while it's in the way
    // not the best bug, but works for "simple" obstacles
    // for better bugs, think about Bug 2!

    static final double passabilityThreshold = 0.3;
    static Direction bugDirection = null;

    static void basicBug(MapLocation target) throws GameActionException {
        Direction d = rc.getLocation().directionTo(target);
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] checkCenters = rc.senseNearbyRobots(actionRadius);
        if (rc.getLocation().isWithinDistanceSquared(target, 4)) {
            for (RobotInfo robot : checkCenters) {
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getTeam() != rc.getTeam()
                        && rc.canEmpower(4)) {
                    rc.empower(4);
                    return;
                }
            }
            stopLocation(target);
            enlightenmentCenterId = -1;
            return;
        } else if (rc.isReady()) {
            myDirection = Search(rc.getLocation(), d);
            if (tryMove(myDirection)) {

            } else if (tryMove(myDirection.rotateLeft())) {

            } else if (tryMove(myDirection.rotateRight())) {

            }
        }
    }
}