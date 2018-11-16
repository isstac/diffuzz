## run_complete_evaluation.sh
# CAUTION
# Run this script within its folder. Otherwise the paths might be wrong!
#####################################
# chmod +x run_complete_evaluation.sh
# ./run_complete_evaluation.sh
#

trap "exit" INT

# Ask user.
# 58 subjects, 5 times, 30min
read -p "Do you really want to run the complete evaluation? It will take around **6 days**? " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
  echo "ABORT."
  exit 1
fi

#####################

echo "Run complete evaluation..."

number_of_runs=5
time_bound=1800 #30min
step_size_eval=30

declare -a subjects=(
"blazer_array_safe" # MicroBench
"blazer_array_unsafe"
"blazer_loopandbranch_safe"
"blazer_loopandbranch_unsafe"
"blazer_sanity_safe"
"blazer_sanity_unsafe"
"blazer_straightline_safe"
"blazer_straightline_unsafe"
"blazer_unixlogin_safe"
"blazer_unixlogin_unsafe"
"blazer_modpow1_safe" # STAC
"blazer_modpow1_unsafe"
"blazer_modpow2_safe"
"blazer_modpow2_unsafe"
"blazer_passwordEq_safe"
"blazer_passwordEq_unsafe"
"blazer_k96_safe" # Literature
"blazer_k96_unsafe"
"blazer_gpt14_safe"
"blazer_gpt14_unsafe"
"example_PWCheck_safe"
"example_PWCheck_unsafe"
"themis_spring-security_safe" # Themis
"themis_spring-security_unsafe"
"themis_jdk_safe"
"themis_jdk_unsafe"
"themis_picketbox_safe"
"themis_picketbox_unsafe"
"themis_tomcat_safe"
"themis_tomcat_unsafe"
"themis_jetty_safe"
"themis_jetty_unsafe"
"themis_orientdb_safe"
"themis_orientdb_unsafe"
"themis_pac4j_safe"
"themis_pac4j_unsafe"
"themis_pac4j_unsafe_ext"
"themis_boot-stateless-auth_safe"
"themis_boot-stateless-auth_unsafe"
"themis_tourplanner_safe"
"themis_tourplanner_unsafe"
"themis_dynatable_unsafe"
"themis_GWT_advanced_table_unsafe"
"themis_openmrs-core_unsafe"
"themis_oacc_unsafe"
"stac_crime_unsafe" # NEW STAC
"stac_ibasys_unsafe"
"apache_ftpserver_clear_safe" # Zero Day
"apache_ftpserver_clear_unsafe"
"apache_ftpserver_md5_safe"
"apache_ftpserver_md5_unsafe"
"apache_ftpserver_salted_safe"
"apache_ftpserver_salted_unsafe"
"apache_ftpserver_salted_encrypt_unsafe"
"apache_ftpserver_stringutils_safe"
"apache_ftpserver_stringutils_unsafe"
"github_authmreloaded_safe"
"github_authmreloaded_unsafe"
)

