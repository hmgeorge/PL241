//class for an expression.
//inherited by op, call, assign, array, id, number

public class ExprNode extends Node{

    public ExprNode( ){
    
    }
    
    public int kind( ){
	throw new Error("invalid call of kind of ExprNode object");
    }

}