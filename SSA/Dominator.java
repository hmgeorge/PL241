import java.util.HashSet;
import java.util.Iterator;

public class Dominator{

    public static BasicBlock ClosestCommonDominator( HashSet<BasicBlock> alive_branches ){
	
	if( alive_branches.isEmpty() ){
	    return null;
	}

	HashSet<BasicBlock> working_set = alive_branches;

	while( working_set.size( ) != 1 ){
	    Iterator<BasicBlock> it = working_set.iterator( );
	    HashSet<BasicBlock> temp_set = new HashSet<BasicBlock>( );

	    while( it.hasNext( ) ){
		BasicBlock b = it.next( );
		if( b != null && b.returnSeen( ) == false ){ //pretty sure b.returnSeen
                                                             //is not needed
		    temp_set.add( b.GetImmDom( ) );
		}
	    }

	    working_set = temp_set;
	}
	
	//guaranteed size = 1 if code reaches here
	Iterator<BasicBlock> it = working_set.iterator( );
	return it.next( );
    }

    
}