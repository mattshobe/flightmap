#!/bin/bash

ant compile || exit

rm ../data/aviation.db

java -cp build/classes:lib/sqlitejdbc.jar:lib/guava.jar:lib/commons-lang.jar:lib/commons-cli.jar:lib/flightmap-common.jar com.google.flightmap.parsing.faa.amr.AviationMasterRecordParser --airports ../data/amr/NfdcFacilities.xls --runways ../data/amr/NfdcRunways.xls --iata_to_icao ../data/iata2icao.txt --aviation_db ../data/aviation.db

java -cp build/classes:lib/sqlitejdbc.jar:lib/guava.jar:lib/commons-lang.jar:lib/commons-cli.jar:lib/flightmap-common.jar com.google.flightmap.parsing.faa.nasr.CommParser --twr ~/nasr/TWR.txt --iata_to_icao ../data/iata2icao.txt --aviation_db ../data/aviation.db

