//result to store name of variable, not array

public class IdentNode extends ExprNode {

    private String idname;
    
    public IdentNode( String id ){
	idname = id;
    }

    public String toString(int indent){
	return idname;
    }

    public int kind( ){
	return Node.IDENT;
    }
}