declare -a classpaths=(
"./bin-instr/" # "blazer_array_safe"
"./bin-instr/" # "blazer_array_unsafe"
"./bin-instr/" # "blazer_loopandbranch_safe"
"./bin-instr/" # "blazer_loopandbranch_unsafe"
"./bin-instr/" # "blazer_sanity_safe"
"./bin-instr/" # "blazer_sanity_unsafe"
"./bin-instr/" # "blazer_straightline_safe"
"./bin-instr/" # "blazer_straightline_unsafe"
"./bin-instr/" # "blazer_unixlogin_safe"
"./bin-instr/" # "blazer_unixlogin_unsafe"
"./bin-instr/" # "blazer_modpow1_safe"
"./bin-instr/" # "blazer_modpow1_unsafe"
"./bin-instr/" # "blazer_modpow2_safe"
"./bin-instr/" # "blazer_modpow2_unsafe"
"./bin-instr/" # "blazer_passwordEq_safe"
"./bin-instr/" # "blazer_passwordEq_unsafe"
"./bin-instr/" # "blazer_k96_safe"
"./bin-instr/" # "blazer_k96_unsafe"
"./bin-instr/" # "blazer_gpt14_safe"
"./bin-instr/" # "blazer_gpt14_unsafe"
"./bin-instr/" # "example_PWCheck_safe"
"./bin-instr/" # "example_PWCheck_unsafe"
"./bin-instr/" # "themis_spring-security_safe"
"./bin-instr/" # "themis_spring-security_unsafe"
"./bin-instr/" # "themis_jdk_safe"
"./bin-instr/" # "themis_jdk_unsafe"
"./bin-instr/" # "themis_picketbox_safe"
"./bin-instr/" # "themis_picketbox_unsafe"
"./bin-instr/:./lib/*" # "themis_tomcat_safe"
"./bin-instr/:./lib/*" # "themis_tomcat_unsafe"
"./bin-instr/" # "themis_jetty_safe"
"./bin-instr/" # "themis_jetty_unsafe"
"./bin-instr/:./lib/*" # "themis_orientdb_safe"
"./bin-instr/:./lib/*" # "themis_orientdb_unsafe"
"./bin-instr/:./lib/*" # "themis_pac4j_safe"
"./bin-instr/:./lib/*" # "themis_pac4j_unsafe"
"./bin-instr/:./lib/*" # "themis_pac4j_unsafe_ext"
"./bin-instr/:./lib/*" # "themis_boot-stateless-auth_safe"
"./bin-instr/:./lib/*" # "themis_boot-stateless-auth_unsafe"
"./bin-instr/:./lib/*" # "themis_tourplanner_safe"
"./bin-instr/:./lib/*" # "themis_tourplanner_unsafe"
"./bin-instr/:./lib/*" # "themis_dynatable_unsafe"
"./bin-instr/:./lib/*" # "themis_GWT_advanced_table_unsafe"
"./bin-instr/:./lib/*" # "themis_openmrs-core_unsafe"
"./bin-instr/:./lib/*" # "themis_oacc_unsafe"
"./bin-instr/" # "stac_crime_unsafe"
"./bin-instr/" # "stac_ibasys_unsafe"
"./bin-instr/:./lib/*" # "apache_ftpserver_clear_safe"
"./bin-instr/:./lib/*" # "apache_ftpserver_clear_unsafe"
"./bin-instr/:./lib/*" # "apache_ftpserver_md5_safe"
"./bin-instr/:./lib/*" # "apache_ftpserver_md5_unsafe"
"./bin-instr/:./lib/*" # "apache_ftpserver_salted_safe"
"./bin-instr/:./lib/*" # "apache_ftpserver_salted_unsafe"
"./bin-instr/:./lib/*" # "apache_ftpserver_salted_encrypt_unsafe"
"./bin-instr/:./lib/*" # "apache_ftpserver_stringutils_safe"
"./bin-instr/:./lib/*" # "apache_ftpserver_stringutils_unsafe"
"./bin-instr/:./lib/*" # "github_authmreloaded_safe"
"./bin-instr/:./lib/*" # "github_authmreloaded_unsafe"
)

