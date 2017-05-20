//class to hold result from an if statement
public class WhileStatNode extends StatNode {
    public ExprNode    condNode;
    public StatSeqNode bodyNode;
    

    public WhileStatNode(ExprNode cond, StatSeqNode body ){
	condNode = cond;
	bodyNode = body;	
    }

    public String toString( int indent ){
	String indentstring = IdentTabs( indent );
	String wstring = indentstring + "WHILE ";

	wstring += condNode.toString(0) + " DO\n";
	wstring += bodyNode.toString(indent+1);
	wstring += indentstring + "OD";

	return wstring;
    }

    public int kind( ){
	return Node.WHILE;
    }
}
