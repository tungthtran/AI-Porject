package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.*;
import java.util.*;
import java.util.Random;

public class RLAgent extends Agent {

    /**
     * Set in the constructor. Defines how many learning episodes your agent should run for.
     * When starting an episode. If the count is greater than this value print a message
     * and call sys.exit(0)
     */
    public final int numEpisodes;

    /**
     * List of your footmen and your enemies footmen
     */
    private List<Integer> myFootmen;
    private List<Integer> enemyFootmen;

    /**
     * Convenience variable specifying enemy agent number. Use this whenever referring
     * to the enemy agent. We will make sure it is set to the proper number when testing your code.
     */
    public static final int ENEMY_PLAYERNUM = 1;

    /**
     * Set this to whatever size your feature vector is.
     */
    public static final int NUM_FEATURES = 5;

    /** Use this random number generator for your epsilon exploration. When you submit we will
     * change this seed so make sure that your agent works for more than the default seed.
     */
    public final Random random = new Random(12345);

    public static final double CONSTANT = 2.2;

    /**
     * Your Q-function weights.
     */
    public Double[] weights;

    /**
     * These variables are set for you according to the assignment definition. You can change them,
     * but it is not recommended. If you do change them please let us know and explain your reasoning for
     * changing them.
     */
    public final double gamma = 0.9;
    public final double learningRate = .0001;
    public final double epsilon = .02;

    //map each footmanID to the length of its action
    Map<Integer, Integer> actionLengths = new HashMap<>();

