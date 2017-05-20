main
var x;
array[5] a;
var y;
{
    let x <- 4;
    if x <= 4 then
	if x < 4 then
	    let a[2] <- x + y;
	    let x <- 999;	    
        else
	    let a[3] <- x + y;
            let x <- a[3];
	    
        fi;
        let x <- a[1] + x;
    else 
	let a[2] <- x + y;
    let y <- x + 3;
    fi;
    let y <- 7;
    let a[2] <- x;
}.
