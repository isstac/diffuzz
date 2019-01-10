## setup.sh
#####################################
# chmod +x setup.sh
# ./setup.sh
#

trap "exit" INT

# Build AFL.
echo "Build AFL.."
cd ./afl-2.51b-wca/
make clean
make -s
make install -s
echo ""

# Build interface program.
echo "Build interface program.."
cd ../fuzzerside
make clean
make -s
echo ""

# Build Kelinci server and instrumentor.
echo "Build Kelinci and instrumentor"
cd ../instrumentor
gradle clean
gradle build
echo ""

echo "Done."
