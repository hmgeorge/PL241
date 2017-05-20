//class to hold result of a statement
public class StatNode extends Node{

    public StatNode( ){

    }

    public int kind( ){
	throw new Error("invalid call of kind of Node object");
    }
    
    public String toString( int indent ){
	throw new Error("invalid call of tostring on StatNode object");
    }
    
}