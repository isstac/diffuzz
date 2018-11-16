## 02_startFuzzer.sh
# CAUTION
# Run this script within its folder. Otherwise the paths might be wrong!
#####################################
# chmod +x 02_startFuzzer.sh
# ./02_startFuzzer.sh
#

trap "exit" INT

# Run fuzzer
AFL_I_DONT_CARE_ABOUT_MISSING_CRASHES=1 AFL_SKIP_CPUFREQ=1 ../../tool/afl-2.51b-wca/afl-fuzz -i in_dir -o fuzzer-out -c userdefined -S afl -t 999999999 ../../tool/fuzzerside/interface @@

echo "Done."
