#!/bin/bash

# Check if JMeter is installed
if ! command -v jmeter &> /dev/null; then
    echo "JMeter is not installed. Please install JMeter first."
    exit 1
fi

# Set variables
JMETER_HOME=$(which jmeter | xargs dirname | xargs dirname)
TEST_PLAN="roundup-performance-test.jmx"
RESULTS_DIR="results"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_FILE="${RESULTS_DIR}/results_${TIMESTAMP}.jtl"
HTML_REPORT="${RESULTS_DIR}/html_report_${TIMESTAMP}"

# Create results directory if it doesn't exist
mkdir -p "${RESULTS_DIR}"

# Run the test
echo "Running performance test..."
jmeter -n \
    -t "${TEST_PLAN}" \
    -l "${RESULTS_FILE}" \
    -e \
    -o "${HTML_REPORT}"

# Check if the test was successful
if [ $? -eq 0 ]; then
    echo "Performance test completed successfully."
    echo "Results file: ${RESULTS_FILE}"
    echo "HTML report: ${HTML_REPORT}"
else
    echo "Performance test failed."
    exit 1
fi 