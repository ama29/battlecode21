package v6;

import battlecode.common.*;

public class Muckraker extends RobotPlayer {
    static Boolean directionFixed = false;
    static Direction moveDir;
    static int enlightenmentCenterId = -1;
    static MapLocation centralStation;
    static int STOPPED = 0;
    static Direction currentDir;

    public static void run() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        int PolRadius = RobotType.POLITICIAN.actionRadiusSquared;
        int sensorRadius = rc.getType().sensorRadiusSquared;
        RobotInfo[] checkCenters = rc.senseNearbyRobots(PolRadius);
        RobotInfo[] movable = rc.senseNearbyRobots(sensorRadius, enemy);

        rc.setFlag(0);
        for (RobotInfo robot : checkCenters) {
            if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER && robot.getTeam() != rc.getTeam()) {
                sendLocation(robot.getLocation());
                currentDir = rc.getLocation().directionTo(robot.location).opposite();
            }
        }

        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    // System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }

        if (enlightenmentCenterId == -1) {
            for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    enlightenmentCenterId = robot.ID;
                    centralStation = robot.location;
                    currentDir = rc.getLocation().directionTo(robot.location).opposite();
                }
            }
        }

        if (rc.getID() % 2 == 0) {
            if (movable.length >= 1) {
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
        } else {
            myDirection = Search(rc.getLocation(), currentDir);
        }
        
        if (rc.onTheMap(rc.getLocation().add(myDirection))) {
            if (rc.isReady()) {
                if (tryMove(myDirection)) {
                    currentDir = myDirection;
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
