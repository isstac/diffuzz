## setup.sh
#####################################
# chmod +x setup.sh
# ./setup.sh
#

trap "exit" INT

# Prepare apache_ftpserver_clear.
echo "Prepare apache_ftpserver_clear.."
cd ./apache_ftpserver_clear/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_Clear.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_md5.
echo "Prepare apache_ftpserver_md5.."
cd ./apache_ftpserver_md5/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_MD5.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_salted.
echo "Prepare apache_ftpserver_salted.."
cd ./apache_ftpserver_salted/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_Salted.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_salted_encrypt.
echo "Prepare apache_ftpserver_salted_encrypt.."
cd ./apache_ftpserver_salted_encrypt/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_Salted_Encrypt.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_stringutils.
echo "Prepare apache_ftpserver_stringutils.."
cd ./apache_ftpserver_stringutils/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_StringUtilsPad.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

echo "Done."
