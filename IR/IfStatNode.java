//class to hold result from an if statement
public class IfStatNode extends StatNode {
    public ExprNode    condNode;
    public StatSeqNode thenNode;
    public StatSeqNode elseNode;

    public IfStatNode(ExprNode cond, StatSeqNode thn, StatSeqNode els ){
	condNode = cond;
	thenNode = thn;
	elseNode = els;
    }

    public String toString( int indent ){
	String indentstring = IdentTabs( indent );
	String ifstring = indentstring + "IF ";

	ifstring += condNode.toString(0) + " THEN\n ";
	ifstring += thenNode.toString(indent+1);
	
	if( elseNode != null ){
	    ifstring += indentstring + "ELSE\n " + 
		        elseNode.toString(indent+1);
	}

	ifstring += indentstring + "FI";
	return ifstring;
    }

    public int kind( ){
	return Node.IF;
    }
}
	