    public RLAgent(int playernum, String[] args) {
        super(playernum);

        if (args.length >= 1) {
            numEpisodes = Integer.parseInt(args[0]);
            System.out.println("Running " + numEpisodes + " episodes.");
        } else {
            numEpisodes = 10;
            System.out.println("Warning! Number of episodes not specified. Defaulting to 10 episodes.");
        }

        boolean loadWeights = false;
        if (args.length >= 2) {
            loadWeights = Boolean.parseBoolean(args[1]);
        } else {
            System.out.println("Warning! Load weights argument not specified. Defaulting to not loading.");
        }

        if (loadWeights) {
            weights = loadWeights();
        } else {
            // initialize weights to random values between -1 and 1
            weights = new Double[NUM_FEATURES];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = random.nextDouble() * 2 - 1;
            }
        }
    }

    /**
     * We've implemented some setup code for your convenience. Change what you need to.
     */
    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {

        // You will need to add code to check if you are in a testing or learning episode

        // Find all of your units
        myFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                myFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }

        // Find all of the enemy units
        enemyFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(ENEMY_PLAYERNUM)) {
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                enemyFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }

        return middleStep(stateView, historyView);
    }

    /**
     * You will need to calculate the reward at each step and update your totals. You will also need to
     * check if an event has occurred. If it has then you will need to update your weights and select a new action.
     *
     * Some useful API calls here are:
	 *
     * If you are using the footmen vectors you will also need to remove killed enemies and your units which being killed. To do so use the historyView
     * to get a DeathLog. Each DeathLog tells you which player's unit died and the unit ID of the dead unit. To get
     * the deaths from the last turn do something similar to the following snippet. Please be aware that on the first
     * turn you should not call this as you will get nothing back.
     *
     ** 
     *for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() -1)) {
     *     System.out.println("Player: " + deathLog.getController() + " unit: " + deathLog.getDeadUnitID());
     * }
     **
     * You should also check for completed actions using the history view. Obviously you never want a footman just
     * sitting around doing nothing (the enemy certainly isn't going to stop attacking). So at the minimum you will
     * have an event whenever one your footmen's targets is killed or an action fails. Actions may fail if the target
     * is surrounded or the unit cannot find a path to the unit. To get the action results from the previous turn
     * you can do something similar to the following. Please be aware that on the first turn you should not call this
     **
     * Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     * for(ActionResult result : actionResults.values()) {
     *     System.out.println(result.toString());
     * }
     **
     *
     * Remember that you can use result.getFeedback() on an ActionResult, and compare the result to an ActionFeedback enum.
     * Useful ActionFeedback values include COMPLETED, FAILED, and INCOMPLETE.
     * 
     * You can also get the ID of the unit executing an action from an ActionResult. For example,
     * result.getAction().getUnitID()
     * 
     * For this assignment it will be most useful to create compound attack actions. These will move your unit
     * within range of the enemy and then attack them once. You can create one using the static method in Action:
     * Action.createCompoundAttack(attackerID, targetID)
     * 
     * You will then need to add the actions you create to a Map that will be returned. This creates a mapping
     * between the ID of the unit performing the action and the Action object.
     * 
     * @return New actions to execute or nothing if an event has not occurred.
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        Map<Integer, Action> result = new HashMap<>();
        
        int lastTurnNumber = stateView.getTurnNumber() - 1;
        if (lastTurnNumber >= 0) {
            checkLastTurn(stateView, historyView, lastTurnNumber);
        }

        for (Integer footmanId : myFootmen) {
            if (lastTurnNumber >= 0) {
                checkLastTurn(stateView, historyView, lastTurnNumber);
                Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, lastTurnNumber);
                int actionlength = 0;
                for(ActionResult result : actionResults.values()) {
                    if (actionResults.get(footmanId).getFeedback().equals(ActionFeedback.COMPLETED)){
                        int enemyId = selectAction(stateView, historyView, footmanId);

                        Action action = Action.createCompoundAttack(attackerID, enemyID);
                        result.put(footmanId, action);
                    }
                    else if (actionResults.get(footmanId).getFeedback().equals(ActionFeedback.INCOMPLETE)) {
                        actionlength++;
                    }
                }
                actionLengths.put(footmanId, actionlength);
            }
            else {
                int enemyId = selectAction(stateView, historyView, footmanId);

                Action action = Action.createCompoundAttack(attackerID, enemyID);
                result.put(footmanId, action);
            }
            
        }
        return result;
    }

    public void checkLastTurn(State.StateView stateView, History.HistoryView historyView,int lastTurnNumber) {
        for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() -1)) {
            //System.out.println("Player: " + deathLog.getController() + " unit: " + deathLog.getDeadUnitID());
            if (deathLog.getController() == ENEMY_PLAYERNUM) {
                enemyFootmen.remove(deathLog.getDeadUnitID());
            }
            else if (deathLog.getController() == playernum) {
                myFootmen.remove(deathLog.getDeadUnitID());
            }
        }
    }


    /**
     *
     * Here you will calculate the cumulative average rewards for your testing episodes. If you have just
     * finished a set of test episodes you will call out testEpisode.
     *
     * It is also a good idea to save your weights with the saveWeights function.
     */
    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

        // MAKE SURE YOU CALL printTestData after you finish a test episode.

        // Save your weights
        saveWeights(weights);

    }

    /**
     * Calculate the updated weights for this agent. 
     * @param oldWeights Weights prior to update
     * @param oldFeatures Features from (s,a)
     * @param totalReward Cumulative discounted reward for this footman.
     * @param stateView Current state of the game.
     * @param historyView History of the game up until this point
     * @param footmanId The footman we are updating the weights for
     * @return The updated weight vector.
     */
    public double[] updateWeights(double[] oldWeights, double[] oldFeatures, double totalReward, State.StateView stateView, History.HistoryView historyView, int footmanId) {
        double estNextQ = calcQValue(stateView, historyView, footmanId, enemyFootmen.get(0).intValue());
        for(Integer enemy : enemyFootmen){
            estNextQ = max(estNextQ, calcQValue, footmanId, enemy.intValue());
        }
        double oldQ = 0;
        for(int i = 0; i<NUM_FEATURES; i++){
            oldQ += oldWeights[i]*oldFeatures[i];
        }
        double[] newWeights = new double[NUM_FEATURES];
        for(int i = 0; i< NUM_FEATURES; i++){
            newWeights[i] = oldWeights[i] + learningRate*(totalReward + gamma*estNextQ-oldQ)*oldFeatures[i];
        }
        return newWeights;
    }

    /**
     * Given a footman and the current state and history of the game select the enemy that this unit should
     * attack. This is where you would do the epsilon-greedy action selection.
     *
     * @param stateView Current state of the game
     * @param historyView The entire history of this episode
     * @param attackerId The footman that will be attacking
     * @return The enemy footman ID this unit should attack
     */
    public int selectAction(State.StateView stateView, History.HistoryView historyView, int attackerId) {
        if (random.nextDouble() < epsilon){
            return enemyFootmen.get(random.nextInt(enemyFootmen.size())).intValue();
        }else{
            int target = enemyFootmen.get(0);
            int maxQ = calcQValue(stateView, historyView, attackerID, target);
            for(Integer enemy : enemyFootmen){
                if (calcQValue(stateView, historyView, attackerID, enemy.intValue()) > maxQ){
                    target = enemy.intValue();
                    maxQ = calcQValue(stateView, historyView, attackerID, target);
                }
            }
            return target;
        }
    }

    /**
     * Given the current state and the footman in question calculate the reward received on the last turn.
     * This is where you will check for things like Did this footman take or give damage? Did this footman die
     * or kill its enemy. Did this footman start an action on the last turn? See the assignment description
     * for the full list of rewards.
     *
     * Remember that you will need to discount this reward based on the timestep it is received on. See
     * the assignment description for more details.
     *
     * As part of the reward you will need to calculate if any of the units have taken damage. You can use
     * the history view to get a list of damages dealt in the previous turn. Use something like the following.
     *
     * for(DamageLog damageLogs : historyView.getDamageLogs(lastTurnNumber)) {
     *     System.out.println("Defending player: " + damageLog.getDefenderController() + " defending unit: " + \
     *     damageLog.getDefenderID() + " attacking player: " + damageLog.getAttackerController() + \
     *     "attacking unit: " + damageLog.getAttackerID() + "damage: " + damageLog.getDamage());
     * }
     *
     * You will do something similar for the deaths. See the middle step documentation for a snippet
     * showing how to use the deathLogs.
     *
     * To see if a command was issued you can check the commands issued log.
     *
     * Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, lastTurnNumber);
     * for (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
     *     System.out.println("Unit " + commandEntry.getKey() + " was command to " + commandEntry.getValue().toString);
     * }
     *
     * @param stateView The current state of the game.
     * @param historyView History of the episode up until this turn.
     * @param footmanId The footman ID you are looking for the reward from.
     * @return The current reward
     */
    public double calculateReward(State.StateView stateView, History.HistoryView historyView, int footmanId, Map<Integer, Integer> actionLengths) {
        double reward = 0;
        double discountReward = 1;
        //calculate cost of the actions
        for(int i = 0; i<actionLengths.get(footmanId); i++){
            reward += -0.1*discountReward;
            discountReward *= gamma;
        }
		int lastTurnNumber = stateView.getTurnNumber() - 1;

		for(DamageLog damageLog : historyView.getDamageLogs(lastTurnNumber)) {
			if(damageLog.getAttackerController() == playernum && damageLog.getAttackerID() == footmanId){
				reward = reward + damageLog.getDamage()*discountReward;
			} else if(damageLog.getAttackerController() == ENEMY_PLAYERNUM && damageLog.getDefenderID() == footmanId){
				reward = reward - damageLog.getDamage()*discountReward;
			}
		}
        for(DeathLog deathLog : historyView.getDeathLogs(previousTurnNumber)){
            if(deathLog.getController() == ENEMY_PLAYERNUM){
                reward = reward + 100*discountReward;
            } else if(deathLog.getDeadUnitID() == footmanId) {
                reward = reward - 100*discountReward;
            }
        }
        return reward;
    }

    /**
     * Calculate the Q-Value for a given state action pair. The state in this scenario is the current
     * state view and the history of this episode. The action is the attacker and the enemy pair for the
     * SEPIA attack action.
     *
     * This returns the Q-value according to your feature approximation. This is where you will calculate
     * your features and multiply them by your current weights to get the approximate Q-value.
     *
     * @param stateView Current SEPIA state
     * @param historyView Episode history up to this point in the game
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman that your footman would be attacking
     * @return The approximate Q-value
     */
    public double calcQValue(State.StateView stateView,
                             History.HistoryView historyView,
                             int attackerId,
                             int defenderId) {
        double result = 0;
        double[] features = calculateFeatureVector(stateView, historyView, attackerID, defenderId);
        for (int i = 0; i < NUM_FEATURES; i++){
            result += weights[i]*features[i];
        }
        return result;
    }

    /**
     * Given a state and action calculate your features here. Please include a comment explaining what features
     * you chose and why you chose them.
     *
     * for example: HP 
     * UnitView attacker = stateView.getUnit(attackerId);
     * attacker.getHP()
     * 
     * All of your feature functions should evaluate to a double. Collect all of these into an array. You will
     * take a dot product of this array with the weights array to get a Q-value for a given state action.
     *
     * It is a good idea to make the first value in your array a constant. This just helps remove any offset
     * from 0 in the Q-function. The other features are up to you. Many are suggested in the assignment
     * description.
     *
     * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman. The one you are considering attacking.
     * @return The array of feature function outputs.
     */
    public double[] calculateFeatureVector(State.StateView stateView,
                                           History.HistoryView historyView,
                                           int attackerId,
                                           int defenderId) {
        // features include:
        // HP of the enemy because we want to fight the enemy with low HP
        // whether the enemy is attacking us
        // how many others are attacking that enemy
        // the distance between us and the enemy
        UnitView attackingUnit = stateView.getUnit(attackerId);
        UnitView defendingUnit = stateView.getUnit(defenderId);
        if (footman == null || enemy == null) {
            return null;
        }
        else {
            double[] features = new double[NUM_FEATURES];
            features[0] = CONSTANT;
            HP of the enemy
            features[1] = defendingUnit.getHP();
            Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
            // if the enemy is attacking me --> save 1
            // otherwise 0
            if (actionResults.containsKey(defenderId) && (TargetedAction)(actionResults.get(defenderId).getAction()).getTargetId() == attackerId) {
                features[2] = 1.0;
            }
            else {
                features[2] = 0.0;
            }
            // how many others are attacking this enemy
            for (int i = 1; i < stateView.getTurnNumber(); i++) {
                Map<Integer, ActionResult> actions = historyView.getCommandFeedback(playernum, i);
                for (int attacker : actions.keySet()) {
                    if ((TargetedAction)(actions.get(attacker).getAction()).getTargetId() == defenderId) {
                        features[3]++;
                    }
                }
            }
            features[4] = chebyshevDistance(attackingUnit.getXPosition(), attackingUnit.getYPosition(), 
                                            defendingUnit.getXPosition(), defendingUnit.getYPosition());
        }
    }

    public int chebyshevDistance(int x1, int y1, int x2, int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * Prints the learning rate data described in the assignment. Do not modify this method.
     *
     * @param averageRewards List of cumulative average rewards from test episodes.
     */
    public void printTestData (List<Double> averageRewards) {
        System.out.println("");
        System.out.println("Games Played      Average Cumulative Reward");
        System.out.println("-------------     -------------------------");
        for (int i = 0; i < averageRewards.size(); i++) {
            String gamesPlayed = Integer.toString(10*i);
            String averageReward = String.format("%.2f", averageRewards.get(i));

            int numSpaces = "-------------     ".length() - gamesPlayed.length();
            StringBuffer spaceBuffer = new StringBuffer(numSpaces);
            for (int j = 0; j < numSpaces; j++) {
                spaceBuffer.append(" ");
            }
            System.out.println(gamesPlayed + spaceBuffer.toString() + averageReward);
        }
        System.out.println("");
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will take your set of weights and save them to a file. Overwriting whatever file is
     * currently there. You will use this when training your agents. You will include th output of this function
     * from your trained agent with your submission.
     *
     * Look in the agent_weights folder for the output.
     *
     * @param weights Array of weights
     */
    public void saveWeights(Double[] weights) {
        File path = new File("agent_weights/weights.txt");
        // create the directories if they do not already exist
        path.getAbsoluteFile().getParentFile().mkdirs();

        try {
            // open a new file writer. Set append to false
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

            for (double weight : weights) {
                writer.write(String.format("%f\n", weight));
            }
            writer.flush();
            writer.close();
        } catch(IOException ex) {
            System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
        }
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will load the weights stored at agent_weights/weights.txt. The contents of this file
     * can be created using the saveWeights function. You will use this function if the load weights argument
     * of the agent is set to 1.
     *
     * @return The array of weights
     */
    public Double[] loadWeights() {
        File path = new File("agent_weights/weights.txt");
        if (!path.exists()) {
            System.err.println("Failed to load weights. File does not exist");
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            List<Double> weights = new LinkedList<>();
            while((line = reader.readLine()) != null) {
                weights.add(Double.parseDouble(line));
            }
            reader.close();

            return weights.toArray(new Double[weights.size()]);
        } catch(IOException ex) {
            System.err.println("Failed to load weights from file. Reason: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
}
