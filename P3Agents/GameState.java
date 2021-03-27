package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.environment.model.state.ResourceNode;

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
 
     // an inner class represent a simulate unit
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
            damage = unitView.getTemplateView().getBasicAttack();
        }
        
        // move by xOffset and yOffset
         public void move(int xOffset, int yOffset) {
            this.x += xOffset;
            this.y += yOffset;
        }

        public void attack(SimUnit target){
            target.loseHPBy(this.damage);
        }

        public void loseHPBy(int amount) {
            this.HP -= amount;
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
    class MapLocation {
       int x;
       int y;
       public MapLocation(int x, int y) {
         this.x = x;
         this.y = y; 
       }
       
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MapLocation location = (MapLocation) o;
            return this.x == location.x && this.y == location.y ;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
     }  
    private State.StateView state;
    private List<SimUnit> player0Units;
    private List<SimUnit> player1Units;
    private int xMax;
    private int yMax;
    // is true if this is player 0 turn, false otherwise
    private boolean player0Turn;
    // a map to keep track of the sim units
    private Map<Integer, SimUnit> UnitIdMap = new HashMap<>();
    private Set<MapLocation> resourcesLocation = new HashSet<>();

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
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit type deals
     * unitView.getTemplateView().getBaseHealth(): The initial amount of health of this unit type
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
        this.state = state;
        player0Turn = true;
        state.getUnitIds(0).forEach((unit) -> {
            player0Units.add(new SimUnit(state.getUnit(unit.intValue())));
        });

        state.getUnitIds(1).forEach((unit) -> {
            player1Units.add(new SimUnit(state.getUnit(unit.intValue())));
        });

        this.xMax = state.getXExtent();
        this.yMax = state.getYExtent();
        
        state.getUnits(0).forEach((unit) -> {
            UnitIdMap.put(unit.getID(), new SimUnit(unit));
        });

        state.getUnits(1).forEach((unit) -> {
            UnitIdMap.put(unit.getID(), new SimUnit(unit));
        });
        List<ResourceNode.ResourceView> resources = state.getAllResourceNodes();
        for(ResourceNode.ResourceView resource: resources) {
            MapLocation eachResource = new MapLocation(resource.getXPosition(), resource.getYPosition());
            resourcesLocation.add(eachResource);
        }
    }
    
      /**
     * Generate a new game state given the previous game state and the action taken
     * @param gameState Current state of the episode
     * @param action Action taken
     */ 
    public GameState(GameState gameState, Action action, boolean changeTurn) {
        this.state = gameState.state;
        this.xMax = gameState.state.getXExtent();
        this.yMax = gameState.state.getYExtent();
        this.resourcesLocation = gameState.resourcesLocation;
        if (changeTurn)player0Turn = !gameState.player0Turn;
        else player0Turn = gameState.player0Turn;
        ActionType type = action.getType();
        int unitID = action.getUnitId();
        // the simulate unit to apply the action
        SimUnit unit = UnitIdMap.get(unitID);    
                                     
        if (type.equals(ActionType.PRIMITIVEATTACK)) {
          TargetedAction attackAction = (TargetedAction)action;
          int targetID = attackAction.getTargetId();
          // the target of the attack
          SimUnit target = UnitIdMap.get(targetID);  
          
          // apply the attack action
          unit.attack(target);
          
          // if the target of the attack is a footman then update 
//          for (SimUnit footman : playerOUnits) {
//            if (footmen.ID == targetID) {
//              play0Units.remove(footman);
//              play0Units.add(target);
//            }
//          }
//          
//          // if the target of the attack is an archer then update 
//          for (SimUnit archer : player1Units) {
//            if (archer.ID == targetID) {
//              play1Units.remove(archer);
//              play1Units.add(target);
//            }
//          }                             
        }
        
        else if(type.equals(ActionType.PRIMITIVEMOVE)) {
          Direction direction = ((DirectedAction)action).getDirection();
          unit.move(direction.xComponent(), direction.yComponent());
          // if the moving unit is a footman then update 
//          for (SimUnit footman : playerOUnits) {
//            if (footmen.ID == unitID) {
//              play0Units.remove(footman);
//              play0Units.add(unit);
//            }
//          }
//          // if the moving unit is an archer then update 
//          for (SimUnit archer : player1Units) {
//            if (archer.ID == unitID) {
//              play1Units.remove(archer);
//              play1Units.add(unit);
//            }
//          }
        }
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
        double healthUtility = 0.0;
        for(SimUnit footman : player0Units) {
           healthUtility += (double)footman.HP/footman.baseHP;
        }
        for(SimUnit archer : player1Units) {
           healthUtility -= (double)archer.HP/archer.baseHP;
        }
        return healthUtility;
    }
    
    private double distanceUtility() {
        double utility = 0.0;

        for(SimUnit footman : player0Units) {
            double distance = 0.0;
            for(SimUnit archer : player1Units) {
                distance = Math.sqrt(Math.pow(footman.x - archer.x, 2) + Math.pow(footman.y - archer.y, 2));
            }
            utility -= distance;
        }
        return utility;
    }

    // return true if all the footmen or archers die
    public boolean isTerminated() {
        double totalArchersHealth = 0.0;
        double totalFootmenHealth = 0.0;
        for(SimUnit archer : player1Units) {
            totalArchersHealth += (double)archer.HP;
        }
        for(SimUnit footman : player0Units) {
            totalFootmenHealth += (double)footman.HP;
        }
        return totalArchersHealth <= 0.0 || totalFootmenHealth <= 0.0;
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
        int firstFootmanID = player0Units.get(0).ID;
        Unit.UnitView firstFootmanView = state.getUnit(firstFootmanID);
        int secondFootmanID = -1;
        Unit.UnitView secondFootmanView = null;
        //get IDs and UnitView of the second Footman
        if (player0Units.size() == 2) {
            secondFootmanID = player0Units.get(1).ID;
            secondFootmanView = state.getUnit(secondFootmanID);
        }

        int firstArcherID = player1Units.get(0).ID;
        Unit.UnitView firstArcherView = state.getUnit(firstArcherID);
        int secondArcherID = -1;
        Unit.UnitView secondArcherView = null;
        if (player1Units.size() == 2) {
            secondArcherID = player1Units.get(1).ID;
            secondArcherView = state.getUnit(secondArcherID);
        }

        //If it's our turn Footmen, find all possible actions and associated state
        if (player0Turn) {
            //actions and state of the first Footman
            for (Direction direction1 : Direction.values()) {
                Action footmanAct1 = footmanAct(direction1, firstFootmanID, firstFootmanView, player1Units);
                GameState childState1 = null;
                if (footmanAct1 != null) {
                    //if there is only one Footman, create a child and add to list and continue consider other directions
                    if (player0Units.size() <= 1) {
                        actions.put(firstFootmanID, footmanAct1);
                        childState1 = new GameState(this, footmanAct1, true);
                        childNodes.add(new GameStateChild(actions, childState1));
                        continue;
                    }else childState1 = new GameState(this, footmanAct1, false);
                }
                
                //actions and state of the second Footman if there are two Footmen
                for (Direction direction2 : Direction.values()) {
                    Map<Integer, Action> actionsBothFootman = new HashMap<Integer, Action>();
                    Action footmanAct2 = footmanAct(direction2, secondFootmanID, secondFootmanView, player1Units);
                    if (footmanAct2 != null) {
                        GameState childStateBothFootman = new GameState(childState1, footmanAct2, true);
                        actionsBothFootman.put(firstFootmanID, footmanAct1);
                        actionsBothFootman.put(secondFootmanID, footmanAct2);
                        childNodes.add(new GameStateChild(actionsBothFootman, childStateBothFootman));
                    }
                }
            }
        }

        //if it's Archer's turn, find all possible actions and associated state
        else {
            for(Direction direction1 : Direction.values()) {
                Action archerAct1 = archerAct(direction1, firstArcherID, firstArcherView, player0Units);
                GameState childState1 = null;
                if (archerAct1 != null) {
                    //if there is only one Archer, create a child and add to list and continue consider other direction
                    if (player0Units.size() <= 1) {
                        actions.put(firstArcherID, archerAct1);
                        childState1 = new GameState(this, archerAct1, true);
                        childNodes.add(new GameStateChild(actions, childState1));
                        continue;
                    }else childState1 = new GameState(this, archerAct1, false);
                }

                //actions and state of the second Footman if there are two Footmen
                for (Direction direction2 : Direction.values()) {
                    Map<Integer, Action> actionsBothArchers = new HashMap<Integer, Action>();
                    Action archerAct2 = footmanAct(direction2, secondArcherID, secondArcherView, player0Units);

                    if (archerAct2 != null) {
                        GameState childStateBothFootman = new GameState(childState1, archerAct2, true);
                        actionsBothArchers.put(firstArcherID, archerAct1);
                        actionsBothArchers.put(secondArcherID, archerAct2);
                        childNodes.add(new GameStateChild(actionsBothArchers, childStateBothFootman));
                    }
                }    
            }
        }
        return childNodes;
    }

    public Action archerAct(Direction direction, int archerID, Unit.UnitView archerView, List<SimUnit> player0Units) {
        int dirX = direction.xComponent();
        int dirY = direction.yComponent();
        int newarcherX = archerView.getXPosition() + dirX;
        int newarcherY = archerView.getYPosition() + dirY;
        int firstFootmanID = player0Units.get(0).ID;
        Unit.UnitView firstFootmanView = state.getUnit(firstFootmanID);
        int secondFootmanID = -1;
        Unit.UnitView secondFootmanView = null;
        //get IDs and UnitView of the second Footman
        if (player0Units.size() == 2) {
            secondFootmanID = player0Units.get(1).ID;
            secondFootmanView = state.getUnit(secondFootmanID);
        }
        if ((dirX == 0 || dirY == 0) && (newarcherX >= 0 && newarcherX <= xMax && newarcherY >=0 && newarcherY <=yMax)
                    && !resourcesLocation.contains(new MapLocation(newarcherX, newarcherY))) {
            //if archer is within the range of the first Footman or both Footman, attack the first Footman
            if(getDistance(firstFootmanView,archerView) <= archerView.getTemplateView().getRange()) {
                return Action.createPrimitiveAttack(archerID, firstFootmanID);
            }

            //if archer is within the range of the second Footman, attack the second Footman
            else if (getDistance(secondFootmanView,archerView) <= archerView.getTemplateView().getRange()) {
                return Action.createPrimitiveAttack(archerID, secondFootmanID);
            }

            //if archer is not within the range, move towards direction
            else {
                return Action.createPrimitiveMove(archerID, direction);
            }
        }
        return null;
    }

    public Action footmanAct(Direction direction, int footmanID, Unit.UnitView footmanView, List<SimUnit> player1Units) {
        int dirX = direction.xComponent();
        int dirY = direction.yComponent();
        int newfootmen1X = footmanView.getXPosition() + dirX;
        int newfootmen1Y = footmanView.getYPosition() + dirY;
        int archerID1 = player1Units.get(0).ID;
        Unit.UnitView archerView1 = state.getUnit(archerID1);
        int archerID2 = -1;
        Unit.UnitView archerView2 = null;
        if (player1Units.size() == 2) {
            archerID2 = player1Units.get(1).ID;
            archerView2 = state.getUnit(archerID2);
        }

        //Only consider Footmen move if it's not diagonal, within the boundaries of the map, and dont collide to any obstacles
        if ((dirX == 0 || dirY == 0) && (newfootmen1X >= 0 && newfootmen1X <= xMax && newfootmen1Y >=0 && newfootmen1Y <=yMax)
            && !resourcesLocation.contains(new MapLocation(newfootmen1X, newfootmen1Y))) {
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
    public double getDistance(Unit.UnitView unit1, Unit.UnitView unit2) {
        double distance = 0;
        double disXsquared = Math.pow(unit2.getXPosition() - unit1.getXPosition(), 2);
        double disYsquared = Math.pow(unit2.getYPosition() - unit1.getYPosition(), 2);
        distance = Math.pow((disXsquared + disYsquared), 1/2);
        return distance;
    }

    //check if 2 units 
    public boolean isAdjacentTo(int footmenX, int footmenY, Unit.UnitView unit2) {
        return Math.abs(footmenX - unit2.getXPosition()) <= 1 && Math.abs(footmenY - unit2.getYPosition()) <= 1;
    }
}
