main
var x;
var i;
var j;
{
    let x <- call InputNum( );

    let i <- 1;
    let j <- 0;

    while i <= x do
	let j <- 1;
	while j <= i do
	    call OutputNum( j );
	    let j <- j + 1;
        od;
        call OutputNewLine( );
        let i <- i + 1;
    od;
}.

