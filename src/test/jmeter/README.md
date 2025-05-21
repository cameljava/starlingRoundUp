# Performance Testing

This directory contains JMeter test plans and scripts for performance testing the Starling RoundUp application.

## Prerequisites

1. Install JMeter:
   ```bash
   # For macOS using Homebrew
   brew install jmeter
   
   # For Ubuntu/Debian
   sudo apt-get install jmeter
   ```

2. Make sure the application is running on localhost:8080

## Test Plan Description

The test plan (`roundup-performance-test.jmx`) simulates:
- 50 concurrent users
- Each user makes 100 requests
- Ramp-up time of 10 seconds
- Measures response times, throughput, and error rates

## Running the Tests

1. Make the script executable:
   ```bash
   chmod +x run-performance-test.sh
   ```

2. Run the performance test:
   ```bash
   ./run-performance-test.sh
   ```

## Test Results

The test results will be saved in the `results` directory:
- JTL file: Contains raw test results
- HTML report: Contains a detailed HTML report with graphs and statistics

## Performance Metrics

The test measures:
- Response time (min, max, average, median)
- Throughput (requests per second)
- Error rate
- Concurrent users
- Network throughput

## Interpreting Results

1. Open the HTML report in a web browser
2. Key metrics to look for:
   - Response time should be under 1 second for 95% of requests
   - Error rate should be less than 1%
   - Throughput should be stable under load

## Customizing the Test

To modify the test parameters:
1. Open `roundup-performance-test.jmx` in JMeter GUI
2. Adjust the following parameters in the Thread Group:
   - Number of Threads (users)
   - Ramp-up period
   - Loop count
3. Save the changes and run the test again 