bash pack.sh $1
scp compiler-$1.tar.gz hmgeorge@family-guy.ics.uci.edu:PL241/
mv compiler-$1.tar.gz stages/