declare -a drivers=(
"MoreSanity_Array_FuzzDriver" # blazer_array_safe"
"MoreSanity_Array_FuzzDriver" # "blazer_array_unsafe"
"MoreSanity_LoopAndBranch_FuzzDriver" # "blazer_loopandbranch_safe"
"MoreSanity_LoopAndBranch_FuzzDriver" # "blazer_loopandbranch_unsafe"
"Sanity_FuzzDriver" # "blazer_sanity_safe"
"Sanity_FuzzDriver" # "blazer_sanity_unsafe"
"Sanity_FuzzDriver" # "blazer_straightline_safe"
"Sanity_FuzzDriver" # "blazer_straightline_unsafe"
"Timing_FuzzDriver" # "blazer_unixlogin_safe"
"Timing_FuzzDriver" # "blazer_unixlogin_unsafe"
"ModPow1_FuzzDriver" # "blazer_modpow1_safe"
"ModPow1_FuzzDriver" # "blazer_modpow1_unsafe"
"ModPow2_FuzzDriver" # "blazer_modpow2_safe"
"ModPow2_FuzzDriver" # "blazer_modpow2_unsafe"
"User_FuzzDriver" # "blazer_passwordEq_safe"
"User_FuzzDriver" # "blazer_passwordEq_unsafe"
"K96_FuzzDriver" # "blazer_k96_safe"
"K96_FuzzDriver" # "blazer_k96_unsafe"
"GPT14_FuzzDriver" # "blazer_gpt14_safe"
"GPT14_FuzzDriver" # "blazer_gpt14_unsafe"
"Driver" # "example_PWCheck_safe"
"Driver" # "example_PWCheck_unsafe"
"PasswordEncoderUtils_FuzzDriver" # "themis_spring-security_safe"
"PasswordEncoderUtils_FuzzDriver" # "themis_spring-security_unsafe"
"MessageDigest_FuzzDriver" # "themis_jdk_safe"
"MessageDigest_FuzzDriver" # "themis_jdk_unsafe"
"UsernamePasswordLoginModule_FuzzDriver" # "themis_picketbox_safe"
"UsernamePasswordLoginModule_FuzzDriver" # "themis_picketbox_unsafe"
"Tomcat_FuzzDriver" # "themis_tomcat_safe"
"Tomcat_FuzzDriver" # "themis_tomcat_unsafe"
"Credential_FuzzDriver" # "themis_jetty_safe"
"Credential_FuzzDriver" # "themis_jetty_unsafe"
"OSecurityManager_FuzzDriver" # "themis_orientdb_safe"
"OSecurityManager_FuzzDriver" # "themis_orientdb_unsafe"
"Driver" # "themis_pac4j_safe"
"Driver" # "themis_pac4j_unsafe"
"Driver" # "themis_pac4j_unsafe_ext"
"Driver" # "themis_boot-stateless-auth_safe"
"Driver" # "themis_boot-stateless-auth_unsafe"
"Driver" # "themis_tourplanner_safe"
"Driver" # "themis_tourplanner_unsafe"
"Driver" # "themis_dynatable_unsafe"
"Driver" # "themis_GWT_advanced_table_unsafe"
"Driver" # "themis_openmrs-core_unsafe"
"Driver" # "themis_oacc_unsafe"
"CRIME_Driver" # "stac_crime_unsafe"
"ImageMatcher_FuzzDriver" # "stac_ibasys_unsafe"
"Driver_Clear" # "apache_ftpserver_clear_safe"
"Driver_Clear" # "apache_ftpserver_clear_unsafe"
"Driver_MD5" # "apache_ftpserver_md5_safe"
"Driver_MD5" # "apache_ftpserver_md5_unsafe"
"Driver_Salted" # "apache_ftpserver_salted_safe"
"Driver_Salted" # "apache_ftpserver_salted_unsafe"
"Driver_Salted_Encrypt" # "apache_ftpserver_salted_encrypt_unsafe"
"Driver_StringUtilsPad" # "apache_ftpserver_stringutils_safe"
"Driver_StringUtilsPad" # "apache_ftpserver_stringutils_unsafe"
"Driver" # "github_authmreloaded_safe"
"Driver" # "github_authmreloaded_unsafe"
)

# Check array sizes
if [[ ${#subjects[@]} != ${#classpaths[@]} ]]
then
echo "[Error in script] the array sizes of subjects and classpaths do not match!. Abort!"
exit 1
fi
if [[ ${#subjects[@]} != ${#drivers[@]} ]]
then
echo "[Error in script] the array sizes of subjects and drivers do not match!. Abort!"
exit 1
fi

subject_counter=0
total_number_subjects=${#subjects[@]}
total_number_experiments=$(( $total_number_subjects * $number_of_runs))

for (( i=0; i<=$(( $total_number_subjects - 1 )); i++ ))
do
  cd ./${subjects[i]}/

  for j in `seq 1 $number_of_runs`
  do

    run_counter=$(( $run_counter + 1 ))
    echo "[$run_counter/$total_number_experiments] Run analysis for ${subjects[i]}, round $j .."

    # Start Kelinci server
    nohup java -cp ${classpaths[i]} edu.cmu.sv.kelinci.Kelinci ${drivers[i]} @@ > ./server-log-$j.txt &
    server_pid=$!

    # Start modified AFL
    AFL_I_DONT_CARE_ABOUT_MISSING_CRASHES=1 AFL_SKIP_CPUFREQ=1 nohup ../../tool/afl-2.51b-wca/afl-fuzz -i in_dir -o fuzzer-out-$j -c userdefined -S afl -t 999999999 ../../tool/fuzzerside/interface @@ > ./afl-log-$j.txt &
    afl_pid=$!

    # Wait for timebound
    sleep $time_bound

    # Stop AFL and Kelinci server
    kill $afl_pid
    kill $server_pid

    # Wait a little bit to make sure that processes are killed
    sleep 10

  done

  cd ..

  # Evaluate run
  python3 evaluate_cost.py ${subjects[i]}/fuzzer-out- $number_of_runs $time_bound $step_size_eval

done
