package v6;

import java.util.ArrayList;
import java.util.HashSet;
// import java.util.Iterator;
import java.util.List;
import java.util.Set;

import battlecode.common.*;

public class EnlightenmentCenter extends RobotPlayer {
    static int botsCreated = 0;
    static int currentFlag = 0;
    static int flagRounds = 0;
    static boolean REQUEST_HELP = false;
    static boolean BID = false;
    static boolean BID_HIGHER = false;
    static boolean ATTACK = false;
    static Set<Integer> allRobotID = new HashSet<Integer>();
    static boolean SET_FLAG = false;
    static List<Integer> toAttack = new ArrayList<Integer>();
    static Set<Integer> capturedCenters = new HashSet<Integer>();

    public static void run() throws GameActionException {
        BidThisRound();
        checkVoteStatus();
        List<Integer> toRemove = new ArrayList<Integer>();

        // System.out.println("Size" + allRobotID.size());
        // System.out.println("Flag" + currentFlag);
        // System.out.println("Attack" + toAttack.size());
        for (int element : allRobotID) {
            if (rc.canGetFlag(element)) {
                // System.out.println("got flag" + rc.getFlag(id));
                if (rc.getFlag(element) != 0) {
                    if (rc.getFlag(element) == currentFlag - 1) {
                        toRemove.add(element);
                        SET_FLAG = true;
                        // break;
                    } else if (!capturedCenters.contains(rc.getFlag(element))) {
                        capturedCenters.add(rc.getFlag(element));
                        toAttack.add(rc.getFlag(element));
                        toRemove.add(element);
                        // System.out.println("there");
                    }
                }
            } else {
                toRemove.add(element);
            }
        }
        allRobotID.removeAll(toRemove);

        if (SET_FLAG) {
            capturedCenters.remove(currentFlag);
            currentFlag = 0;
            rc.setFlag(0);
            toAttack.remove(0);
            ATTACK = false;

        }

        if (currentFlag == 0) {
            SET_FLAG = false;
            if (toAttack.size() > 0) {
                ATTACK = true;
                currentFlag = toAttack.get(0);
                rc.setFlag(currentFlag);
            }
        }

        RobotType newRobot = decideUnitToBuild();
        int influence;
        if (newRobot == RobotType.MUCKRAKER) {
            influence = 1;
        } else if (newRobot == RobotType.POLITICIAN && botsCreated % 2 == 0) {
            influence = 19;
        } else {
            influence = Math.max((int) rc.getInfluence() - 20, 50);
        }
        if (rc.canBuildRobot(newRobot, directionList.get(dirIndex), influence)) {
            rc.buildRobot(newRobot, directionList.get(dirIndex), influence);
            allRobotID.add(rc.senseRobotAtLocation(rc.getLocation().add(directionList.get(dirIndex))).ID);
            botsCreated += 1;
        }
        dirIndex = (dirIndex + 1) % 8;

        if (BID) {
            if (!BID_HIGHER) {
                if (rc.canBid(Math.max(2, (int) rc.getInfluence() / 20))) {
                    // System.out.println("V5 Round bid" + turnCount + rc.getInfluence());
                    rc.bid(Math.max(2, (int) rc.getInfluence() / 20));
                }
            } else {
                if (rc.canBid((int) rc.getInfluence() / 5)) {
                    // System.out.println("V2 Round bid" + turnCount + rc.getInfluence());
                    rc.bid((int) rc.getInfluence() / 5);
                }
            }
        }
    }

    public static RobotType decideUnitToBuild() throws GameActionException {
        if (!ATTACK) {
            if (botsCreated % 4 == 0) {
                return RobotType.SLANDERER;
            } else if (botsCreated % 9 == 0) {
                return RobotType.POLITICIAN;
            } else {
                return RobotType.MUCKRAKER;
            }
        } else {
            if (botsCreated % 2 == 0 || botsCreated % 5 == 0) {
                return RobotType.POLITICIAN;
            } else if (botsCreated % 3 == 0) {
                return RobotType.MUCKRAKER;
            } else {
                return RobotType.SLANDERER;
            }
        }

    }

    public static void BidThisRound() {
        if (rc.getTeamVotes() > 750) {
            BID = false;
        } else {
            BID = true;
        }
    }

    public static void checkVoteStatus() {
        if (rc.getTeamVotes() < (int) rc.getRoundNum() / 3) {
            BID_HIGHER = true;
        } else {
            BID_HIGHER = false;
        }
    }
}
