main
var x;
array[5] a;
var y;
var z;
{
    let x <- 4;
    let y <- 3;

    let z <- a[ x + y - 3 ];
    let x <- a[ x + y - 3 ];

    while x > 0 do
	
	while y > 0 do

	    if x < 2 then
	       let a[ x ] <- x + z + y;
            else
                let a[ x ] <-  x + z - y;
            fi;
             
            let y <- y - 1;
        od;
        let x <- x - 1;
    od;
     
    let y <- x + 4;
}.

