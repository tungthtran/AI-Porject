package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.Unit;

public class SimUnit {

    private int HP;
    private int ID;
    private Position position;
    //the amount of gold or wood being carried by this unit.
    private int cargoAmount;
    //the type of resource being carried by the unit
    private ResourceType cargoType;
    private String name;


    public SimUnit(Unit.UnitView unit) {
        this.HP = unit.getHP();
        this.ID = unit.getID();
        this.position = new Position(unit.getXPosition(), unit.getYPosition());
        this.cargoAmount = unit.getCargoAmount();
        this.cargoType = unit.getCargoType();
        this.name = unit.getTemplateView().getName();
    }

    public SimUnit(int ID, Position position, String name) {
        if (name.equals("peasant")) {
            this.HP = 30;
            this.ID = ID;
            this.position = new Position(position);
            this.name = "peasant";
            this.cargoAmount = 0;
            this.cargoType = null;
        }
        else {
            throw IllegalStateException("Not valid unit type");
        }
    }

    /***
     * A copy of the simulated unit
     * @param unit
     */

    public SimUnit(SimUnit unit) {
        this.HP = unit.getHP();
        this.ID = unit.getID();
        this.position = unit.getPosition();
        this.cargoAmount = unit.getCargoAmount();
        this.cargoType = unit.getCargoType();
        this.name = unit.name;
    }


    public int getHP() {
        return this.HP;
    }

    public int getID() {
        return this.ID;
    }

    public Position getPosition() {
        return this.position;
    }

    public int getCargoAmount() {
        return this.cargoAmount;
    }

    public ResourceType getCargoType() {
        return this.cargoType;
    }

    public String getName() {
        return this.name;
    }

    // move to a new position
    public void setPosition(Position newPosition) {
        this.position = newPosition;
    }

    public void setCargoAmount(int amount) {
        this.cargoAmount = amount;
    }

    public void setCargoType(ResourceType type) {
        this.cargoType = type;
    }

    @Override
    public int hashCode() {
        return ID + position.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof SimUnit) {
            return ((SimUnit) o).getID() == this.ID
                    && ((SimUnit) o).getPosition().equals(this.position);
        }
        else
            return false;
    }
}