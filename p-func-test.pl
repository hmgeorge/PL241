main
var x;
var y;
var sum;
function dec( a , b, c, d, e, f );
var x;
var y;
{
    let x <- a - 1 ;
    return x;
};
function fact( a );
var x;
{ 
    let x <- 0;

    if a == 1 then
	return 1;
    else
	return a * call fact ( call dec ( a, 1, 2, 3, 4, 5 ) );
    fi;
};
function total( a, b, c, d );
{
    call OutputNum( c - d );
    return a + b;
};
{
    let y <- call InputNum ( );
  
    if  y > 3  then
        let x <- call fact( y );
    else
	let x <- 1;
    fi;

    call OutputNum ( call total ( x, y, 4, 3 ) );
}.
