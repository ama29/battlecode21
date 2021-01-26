package v6;

import battlecode.common.*;

public class Slanderer extends RobotPlayer {
    static MapLocation slandererTarget = null;
    static Boolean directionFixed = false;
    static Boolean foundCenter = false;
    static Direction moveDir;
    static int enlightenmentCenterId = -1;
    static MapLocation centralStation;
    static int STOPPED = 0;
    static Direction currentDir;
    static Direction homeDir;

    public static void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] movable = rc.senseNearbyRobots(sensorRadius, enemy);
        RobotInfo[] checkCenters = rc.senseNearbyRobots(sensorRadius);

        rc.setFlag(0);
        if (enlightenmentCenterId == -1) {
            for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    enlightenmentCenterId = robot.ID;
                    centralStation = robot.location;
                    homeDir = rc.getLocation().directionTo(robot.location);
                    currentDir = rc.getLocation().directionTo(robot.location).opposite();
                }
            }
        }

        if (rc.canGetFlag(enlightenmentCenterId)) {
            if (rc.getFlag(enlightenmentCenterId) != 0) {
                slandererTarget = getLocationFromFlag(rc.getFlag(enlightenmentCenterId));
            } else {
                slandererTarget = null;
            }
        }
        for (RobotInfo robot : checkCenters) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getTeam() != rc.getTeam()) {
                sendLocation(robot.location);
            }
        }

        if (movable.length >= 1) {
            int x = 0;
            int y = 0;
            for (RobotInfo info : movable) {
                MapLocation enemyLoc = info.getLocation();
                x += enemyLoc.x;
                y += enemyLoc.y;
            }
            MapLocation finalLocation = new MapLocation((int) x / movable.length, (int) y / movable.length);
            currentDir = rc.getLocation().directionTo(finalLocation).opposite();
            myDirection = Evade(rc.getLocation(), currentDir);


        } else if (slandererTarget != null) {
            basicBug(slandererTarget);
            return;

        } else {
            myDirection = Evade(rc.getLocation(), currentDir);
        }
        
        if (rc.getLocation().isWithinDistanceSquared(centralStation,
                RobotType.ENLIGHTENMENT_CENTER.sensorRadiusSquared - 1)) {
            if (rc.onTheMap(rc.getLocation().add(myDirection))) {
                if (rc.isReady()) {
                    tryMove(myDirection);
                }
            } else {
                currentDir = homeDir;
            }
        } else {
            if (rc.isReady()) {
                currentDir = homeDir;
                tryMove(currentDir);
            }
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

    static void basicBug(MapLocation target) throws GameActionException {
        Direction d = rc.getLocation().directionTo(target);
        tryMove(d.opposite());
        return;
    }
}