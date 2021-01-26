package v6;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
// import java.util.HashMap;
import java.util.List;
// import java.util.Map;
// import java.util.Set;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = { RobotType.POLITICIAN, RobotType.SLANDERER, RobotType.MUCKRAKER, };

    static final Direction[] directions = { Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
            Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST };

    static final int NBITS = 7;
    static final int BITMASK = (1 << NBITS) - 1;
    static Boolean bounceOn = false;
    static int turnCount;
    static int botsCreated;
    static int newRobot = 0;
    static int dirIndex = 0;
    static RobotType unitToBuild;
    static Direction buildDir;
    static Direction myDirection = Direction.CENTER;
    static List<Direction> bounceDirections = new ArrayList<Direction>();
    static List<Direction> directionList = Arrays.asList(directions);
    static int enlightenmentCenterId = -1;

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world. If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this
        // robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        // System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER:
                        EnlightenmentCenter.run();
                        break;
                    case POLITICIAN:
                        Politician.run();
                        break;
                    case SLANDERER:
                        Slanderer.run();
                        break;
                    case MUCKRAKER:
                        Muckraker.run();
                        break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform
                // this loop again
                Clock.yield();

            } catch (Exception e) {
                // System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType SpawnableRobotType() {
        // Create a slanderer
        if (botsCreated % 3 == 0) {
            return spawnableRobot[1];
            // Create a muckraker
        } else if (botsCreated % 10 == 0) {
            return spawnableRobot[2];
            // Create a politician
        } else {
            return spawnableRobot[0];
        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " +
        // rc.getCooldownTurns() + " "
        // + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else
            return false;
    }

    static void sendLocation(MapLocation location) throws GameActionException {
        int x = location.x, y = location.y;
        int encodedLocation = ((x & BITMASK) << NBITS) + (y & BITMASK);
        if (rc.canSetFlag(encodedLocation)) {
            rc.setFlag(encodedLocation);
        }
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param loc The current location
     * @return Direction direction to move
     * @throws GameActionException
     */

    public static Direction movePassable(MapLocation loc) throws GameActionException {
        // Compute the total weight of all items together.
        // This can be skipped of course if sum is already 1.
        ArrayList<Direction> possibleMoves = new ArrayList<Direction>();
        for (Direction dir : directions) {
            // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " +
            // rc.getCooldownTurns() + " " + rc.canMove(dir));
            if (rc.canSenseLocation(loc.add(dir))) {
                possibleMoves.add(dir);
            }
        }
        // System.out.println("Possible" + possibleMoves);
        double totalWeight = 0.0;
        for (int i = 0; i < possibleMoves.size(); i++) {
            totalWeight += Math.pow(rc.sensePassability(loc.add(possibleMoves.get(i))), 2);
        }

        // Now choose a random item.
        int randomItem = 0;
        for (double r = Math.pow(Math.random() * totalWeight, 2); randomItem < possibleMoves.size() - 1; ++randomItem) {
            r -= rc.sensePassability(loc.add(possibleMoves.get(randomItem)));
            if (r <= 0.0)
                break;
        }
        Direction myRandomDirection = possibleMoves.get(randomItem);
        // System.out.println("final:" + myRandomDirection);
        return myRandomDirection;
    }

    public static Direction Search(MapLocation loc, Direction direction) throws GameActionException {
        List<Direction> myDirections = new ArrayList<Direction>();
        myDirections.add(direction);
        myDirections.add(direction.rotateLeft());
        myDirections.add(direction.rotateRight());

        Collections.shuffle(myDirections);

        double passability = 0;
        Direction toMove = direction;
        for (Direction dir : myDirections) {
            if (rc.onTheMap(rc.getLocation().add(dir))) {
                if (rc.sensePassability(rc.getLocation().add(dir)) > passability) {
                    passability = rc.sensePassability(rc.getLocation().add(dir));
                    toMove = dir;
                }

            }
        }
        return toMove;
    }

    public static Direction Evade(MapLocation loc, Direction direction) throws GameActionException {
        List<Direction> myDirections = new ArrayList<Direction>();
        myDirections.add(direction);
        myDirections.add(direction.rotateLeft());
        myDirections.add(direction.rotateRight());
        myDirections.add(direction.rotateLeft().rotateLeft());
        myDirections.add(direction.rotateRight().rotateRight());

        Collections.shuffle(myDirections);

        double passability = 0;
        Direction toMove = direction;
        for (Direction dir : myDirections) {
            if (rc.onTheMap(rc.getLocation().add(dir))) {
                if (rc.sensePassability(rc.getLocation().add(dir)) > passability) {
                    passability = rc.sensePassability(rc.getLocation().add(dir));
                    toMove = dir;
                }
            }
        }
        return toMove;
    }
}
