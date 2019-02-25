[![DOI](https://zenodo.org/badge/157902250.svg)](https://zenodo.org/badge/latestdoi/157902250)
# DifFuzz: Differential Fuzzing for Side-Channel Analysis

This repository provides the tool and the evaluation subjects for the paper "DifFuzz: Differential Fuzzing for Side-Channel Analysis" accepted for the technical track at ICSE'2019. A pre-print of the paper can be found on [arxiv.org](https://arxiv.org/pdf/1811.07005.pdf).

The repository contains two folders: *tool* and *evaluation*.

## Tool
*DifFuzz* is built on top of the fuzzer *KelinciWCA*, which is already published as a separate branch of the *Kelinci* fuzzer by Rody Kersten. Check out their GitHub repository for more information details: [https://github.com/isstac/kelinci/tree/kelinciwca](https://github.com/isstac/kelinci/tree/kelinciwca). In the meantime our extensions have been merged into the *kelinciwca* branch of *Kelinci*. Nevertheless, we provide here a snapshot of the tool.

### Requirements
* Git, Ant, Build-Essentials, Gradle
* Java JDK = 1.8
* Python3, Numpy Package
* recommended: Ubuntu 18.04.1 LTS  

### Installation
We provide a script "setup.sh" to simply build everything. But please read first the explanations below. 

The folder *tool* contains 3 subfolders:

* *afl-2.51b-wca*: KelinciWCA, and hence also DifFuzz, is using [AFL](http://lcamtuf.coredump.cx/afl/) as the underlying fuzzing engine. KelinciWCA leverages a server-client architecture to make AFL applicable to Java applications, please refer to the Kelinci [poster-paper](https://dl.acm.org/citation.cfm?id=3138820) for more details. In order to make it easy for the users, we provide our complete modified AFL variant in this folder. Note that we only modified the file *afl-fuzz.c*. For our experiments we have used [afl-2.51b](http://lcamtuf.coredump.cx/afl/releases/?O=D). Please build AFL by following their instructions. Although the `make` command should be enough.

* *fuzzerside*: This folder includes the *interface* program to connect the *Kelinci server* to the AFL fuzzer. Simply use `make` to compile the interface.c file. If there is an error, you will have to modify the Makefile according to your system setup.

* *instrumentor*: This folder includes the *Kelinci server* and the *instrumentor* written in Java. The instrumentor is used to instrument the Java bytecode, which is necessary to add the coverage reporting and other metric collecting for the fuzzing. The Kelinci server handles requests from AFL to execute a mutated input on the application. Both are included in the same Gradle project. Therefore, you can simply use `gradle build` to build them.

As already mentioned, we have provided a script to build everything. Please execute `tool/setup.sh` to trigger that. Note that depending on your execution environment, you may want to modify this script. We tested our scripts on a Ubuntu Ubuntu 18.04.1 LTS machine.

### General Execution Instructions
In general, you will have to follow six steps in order to apply DifFuzz for side-channel analysis:

1. **Write the fuzzing driver**: In our paper we explain how a fuzzing driver should look like. Please check our evaluation subjects for some examples.

2. **Provide an initial fuzzing input**: The initial fuzzing input should be a file that does not crash the application. You can also provide multiple files.

3. **Instrument the bytecode**: Assuming that your bytecode is in the `bin` folder, the command for instrumentation could look like: `java -cp [..]/diffuzz/tool/instrumentor/build/libs/kelinci.jar edu.cmu.sv.kelinci.instrumentor.Instrumentor -i bin -o bin-instr -skipmain`

4. **Starting the Kelinci server**: Assuming that the fuzzing driver class is called `Driver`, the command for starting the Kelinci server could look like: `java -cp bin-instr edu.cmu.sv.kelinci.Kelinci Driver @@`

5. **Start fuzzing by starting the modified AFL**: Assuming that you have installed AFL correctly, the command for starting AFL could be like this: `afl-fuzz -i in_dir -o fuzzer-out -c userdefined -S afl -t 999999999 [..]/diffuzz/tool/fuzzerside/interface @@`.  Depending on your execution environment, you might want to add flags like: `AFL_I_DONT_CARE_ABOUT_MISSING_CRASHES=1` or `AFL_SKIP_CPUFREQ=1`. The timeout parameter `-t` is set to a high value, just because that we want to kill AFL process ourself.

6. **Stop fuzzing and check results**: After running AFL for a couple of minutes (the exact time depends on your execution budget, we used 30 minutes in our evaluation), you can kill the AFL process, as well as stopping the Kelinci server process. Please have a look at the file `[..]/fuzzer-out/afl/path_cost.csv`. It includes a list of mutated input that were considered *interesting*, i.e. increased overall coverage or improved the cost difference. You want to check the last file that is labeled with `highscore`. The following commmand might be helpful: `cat [..]/fuzzer-out/afl/path_cost.csv | grep highscore`. The last file labeled as `highscore` provides the maximum cost difference observed during the fuzzing run. The column `User-Defined`shows the observed cost value.

**Note**: between step 4 and 5 you might want to test that the Kelinci server is running correctly, by executing the initial input with the interface program. Assuming that the initial input file is located in the folder `in_dir` and is called `example`, the command could look like this: `[..]/diffuzz/tool/fuzzerside/interface in_dir/example`.

**Recommendation**: In order to run and at the same time check the current results, we think it is comfortable to open a terminal with three windows: (1) for the Kelinci server, (2) for the modified AFL, and (3) to regularly check the latest `highscore` path_cost.csv file. During the process you may want to check window (1) for any unexpected exceptions. Make sure that the server runs and produces messages. You mostly want to check window (2), which shows the AFL status screen (check their [description](http://lcamtuf.coredump.cx/afl/status_screen.txt) for more details).

## Evaluation
The folder *evaluation* contains all our evaluation subjects. After having DifFuzz installed, you can run the script *prepare.sh* to build and instrument all subjects.

The subjects *themis_pac4j_** and *themis_tomcat_** need a running database connection. In our experiments we used *h2* databases. Please follow their [instructions](https://www.h2database.com/html/main.html) for setup and maybe consider our helping [notes](evaluation/database_resources/database_notes.txt). Our fuzzing drivers for the *themis_pac4j_** subjects assume a connection with `url="jdbc:h2:~/pac4j-fuzz"`, `user="sa"` and `password=""`. Furthermore they assume an existing table `users (id INT, username varchar(255), password varchar(255))`. Our fuzzing drivers for the *themis_tomcat_** subjects assume a connection with `url="jdbc:h2:~/tomcat"`, `user="sa"` `password=""`, and an existing table `users (user_name varchar(255), user_pass varchar(255))`. Please make sure that your environment matches these requirements or adjust the drivers/subjects.

Be aware that the fuzzing approach uses random mutations and therefore it is necessary to repeat the experiments to get reliable results. For the paper we executed each subject for 30 minutes. We repeated each experiment 5 times and reported the averaged results.

### Scripts
In each subject folder you find two scripts: `01_startKelinciServer.sh` and `02_startFuzzer.sh`. They show how to start the Kelinci server and the modified AFL for this specific subject. You can use them directly to start the analysis (we recommend starting them in multiple terminal windows like mentioned above). Note that the execution order is *crucial*: first start the Kelinci server, and afterwards start the modified AFL! These scripts do not use any timeout, i.e. in general they will run forever. You can stop the scripts by using CTRL-C. Be aware that it is necessary to start the scripts within the specific subject folder, otherwise the paths used in the scripts won't match. Feel free to reuse our scripts to build your own execution environment, e.g. to stop the analysis after a certain timebound or run multiple instances of the same analysis repeatedly.

### Complete Evaluation Reproduction
In order to reproduce our evaluation completely, we provide the script `run_complete_evaluation.sh`. It will run all subjects 5 times, each for 30 minutes. Afterwards it will automtically generate all statistics (see next section). Be aware that the script, as it is, will run for approximately **6 days**. So it might be worthwhile to run it only for some specific subjects or modify the runtime. Feel free to adjust the script or reuse it for your own purpose.

### Statistics
The script `evaluate_cost.py` provides the aggregation of several experiment runs and will generate a file with the information about the average delta value, the std error and the overall maximum over time. It will also provide the average time for which the delta was greater than zero. The script will be automatically executed when running the complete evaluation. Feel free to adapt the command used at the end of the script `run_complete_evaluation.sh` to generate evaluation reports for specific subjects.

## Developers

* **Yannic Noller** (yannic.noller at acm.org)


## License
This project is licensed under the Apache-2.0 License - see the [LICENSE](LICENSE) file for details
