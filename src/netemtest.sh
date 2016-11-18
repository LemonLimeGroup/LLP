#!/bin/bash
# CS3251 sample network emulation script
#
# This script will run through a set of changes to the network emulation allowing you to test
# your program as we will when grading the project. 
#
# give the program you want to test and its arguments on the command line to this script
# ex: netemtest.sh iperf -c 172.17.0.2 -i 1 -b 200Mb
# ex: netemtest.sh python FTA-client.py 172.17.0.2 8000
# ex: netemtest.sh python FTA-client.py fe80::42:acff:fe11:2 8000
testProgram=$@

# trap ctrl-c and call ctrl_c()
trap ctrl_c INT

function ctrl_c() {
        echo "==== aborting, you might need to cleaup network emulation table"
	exit
}

# don't forget to execute this command:
# ethtool -K eth0 rx off
# when you run your application.

echo ===== START TEST: Print out network emulation status
echo ===== Expecting to see:
echo       qdisc noqueue 0: root refcnt 2
echo ===== otherwise you might need to cleanup your existing rules
tc qdisc show dev eth0
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: without network emulation
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: drop 1% of packets outbound
tc qdisc add dev eth0 root netem loss 1 
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: drop 5% of packets outbound
tc qdisc replace dev eth0 root netem loss 5
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: drop 20% of packets outbound
tc qdisc replace dev eth0 root netem loss 20
echo
$testProgram
tc qdisc del dev eth0 root netem loss 20
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: corrupt 1% of packets outbound
tc qdisc add dev eth0 root netem corrupt 1
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: corrupt 5% of packets outbound
tc qdisc replace dev eth0 root netem corrupt 5
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: corrupt 20% of packets outbound
tc qdisc replace dev eth0 root netem corrupt 20
echo
$testProgram
tc qdisc del dev eth0 root netem corrupt 20
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: duplicate 1% of packets outbound
tc qdisc add dev eth0 root netem duplicate 1
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: duplicate 5% of packets outbound
tc qdisc replace dev eth0 root netem duplicate 5
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: duplicate 20% of packets outbound
tc qdisc replace dev eth0 root netem duplicate 20
echo
$testProgram
tc qdisc del dev eth0 root netem duplicate 20
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: delay packets outbound 100ms
tc qdisc add dev eth0 root netem delay 100
echo
$testProgram
tc qdisc del dev eth0 root netem delay 100
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: reorder 1% of packets outbound
tc qdisc add dev eth0 root netem delay 100 reorder 1
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: reorder 5% of packets outbound
tc qdisc replace dev eth0 root netem delay 100 reorder 95
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: reorder 20% of packets outbound
tc qdisc replace dev eth0 root netem delay 100 reorder 80
echo
$testProgram
tc qdisc del dev eth0 root netem delay 100 reorder 80
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: drop, corrupt, duplicate, and reorder 2% of packets outbound
tc qdisc add dev eth0 root netem delay 100 reorder 98 loss 2 corrupt 2 duplicate 2 reorder 2
echo
$testProgram
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: drop, corrupt, duplicate, and reorder 10% of packets outbound
tc qdisc replace dev eth0 root netem delay 100 reorder 90 loss 10 corrupt 10 duplicate 10 reorder 10
echo
$testProgram
tc qdisc del dev eth0 root netem delay 100 reorder 90 loss 10 corrupt 10 duplicate 10 reorder 10
echo ===== TEST COMPLETED ==================================================================
echo

echo ===== START TEST: without network emulation
$testProgram
echo ===== TEST COMPLETED ==================================================================
