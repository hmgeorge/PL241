//class to hold result from an if statement
public class ReturnStatNode extends StatNode {
    public ExprNode retNode;
 
    public ReturnStatNode(ExprNode ret ){
	retNode = ret;
    }

    public String toString( int indent ){

	String indentstring = IdentTabs( indent );
	String rstring = indentstring + "RETURN ";
	
	if( retNode != null ){
	    rstring += retNode.toString( 0 );
	}
	return rstring;
    }

    public int kind( ){
	return Node.RETURN;
    }
}
