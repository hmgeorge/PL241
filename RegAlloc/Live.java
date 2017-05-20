//nothing but a typedef over HashSet

import java.util.HashSet;
import java.util.Iterator;

public class Live{
    HashSet<SSAValue> set;
    
    public Live( ){
	set = new HashSet<SSAValue>( );
    }

    boolean add( SSAValue v ){
	return set.add( v );
    }

    boolean remove( SSAValue v ){
	return set.remove( v );
    }

    boolean addAll( Live l ){
	return set.addAll( l.set );
    }

    boolean removeAll( Live l ){
	return set.removeAll( l.set );
    }

    Iterator<SSAValue> iterator( ){
	return set.iterator( ); //remember, using an iterator. so do 
	                        //all adding removing using that iterator
	                        //itself. 
    }
}
    