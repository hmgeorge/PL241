//class for operations
//operations are
// add, mul, div, sub and logical operators.

public class OpNode extends ExprNode {

    public ExprNode lvalresult;
    public ExprNode rvalresult;
    public OpKind operation;

    public OpNode(OpKind o, ExprNode l, ExprNode r ){
	operation = o;
	lvalresult = l;
	rvalresult = r;
    }

    public String toString( int indent ){

	String indentstring = IdentTabs( indent );

	String ostring = indentstring + operation.toString(0) + " "; 
	ostring += lvalresult.toString(0) ;
	if( rvalresult != null ){
	    ostring += " " + rvalresult.toString(0);
	}
	
	return ostring ;
    }

    public int kind( ){
	return Node.OP;
    }
}

