main
var x;
var z;
var y;
array [5] a;
{
    let x <- 4;
    let y <- 3;

    if x  > y then
       let z <- z - x;
       let a[2] <- z;
       return;
    else
       let z <- z - y;
    fi;
    
    let z <- z + 1;
}.
