main
var x;
array [5] a;
{
    let x <- 0;
    while x < 5 do
	let a[ x ] <- call InputNum( );
        let x <- x + 1;
    od;

    let x <- 0;
    
    while x < 5 do
	call OutputNum( a[ x ] );
        let x <- x + 1;
    od;
}.
