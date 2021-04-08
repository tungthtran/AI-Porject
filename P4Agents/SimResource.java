package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;

public class SimResource {
    private int ID;
    private Position position;
    private ResourceNode.Type type;
    private int amountLeft;

    private static int COLLECTED_AMOUNT = 100;

    public SimResource(ResourceView unit, ResourceNode.Type type) {
        this.amountLeft = unit.getAmountRemaining();
        this.ID = unit.getID();
        this.position = new Position(unit.getXPosition(), unit.getYPosition());
        this.type = type;
    }

    /***
     * a copy of the resource
     * @param unit
     */
    public SimResource(SimResource unit) {
        this.amountLeft = unit.getAmountLeft();
        this.ID = unit.getID();
        this.position = unit.getPosition();
        this.type = unit.getType();
    }

    public int getID() {
        return this.ID;
    }

    public Position getPosition() {
        return this.position;
    }

    public ResourceNode.Type getType() {
        return this.type;
    }

    public int getAmountLeft() {
        return this.amountLeft;
    }

    public void getCollectedFrom() {
        this.amountLeft -= COLLECTED_AMOUNT;
    }

    @Override
    public int hashCode() {
        return ID + position.hashCode() + type.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SimResource) {
            return ((SimResource) o).getID() == this.ID
                    && ((SimResource) o).getPosition().equals(position)
                    && ((SimResource) o).getType() == this.type
                    && ((SimResource) o).getAmountLeft() == this.amountLeft;
        }
        else
            return false;
    }
}