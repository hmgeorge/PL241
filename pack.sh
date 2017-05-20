#rm compiler-present.tar.gz

if [ $# != 1 ]; then
    echo "retry with date"
else

tar -cvf compiler-$1.tar *.java IR SSA Analysis RegAlloc CodeGen Makefile *.pl run.sh TODO upload.sh pack.sh 
gzip compiler-$1.tar
fi
