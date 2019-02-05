package core.modAPI;

import java.util.ArrayList;

import core.Board;
import core.Creature;

public interface CreatureAttribute<T> {
    
    public void init(Board b, Creature c);
    
    public String getName();
    public T getValue();
    public void setValue(T v);
    
    public void update(double lastUpdateTime, double updateTime, Creature c, Board b);

	public void initFromParents(ArrayList<CreatureAttribute> parentAttributes, Board board);
}