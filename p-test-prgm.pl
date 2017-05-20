main
var colcount;
var rowcount;
array[82] data;
var rule;
array[2][2][2] rulebin;

function setnextbit(last, akt, next, bits);
{
  let rulebin[last][akt][next] <- bits - (bits / 2) * 2;
  return bits / 2
};

procedure initrulebin();
var bits;
{
  let bits <- rule;
  let bits <- call setnextbit(0, 0, 0, bits);
  let bits <- call setnextbit(0, 0, 1, bits);
  let bits <- call setnextbit(0, 1, 0, bits);
  let bits <- call setnextbit(0, 1, 1, bits);
  let bits <- call setnextbit(1, 0, 0, bits);
  let bits <- call setnextbit(1, 0, 1, bits);
  let bits <- call setnextbit(1, 1, 0, bits);
  let bits <- call setnextbit(1, 1, 1, bits)
};

procedure cleardata();
var i;
{
  let i <- 0;
  while i < colcount + 2 do
    let data[i] <- 0;
    let i <- i + 1
  od
};

procedure output();
var i;
{
  let i <- 1;
  while i <= colcount do
    if data[i] == 0 then
      call OutputNum(1)
    else
     if data[i] == 1 then
       call OutputNum(8)
     else
       call OutputNum(0)
       fi
    fi;
    let i <- i + 1
  od;
  call OutputNewLine
};

procedure calcnext();
var i;
var last, akt, next;
{
  let data[0] <- data[1];
  let data[colcount + 1] <- data[colcount];

  let last <- data[0];
  let akt <- data[1];

  let i <- 1;
  while i <= colcount do
    let next <- data[i + 1];
    let data[i] <- rulebin[last][akt][next];

    let last <- akt;
    let akt <- next;
    let i <- i + 1
  od
};

procedure run();
var i;
{
  let i <- 0;
  while i < rowcount do
    call output;
    call calcnext;
    let i <- i + 1
  od
};

{
  call OutputNum(22);
  let colcount <- 80;
  let rowcount <- 60;

  call cleardata;
  let data[40] <- 1;
  let rule <- 45;

  call initrulebin;

  call run
}.