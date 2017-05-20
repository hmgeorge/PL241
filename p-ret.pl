main
var x;
array[5] a;
var y;
{
    let x <- 4;
    while x > 0 do

	if x < 4 then
	    let a[2] <- x;
        else
	    let x <- x + 1;
            let a[3] <- x;
        fi;
 
    od;

    return
}.
