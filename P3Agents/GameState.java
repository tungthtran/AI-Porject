package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.util.*;
/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {

     class SimUnit {
        int ID;
        int range;
        int x;
        int y;
        int baseHP;
        int HP;
        int damage;

        public SimUnit(Unit.UnitView unitView) {
            ID = unitView.getID();
            x = unitView.getXPosition();
            y = unitView.getYPosition();
            HP = unitView.getHP();

            range = unitView.getTemplateView().getRange();
            baseHP = unitView.getTemplateView().getBaseHealth();
            damage = unitView.getTemplateView().getdamageack();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimUnit simUnit = (SimUnit) o;
            return ID == simUnit.ID;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ID);
        }
    }
     
    private State.StateView state;
    private List<SimUnit> player0Units;
    private List<SimUnit> player1Units;
    private int xMax;
    private int yMax;
    private State.StateView state;
     
    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns the IDs of all of the obstacles in the map
     * state.getResourceNode(int resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     * 
     * You can get a list of all the units belonging to a player with the following command:
     * state.getUnitIds(int playerNum): gives a list of all unit IDs beloning to the player.
     * You control player 0, the enemy controls player 1.
     * 
     * In order to see information about a specific unit, you must first get the UnitView
     * corresponding to that unit.
     * state.getUnit(int id): gives the UnitView for a specific unit
     * 
     * With a UnitView you can find information about a given unit
     * unitView.getXPosition() and unitView.getYPosition(): get the current location of this unit
     * unitView.getHP(): get the current health of this unit
     * 
     * SEPIA stores information about unit types inside TemplateView objects.
     * For a given unit type you will need to find statistics from its Template View.
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getdamageack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
        this.state = state;

        state.getUnits(0).forEach((unit) -> {
            player0Units.add(new SimUnit(unit));
        });

        state.getUnits(1).forEach((unit) -> {
            player1Units.add(new SimUnit(unit));
        });

        this.xMax = state.getXExtent();
        this.yMax = state.getYExtent();
    }

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {
        double utility = 0.0;
        utility += 1000 * healthUtility();
        utility += 1 * distanceUtility();
        return utility;
    }
    
    
    private double healthUtility() {
        double health = 0.0;
        for(SimUnit archer : player1Units) {
            health -= (double)archer.HP/archer.baseHP;
        }
        for(SimUnit footman : player0Units) {
            health += (double)footman.HP/footman.baseHP;
        }
        return health;
    }
    private double distanceUtility() {}

    private boolean isTerminated() {
      return player0Units.size() == 0 || player1Units.size() == 0;
    }
    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     * 
     * It may be useful to be able to create a SEPIA Action. In this assignment you will
     * deal with movement and attacking actions. There are static methods inside the Action
     * class that allow you to create basic actions:
     * Action.createPrimitiveAttack(int attackerID, int targetID): returns an Action where
     * the attacker unit attacks the target unit.
     * Action.createPrimitiveMove(int unitID, Direction dir): returns an Action where the unit
     * moves one space in the specified direction.
     *
     * You may find it useful to iterate over all the different directions in SEPIA. This can
     * be done with the following loop:
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     * 
     * If you wish to explicitly use a Direction you can use the Direction enum, for example
     * Direction.NORTH or Direction.NORTHEAST.
     * 
     * You can check many of the properties of an Action directly:
     * action.getType(): returns the ActionType of the action
     * action.getUnitID(): returns the ID of the unit performing the Action
     * 
     * ActionType is an enum containing different types of actions. The methods given above
     * create actions of type ActionType.PRIMITIVEATTACK and ActionType.PRIMITIVEMOVE.
     * 
     * For attack actions, you can check the unit that is being attacked. To do this, you
     * must cast the Action as a TargetedAction:
     * ((TargetedAction)action).getTargetID(): returns the ID of the unit being attacked
     * 
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
        //check if one player's all units dead -> end game
        if (player0Units.size() <= 0 || player1Units.size() <= 0) {
            return null;
        }

        List<GameStateChild> childNodes = new ArrayList<GameStateChild>();
        Map<Integer, Action> actions = new HashMap<Integer, Action>();
        int numArchers = player1Units.size();

        //get IDs and UnitView of the first Footman and Archer
        int firstFootmanID = player0Units.get(0);
        UnitView firstFootmanView = state.getUnit(firstFootmanID);
        int secondFootmanID;
        UnitView secondFootmanView;
        //get IDs and UnitView of the second Footman
        if (player0Units.size() == 2) {
            secondFootmanID = player0Units.get(1);
            secondFootmanView = state.getUnit(secondFootmanID);
        }

        int firstArcherID = player1Units.get(0);
        UnitView firstArcherView = state.getUnit(firstArcherID);
        int secondArcherID;
        UnitView secondArcherView;
        if (player1Units.size() == 2) {
            secondArcherID = player1Units.get(1);
            secondArcherView = state.getUnit(secondArcherID);
        }

        //If it's our turn Footmen, find all possible actions and associated state
        if (playerNum == 0) {
            //actions and state of the first Footman
            for (Direction direction1 : Directions.values()) {
                Action footmanAct1 = footmanAct(direction1, firstFootmanID, firstFootmanView, player1Units);
                if (footmanAct1 != null) {
                    GameState childState1 = new GameState(this, footmanAct1);
                    //if there is only one Footman, create a child and add to list and continue consider other directions
                    if (player0Units.size() <= 1) {
                        actions.put(firstFootmanID, footmanAct1);
                        childNodes.add(new GameStateChild(actions, childState1));
                        continue;
                    }
                }
                
                //actions and state of the second Footman if there are two Footmen
                for (Direction direction2 : Directions.values()) {
                    Map<Integer, Action> actionsBothFootman = new HashMap<Integer, Action>();
                    Action footmanAct2 = footmanAct(direction2, secondFootmanID, secondFootmanView, player1Units);
                    if (footmanAct2 != null) {
                        GameState childStateBothFootman = new GameState(childState1, footmanAct2);
                        actionsBothFootman.put(firstFootmanID, footmanAct1);
                        actionsBothFootman.put(secondFootmanID, footmanAct2);
                        childNodes.add(new GameStateChild(actionsBothFootman, childStateBothFootman));
                    }
                }
            }
        }

        //if it's Archer's turn, find all possible actions and associated state
        else {
            for(Direction direction1 : Directions.values()) {
                Action archerAct1 = archerAct(direction1, firstArcherID, firstArcherView, player0Units);
                if (archerAct1 != null) {
                    GameState childState1 = new GameState(this, archerAct1);
                    //if there is only one Archer, create a child and add to list and continue consider other direction
                    if (player0Units.size() <= 1) {
                        actions.put(firstArcherID, archerAct1);
                        childNodes.add(new GameStateChild(actions, childState1));
                        continue;
                    }
                }

                //actions and state of the second Footman if there are two Footmen
                for (Direction direction2 : Directions.values()) {
                    Map<Integer, Action> actionsBothArchers = new HashMap<Integer, Action>();
                    Action archerAct2 = footmanAct(direction2, secondArcherID, secondArcherView, player0Units);

                    if (archerAct2 != null) {
                        GameState childStateBothFootman = new GameState(childState1, archerAct2);
                        actionsBothArchers.put(firstArcherID, archerAct1);
                        actionsBothArchers.put(secondArcherID, archerAct2);
                        childNodes.add(new GameStateChild(actionsBothArchers, childStateBothFootman));
                    }
                }    
            }
        }
        return childNodes;
    }

    public Action archerAct(Direction direction, int archerID, UnitView archerView, List<int> player0Units) {
        int dirX = direction.xComponent();
        int dirY = direction.yComponent();
        int newarcherX = archerView.getXPosition() + dirX;
        int newarcherY = archerView.getYPosition() + dirY;
        int firstFootmanID = player0Units.get(0);
        UnitView firstFootmanView = state.getUnit(firstFootmanID);
        int secondFootmanID;
        UnitView secondFootmanView;
        //get IDs and UnitView of the second Footman
        if (player0Units.size() == 2) {
            secondFootmanID = player0Units.get(1);
            secondFootmanView = state.getUnit(secondFootmanID);
        }
        if ((dirX == 0 || dirY == 0) && (newarcherX >= 0 && newarcherX <= mapXExtent && newarcherY >=0 && newarcherY <=mapYExtent)
                    && !resourceLocations.contains(new MapLocation(newarcherX, newarcherY))) {
            //if archer is within the range of the first Footman or both Footman, attack the first Footman
            if(getDistance(firstFootmanView,archerView) <= archerView.getRange()) {
                return Action.createPrimitiveAttack(archerID, firstFootmanID);
            }

            //if archer is within the range of the second Footman, attack the second Footman
            else if (getDistance(secondFootmanView,archerView) <= archerView.getRange()) {
                return Action.createPrimitiveAttack(archerID, secondFootmanID);
            }

            //if archer is not within the range, move towards direction
            else {
                return Action.createPrimitiveMove(archerID, direction);
            }
        }
        return null;
    }

    public Action footmanAct(Direction direction, int footmanID, UnitView footmanView, List<int> player1Units) {
        int dirX = direction.xComponent();
        int dirY = direction.yComponent();
        int newfootmen1X = footmanView.getXPosition() + dirX;
        int newfootmen1Y = footmanView.getYPosition() + dirY;
        int archerID1 = player1Units.get(0);
        UnitView archerView1 = state.getUnit(archerID1);
        int archerID2;
        UnitView archerView2;
        if (player1Units.size() == 2) {
            archerID2 = player1Units.get(1);
            archerView2 = state.getUnit(archerID2);
        }

        //Only consider Footmen move if it's not diagonal, within the boundaries of the map, and dont collide to any obstacles
        if ((dirX == 0 || dirY == 0) && (newfootmen1X >= 0 && newfootmen1X <= mapXExtent && newfootmen1Y >=0 && newfootmen1Y <=mapYExtent)
            && !resourceLocations.contains(new MapLocation(newfootmen1X, newfootmen1Y))) {
            //if adjacent to the first Archer or both Archers, attack the first Archer
            if (isAdjacentTo(footmanView.getXPosition(), footmanView.getYPosition(), archerView1)) {
                return Action.createPrimitiveAttack(footmanID, archerID1);
            }
            //if adjacent to the second Archer, attack it
            else if (isAdjacentTo(footmanView.getXPosition(), footmanView.getYPosition(), archerView2)) {
                return Action.createPrimitiveAttack(footmanID, archerID2);
            }
            //else it will move towards an Archer
            else {
                return Action.createPrimitiveMove(footmanID, direction);
            }
        }
        return null;
    }

    //get distance between 2 units
    public int getDistance(UnitView unit1, UnitView unit2) {
        int distance = 0;
        int disXsquared = Math.pow(unit2.x - unit1.x, 2);
        int disYsquared = Math.pow(unit2.y - unit1.y, 2);
        distance = Math.pow((disXsquared + disYsquared), 1/2);
        return distance;
    }

    //check if 2 units 
    public boolean isAdjacentTo(int footmenX, int footmenY, UnitView unit2) {
        return Math.abs(footmenX - unit2.getXPosition()) <= 1 && Math.abs(footmenY - unit2.getYPosition()) <= 1;
    }
}
