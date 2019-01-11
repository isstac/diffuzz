## setup.sh
#####################################
# chmod +x setup.sh
# ./setup.sh
#

trap "exit" INT

# Prepare apache_ftpserver_clear_safe.
echo "Prepare apache_ftpserver_clear_safe.."
cd ./apache_ftpserver_clear_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_Clear.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_clear_unsafe.
echo "Prepare apache_ftpserver_clear_unsafe.."
cd ./apache_ftpserver_clear_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_Clear.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_md5_safe.
echo "Prepare apache_ftpserver_md5_safe.."
cd ./apache_ftpserver_md5_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_MD5.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_md5_unsafe.
echo "Prepare apache_ftpserver_md5_unsafe.."
cd ./apache_ftpserver_md5_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_MD5.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_salted_safe.
echo "Prepare apache_ftpserver_salted_safe.."
cd ./apache_ftpserver_salted_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_Salted.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_salted_unsafe.
echo "Prepare apache_ftpserver_salted_unsafe.."
cd ./apache_ftpserver_salted_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_Salted.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_salted_encrypt_unsafe.
echo "Prepare apache_ftpserver_salted_encrypt_unsafe.."
cd ./apache_ftpserver_salted_encrypt_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_Salted_Encrypt.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_stringutils_safe.
echo "Prepare apache_ftpserver_stringutils_safe.."
cd ./apache_ftpserver_stringutils_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_StringUtilsPad.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare apache_ftpserver_stringutils_unsafe.
echo "Prepare apache_ftpserver_stringutils_unsafe.."
cd ./apache_ftpserver_stringutils_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver_StringUtilsPad.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_array_safe.
echo "Prepare blazer_array_safe.."
cd ./blazer_array_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar MoreSanity_Array_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_array_unsafe.
echo "Prepare blazer_array_unsafe.."
cd ./blazer_array_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar MoreSanity_Array_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_gpt14_safe.
echo "Prepare blazer_gpt14_safe.."
cd ./blazer_gpt14_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar GPT14_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_gpt14_unsafe.
echo "Prepare blazer_gpt14_unsafe.."
cd ./blazer_gpt14_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar GPT14_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_k96_safe.
echo "Prepare blazer_k96_safe.."
cd ./blazer_k96_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar K96_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_k96_unsafe.
echo "Prepare blazer_k96_unsafe.."
cd ./blazer_k96_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar K96_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_loopandbranch_safe.
echo "Prepare blazer_loopandbranch_safe.."
cd ./blazer_loopandbranch_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar MoreSanity_LoopAndBranch_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_loopandbranch_unsafe.
echo "Prepare blazer_loopandbranch_unsafe.."
cd ./blazer_loopandbranch_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar MoreSanity_LoopAndBranch_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_modpow1_safe.
echo "Prepare blazer_modpow1_safe.."
cd ./blazer_modpow1_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar ModPow1_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_modpow1_unsafe.
echo "Prepare blazer_modpow1_unsafe.."
cd ./blazer_modpow1_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar ModPow1_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_modpow2_safe.
echo "Prepare blazer_modpow2_safe.."
cd ./blazer_modpow2_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar ModPow2_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_modpow2_unsafe.
echo "Prepare blazer_modpow2_unsafe.."
cd ./blazer_modpow2_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar ModPow2_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_passwordEq_safe.
echo "Prepare blazer_passwordEq_safe.."
cd ./blazer_passwordEq_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar User_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_passwordEq_unsafe.
echo "Prepare blazer_passwordEq_unsafe.."
cd ./blazer_passwordEq_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar User_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_sanity_safe.
echo "Prepare blazer_sanity_safe.."
cd ./blazer_sanity_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Sanity_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_sanity_unsafe.
echo "Prepare blazer_sanity_unsafe.."
cd ./blazer_sanity_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Sanity_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_straightline_safe.
echo "Prepare blazer_straightline_safe.."
cd ./blazer_straightline_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Sanity_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_straightline_unsafe.
echo "Prepare blazer_straightline_unsafe.."
cd ./blazer_straightline_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Sanity_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_unixlogin_safe.
echo "Prepare blazer_unixlogin_safe.."
cd ./blazer_unixlogin_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Timing_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare blazer_unixlogin_unsafe.
echo "Prepare blazer_unixlogin_unsafe.."
cd ./blazer_unixlogin_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Timing_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare example_PWCheck_safe.
echo "Prepare example_PWCheck_safe.."
cd ./example_PWCheck_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Driver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare example_PWCheck_unsafe.
echo "Prepare example_PWCheck_unsafe.."
cd ./example_PWCheck_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Driver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare github_authmreloaded_safe.
echo "Prepare github_authmreloaded_safe.."
cd ./github_authmreloaded_safe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare github_authmreloaded_unsafe.
echo "Prepare github_authmreloaded_unsafe.."
cd ./github_authmreloaded_unsafe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare stac_crime_unsafe.
echo "Prepare stac_crime_unsafe.."
cd ./stac_crime_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar CRIME_Driver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare stac_ibasys_unsafe.
echo "Prepare stac_ibasys_unsafe.."
cd ./stac_ibasys_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar ImageMatcher_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_boot-stateless-auth_safe.
echo "Prepare themis_boot-stateless-auth_safe.."
cd ./themis_boot-stateless-auth_safe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_boot-stateless-auth_unsafe.
echo "Prepare themis_boot-stateless-auth_unsafe.."
cd ./themis_boot-stateless-auth_unsafe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_dynatable_unsafe.
echo "Prepare themis_dynatable_unsafe.."
cd ./themis_dynatable_unsafe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_GWT_advanced_table_unsafe.
echo "Prepare themis_GWT_advanced_table_unsafe.."
cd ./themis_GWT_advanced_table_unsafe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_jdk_safe.
echo "Prepare themis_jdk_safe.."
cd ./themis_jdk_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar MessageDigest_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_jdk_unsafe.
echo "Prepare themis_jdk_unsafe.."
cd ./themis_jdk_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar MessageDigest_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_jetty_safe.
echo "Prepare themis_jetty_safe.."
cd ./themis_jetty_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Credential_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_jetty_unsafe.
echo "Prepare themis_jetty_unsafe.."
cd ./themis_jetty_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar Credential_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_oacc_unsafe.
echo "Prepare themis_oacc_unsafe.."
cd ./themis_oacc_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Driver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_openmrs-core_unsafe.
echo "Prepare themis_openmrs-core_unsafe.."
cd ./themis_openmrs-core_unsafe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_orientdb_safe.
echo "Prepare themis_orientdb_safe.."
cd ./themis_orientdb_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* OSecurityManager_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_orientdb_unsafe.
echo "Prepare themis_orientdb_unsafe.."
cd ./themis_orientdb_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* OSecurityManager_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_pac4j_safe.
echo "Prepare themis_pac4j_safe.."
cd ./themis_pac4j_safe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_pac4j_unsafe.
echo "Prepare themis_pac4j_unsafe.."
cd ./themis_pac4j_unsafe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_pac4j_unsafe_ext.
echo "Prepare themis_pac4j_unsafe_ext.."
cd ./themis_pac4j_unsafe_ext/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_picketbox_safe.
echo "Prepare themis_picketbox_safe.."
cd ./themis_picketbox_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar UsernamePasswordLoginModule_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_picketbox_unsafe.
echo "Prepare themis_picketbox_unsafe.."
cd ./themis_picketbox_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar UsernamePasswordLoginModule_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_spring-security_safe.
echo "Prepare themis_spring-security_safe.."
cd ./themis_spring-security_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar PasswordEncoderUtils_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_spring-security_unsafe.
echo "Prepare themis_spring-security_unsafe.."
cd ./themis_spring-security_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar PasswordEncoderUtils_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_tomcat_safe.
echo "Prepare themis_tomcat_safe.."
cd ./themis_tomcat_safe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Tomcat_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_tomcat_unsafe.
echo "Prepare themis_tomcat_unsafe.."
cd ./themis_tomcat_unsafe/
rm -rf bin
mkdir bin
cd src
javac -cp .:../../../tool/instrumentor/build/libs/kelinci.jar:../lib/* Tomcat_FuzzDriver.java -d ../bin
cd ..
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_tourplanner_safe.
echo "Prepare themis_tourplanner_safe.."
cd ./themis_tourplanner_safe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

# Prepare themis_tourplanner_unsafe.
echo "Prepare themis_tourplanner_unsafe.."
cd ./themis_tourplanner_unsafe/
rm -rf bin
mkdir bin
cd src/main/java/
javac -cp .:../../../../../tool/instrumentor/build/libs/kelinci.jar:../../../lib/* Driver.java -d ../../../bin
cd ../../../
rm -rf bin-instr
java -cp ../../tool/instrumentor/build/libs/kelinci.jar:./lib/* edu.cmu.sv.kelinci.instrumentor.Instrumentor -i ./bin/ -o ./bin-instr -skipmain
cd ..
echo ""

echo "Done."
