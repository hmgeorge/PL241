import java.util.ArrayList;

//Class to store sequence of statement results. 
public class StatSeqNode extends Node{

    public ArrayList<StatNode> sresults;

    public StatSeqNode( ){
	sresults = new ArrayList<StatNode>( );
    }

    public void AddStatNode( StatNode r ){
	if( r == null ){
	    //this is an error
	    return;
	}
	sresults.add( r );
    }

    public String toString( int indent ){
	String sseqstring = "";

	String idenstring = IdentTabs( indent );

	for(int i = 0; i < sresults.size( ); i++ ){
	    StatNode s = sresults.get(i);
	    sseqstring += s.toString( indent + 1 ) + ";\n";
	}
	return sseqstring;
    }

    public int kind( ){
	return Node.STATSEQ;
    }
}