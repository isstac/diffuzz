"""
    Script to aggregate the results from several experiments.

    Input: results folder path, e.g.
    python3 evaluate_cost.py blazer_login_unsafe/fuzzer-out-
"""
import sys
import csv
import statistics
import math
import numpy

# do not change this parameters
START_INDEX = 1
UNIX_TIME_COLUMN_ID = 0
HIGHSCORE_COLUMN_ID = 12

if __name__ == '__main__':

    if len(sys.argv) != 5:
        raise Exception("usage: fuzzer-out-dir n timeout stepsize")

    fuzzerOutDir = sys.argv[1]
    NUMBER_OF_EXPERIMENTS = int(sys.argv[2])
    EXPERIMENT_TIMEOUT = int(sys.argv[3])
    STEP_SIZE = int(sys.argv[4])

    # Read data
    collected_data = []
    time_delta_greater_zero = {}
    for i in range(START_INDEX, NUMBER_OF_EXPERIMENTS+1):
        experimentFolderPath = fuzzerOutDir + str(i)

        data = {}
        statFilePath = experimentFolderPath + "/afl/fuzzer_stats"
        with open(statFilePath, 'r') as statFile:
            firstLine = statFile.readline()
            firstLine = firstLine.split(":")
            startTime = int(firstLine[1].strip())

        dataFile = experimentFolderPath + "/afl/plot_data"
        with open(dataFile,'r') as csvfile:
            csvreader = csv.reader(csvfile)
            timeBucket = STEP_SIZE
            next(csvreader) # skip first row
            previousValue = 0
            for row in csvreader:
                currentTime = int(row[UNIX_TIME_COLUMN_ID]) - startTime
                currentValue = int(row[HIGHSCORE_COLUMN_ID])

                if i not in time_delta_greater_zero and currentValue > 0:
                    time_delta_greater_zero[i] = currentTime

                while (currentTime > timeBucket):
                    data[timeBucket] = previousValue
                    timeBucket += STEP_SIZE

                previousValue = currentValue

                if timeBucket > EXPERIMENT_TIMEOUT:
                    break

            # fill data with last known value if not enough information
            while timeBucket <= EXPERIMENT_TIMEOUT:
                data[timeBucket] = previousValue
                timeBucket += STEP_SIZE

        collected_data.append(data)


    # Aggregate dataFile
    mean_values = {}
    std_error_values = {}
    max_values = {}

    for i in range(STEP_SIZE, EXPERIMENT_TIMEOUT+1, STEP_SIZE):
        values = []
        for j in range(START_INDEX-1, NUMBER_OF_EXPERIMENTS):
            values.append(collected_data[j][i])
        mean_values[i] = "{0:.2f}".format(sum(values)/float(NUMBER_OF_EXPERIMENTS))
        std_error_values[i] = "{0:.2f}".format(numpy.std(values)/float(math.sqrt(NUMBER_OF_EXPERIMENTS)))
        max_values[i] = max(values)

    # Write collected data
    headers = ['seconds', 'average_delta', 'std_error', 'maximum']
    outputFileName = fuzzerOutDir + "results-n=" + str(NUMBER_OF_EXPERIMENTS) + "-t=" + str(EXPERIMENT_TIMEOUT) + "-s=" + str(STEP_SIZE) + ".csv"
    print (outputFileName)
    with open(outputFileName, "w") as csv_file:
        writer = csv.DictWriter(csv_file, fieldnames=headers)
        writer.writeheader()
        for timeBucket in range(STEP_SIZE, EXPERIMENT_TIMEOUT+1, STEP_SIZE):
            if timeBucket == EXPERIMENT_TIMEOUT:
                csv_file.write("\n")
            values = {'seconds' : int(timeBucket)}
            values['average_delta'] = mean_values[timeBucket]
            values['std_error'] = std_error_values[timeBucket]
            values['maximum'] = max_values[timeBucket]
            writer.writerow(values)

        time_values = list(time_delta_greater_zero.values())
        if len(time_values) == NUMBER_OF_EXPERIMENTS:
            avg_time = "{0:.2f}".format(sum(time_values)/float(NUMBER_OF_EXPERIMENTS))
            std_error = "{0:.2f}".format(numpy.std(time_values)/float(math.sqrt(NUMBER_OF_EXPERIMENTS)))
            csv_file.write("\ntime delta>0:\n" + str(avg_time) + " (+/- " + str(std_error) + ")\n" + str(time_values))
        else:
            csv_file.write("\ntime delta>0: -\n" + str(time_values))
