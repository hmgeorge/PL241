main
var x;
var sum;
function inc( a, b, c, d );
var x;
var y;
{
    return a + 1;
};

function dec( a,b );
var x;
var y;
{
    let x <- a - 1;
    return x;
};
{
    let sum <- 0;
    let x <- call InputNum( );
    
    while x > 0 do
	let sum <- sum + x;
        let x <- x - 1;
    od;

    call OutputNum( sum );
    call OutputNewLine( );
}.
