#!/bin/bash

ant compile || exit

AVIATION_DB="aviation.db"

rm "$AVIATION_DB"

java -cp build/classes:lib/sqlitejdbc.jar:lib/guava.jar:lib/commons-lang.jar:lib/commons-cli.jar:lib/flightmap-common.jar com.google.flightmap.parsing.faa.amr.AviationMasterRecordParser --airports ../data/amr/NfdcFacilities.xls --runways ../data/amr/NfdcRunways.xls --iata_to_icao ../data/iata2icao.txt --aviation_db "$AVIATION_DB"

java -cp build/classes:lib/sqlitejdbc.jar:lib/guava.jar:lib/commons-lang.jar:lib/commons-cli.jar:lib/flightmap-common.jar:lib/opencsv.jar com.google.flightmap.parsing.faa.nasr.CommParser --twr ~/nasr/TWR.txt --iata_to_icao ../data/iata2icao.txt --freq_uses_normalization ../data/nasr/distinct_freq_uses.csv --aviation_db "$AVIATION_DB"

java -cp build/classes/:lib/commons-cli.jar:lib/flightmap-common.jar:lib/sqlitejdbc.jar com.google.flightmap.parsing.faa.nfd.NfdAirspaceParser --aviation_db "$AVIATION_DB" --nfd ~/Desktop/FAA/FAANFD18
