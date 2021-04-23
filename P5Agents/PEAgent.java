package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may add your own methods and members.
 */
public class PEAgent extends Agent {

    // The plan being executed
    private Stack<StripsAction> plan = null;

    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;

    public PEAgent(int playernum, Stack<StripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<Integer, Integer>();
        this.plan = plan;

    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        // gets the townhall ID and the peasant ID
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if(unitType.equals("peasant")) {
                peasantIdMap.put(unitId, unitId);
            }
        }

        // Gets the peasant template ID. This is used when building a new peasant with the townhall
        for(Template.TemplateView templateView : stateView.getTemplates(playernum)) {
            if(templateView.getName().toLowerCase().equals("peasant")) {
                peasantTemplateId = templateView.getID();
                break;
            }
        }

        return middleStep(stateView, historyView);
    }

    /**
     * This is where you will read the provided plan and execute it. If your plan is correct then when the plan is empty
     * the scenario should end with a victory. If the scenario keeps running after you run out of actions to execute
     * then either your plan is incorrect or your execution of the plan has a bug.
     *
     * For the compound actions you will need to check their progress and wait until they are complete before issuing
     * another action for that unit. If you issue an action before the compound action is complete then the peasant
     * will stop what it was doing and begin executing the new action.
     *
	 * To check a unit's progress on the action they were executing last turn, you can use the following:
     * historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1).get(unitID).getFeedback()
     * This returns an enum ActionFeedback. When the action is done, it will return ActionFeedback.COMPLETED
     *
     * Alternatively, you can see the feedback for each action being executed during the last turn. Here is a short example.
     * if (stateView.getTurnNumber() != 0) {
     *   Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     *   for (ActionResult result : actionResults.values()) {
     *     <stuff>
     *   }
     * }
     * Also remember to check your plan's preconditions before executing!
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        Map<Integer, Action> actionMap = new HashMap<>();

        if(plan.isEmpty()) {
            return actionMap;
        }
        //if the turn is not the first turn
        if (stateView.getTurnNumber() != 0) {
            Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
            for (ActionResult result : actionResults.values()) {
                if (result.getFeedback() == ActionFeedback.FAILED) {
                    actionMap.put(result.getAction().getUnitId(), result.getAction());
                }
                if (result.getFeedback() == ActionFeedback.INCOMPLETE) {
                    return actionMap;
                }
            }
        }
        StripsAction action = plan.pop();
        Action act = createSepiaAction(action);
        if (act == null) return actionMap;
        actionMap.put(act.getUnitId(), act);

        return actionMap;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     *
     * You can create a SEPIA deposit action with the following method
     * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
     *
     * You can create a SEPIA harvest action with the following method
     * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
     *
     * You can create a SEPIA build action with the following method
     * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
     *
     * You can create a SEPIA move action with the following method
     * Action.createCompoundMove(int peasantId, int x, int y)
     * 
     * Hint:
     * peasantId could be found in peasantIdMap
     *
     * these actions are stored in a mapping between the peasant unit ID executing the action and the action you created.
     *
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private Action createSepiaAction(StripsAction action) {
        int peasantId = peasantIdMap.get(action.getUnitId());
        Position peasantPos = action.getPerformingUnit().getPosition();
        if (action.getType().equals("Deposit")) {
            Position destinationPos = action.getTownhall().getPosition();
            return Action.createPrimitiveDeposit(peasantId, peasantPos.getDirection(destinationPos));
        }
        else if (action.getType().equals("HarvestGold")) {
            Position destinationPos = action.getGold().getPosition();
            return Action.createPrimitiveGather(peasantId, peasantPos.getDirection(destinationPos));
        }
        else if (action.getType().equals("HarvestWood")) {
            Position destinationPos = action.getWood().getPosition();
            return Action.createPrimitiveGather(peasantId, peasantPos.getDirection(destinationPos));
        }
        // else if (action.getType() == "Build") {
        //     return Action.createPrimitiveProduction(townhallId, peasantTemplateId);
        // }
        else if (action.getType().equals("MoveToGold")) {
            Position destinationPos = action.getGold().getPosition();
            return Action.createCompoundMove(peasantId, destinationPos.x, destinationPos.y);
        }
        else if (action.getType().equals("MoveToWood")) {
            Position destinationPos = action.getWood().getPosition();
            return Action.createCompoundMove(peasantId, destinationPos.x, destinationPos.y);
        }
        else if (action.getType().equals("MoveToBase")) {
            Position destinationPos = action.getTownhall().getPosition();
            return Action.createCompoundMove(peasantId, destinationPos.x, destinationPos.y);
        }
        return null;
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
